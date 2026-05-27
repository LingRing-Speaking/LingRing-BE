# RecommendedExpression 엔티티 및 조회 API 설계

- 작성일: 2026-04-25
- 스콥: `recommended_expression` 테이블을 JPA 엔티티로 매핑하고, "오늘의 추천 표현" 조회 API를 구현한다. CREATE / UPDATE / DELETE는 관리자 전용으로 본 스펙 범위 밖.

## 1. 배경 및 목표

- `table.md`의 `recommended_expression` 테이블을 JPA 엔티티로 설계한다.
- 클라이언트의 "오늘의 추천 표현" 기능을 위해 단일 추천 표현을 조회하는 API를 제공한다.
- 같은 날 호출은 같은 결과를 보장한다 (글로벌 결정론적). 프론트가 하루 동안 캐싱하지만, 캐시가 깨져도 같은 표현이 다시 반환되도록 한다.
- 데이터 적재(CREATE/UPDATE/DELETE)는 운영자가 직접 DB에 INSERT 한다. 본 스펙은 READ만 다룬다.
- 프로젝트 규칙(`.claude/rules/code-style.md`, `.claude/rules/package-structure.md`, `.claude/rules/exception-code.md`, `.claude/rules/test-code.md`)을 준수한다.

## 2. Aggregate 경계

- `RecommendedExpression`은 독립 Aggregate Root.
- 컬럼이 `UserExpression`과 동일하게 `expression` / `meaning` (각 VARCHAR(500))을 갖지만, **두 Aggregate는 분리한다**:
    - `UserExpression`: 사용자가 자유 입력하는 개인 표현
    - `RecommendedExpression`: 운영자가 큐레이션하는 공용 추천
- VO(`Expression`, `Meaning`)도 도메인별로 분리해 새로 정의한다 (DRY보다 도메인 경계 우선). 향후 검증 규칙이 갈라질 가능성에 대비.

## 3. 패키지 구조

```
com.lingring.domain.recommendedexpression/
├── api/
│   ├── RecommendedExpressionApi.java          (interface, Swagger)
│   └── RecommendedExpressionController.java
├── service/
│   └── RecommendedExpressionService.java
├── dao/
│   └── RecommendedExpressionRepository.java
├── dto/response/
│   └── RecommendedExpressionResponse.java
└── domain/
    ├── RecommendedExpression.java
    └── vo/
        ├── Expression.java
        └── Meaning.java
```

추가로 수정할 파일:
- `com.lingring.global.error.ErrorCode` — 추천 표현 에러 추가

## 4. 컴포넌트

### 4.1 `Expression` VO

- 타입: `@Embeddable`. `RecommendedExpression`에서 `@Embedded`로 사용.
- 검증 규칙: `trim` 후 1자 이상 500자 이하. 빈 문자열/공백만 입력은 거부.
- 불변: `value`는 `final`. JPA용 기본 생성자는 `@NoArgsConstructor(force = true, access = PROTECTED)`.
- 예외: `InvalidValueException` + 신규 `ErrorCode.INVALID_RECOMMENDED_EXPRESSION`.

```java
package com.lingring.domain.recommendedexpression.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = PROTECTED)
public class Expression {

    private static final int MAX_LENGTH = 500;

    @Column(name = "expression", nullable = false, length = MAX_LENGTH)
    private final String value;

    public Expression(@NonNull final String value) {
        final String trimmed = value.trim();
        validate(trimmed);
        this.value = trimmed;
    }

    private void validate(final String value) {
        if (value.isEmpty()) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_RECOMMENDED_EXPRESSION,
                    "추천 표현은 공백일 수 없습니다."
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_RECOMMENDED_EXPRESSION,
                    "추천 표현은 %d자 이하여야 합니다.".formatted(MAX_LENGTH)
            );
        }
    }
}
```

### 4.2 `Meaning` VO

`Expression`과 동일한 구조. 컬럼 `meaning`, ErrorCode `INVALID_RECOMMENDED_MEANING`.

### 4.3 `RecommendedExpression` 엔티티

- 필드: `id`, `expression` (VO), `meaning` (VO) — 3개이므로 `@Builder` 미사용.
- 생성 진입점:
    - `@NoArgsConstructor(access = PROTECTED)` — JPA hydration 전용.
    - `private` 생성자 — VO 두 개를 받음. 모든 생성 경로의 단일 진입점.
    - `public static create(String, String)` — 외부 진입점. 원시 String을 VO로 감싸며 검증.
- `id`는 `IDENTITY`. 생성자에서 설정하지 않는다.
- 인덱스: 테이블에 별도 인덱스 없음. PK만으로 충분 (조회는 `count()` + `LIMIT 1 OFFSET n`).

```java
package com.lingring.domain.recommendedexpression.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.userexpression.domain.vo.recommendedexpression.Expression;
import com.lingring.domain.userexpression.domain.vo.recommendedexpression.Meaning;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "recommended_expression")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class RecommendedExpression extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Embedded
    private Expression expression;

    @Embedded
    private Meaning meaning;

    private RecommendedExpression(
            @NonNull final Expression expression,
            @NonNull final Meaning meaning
    ) {
        this.expression = expression;
        this.meaning = meaning;
    }

    public static RecommendedExpression create(
            @NonNull final String expression,
            @NonNull final String meaning
    ) {
        return new RecommendedExpression(new Expression(expression), new Meaning(meaning));
    }
}
```

### 4.4 `ErrorCode` 추가

```java
// RecommendedExpression Error
INVALID_RECOMMENDED_EXPRESSION(BAD_REQUEST, "유효하지 않은 추천 표현입니다."),
INVALID_RECOMMENDED_MEANING(BAD_REQUEST, "유효하지 않은 추천 뜻입니다."),
RECOMMENDED_EXPRESSION_NOT_FOUND(NOT_FOUND, "오늘의 추천 표현을 찾을 수 없습니다."),
```

### 4.5 Repository

```java
package com.lingring.domain.recommendedexpression.dao;

import com.lingring.domain.userexpression.domain.RecommendedExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendedExpressionRepository
        extends JpaRepository<RecommendedExpression, Long> {
}
```

커스텀 메서드 없음. `count()`와 `findAll(Pageable)`은 JpaRepository 기본 제공이며, 호출은 Service가 책임진다.

### 4.6 Service — 일자 결정론적 단일 조회

핵심 로직:
1. `count()` 호출. 0이면 `RECOMMENDED_EXPRESSION_NOT_FOUND` 던짐.
2. `Asia/Seoul` 기준 오늘 날짜의 `epochDay`를 시드로 사용.
3. `offset = floorMod(epochDay, count)` — 음수 안전.
4. `findAll(PageRequest.of(offset, 1, Sort.by(ASC, "id")))` 으로 단일 행 조회. **정렬을 명시**해 같은 날·같은 데이터셋이면 항상 같은 행이 반환되도록 보장.

```java
package com.lingring.domain.recommendedexpression.service;

import static org.springframework.data.domain.Sort.Direction.ASC;

import com.lingring.domain.userexpression.dao.RecommendedExpressionRepository;
import com.lingring.domain.userexpression.domain.RecommendedExpression;
import com.lingring.domain.userexpression.dto.response.RecommendedExpressionResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.util.DateTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendedExpressionService {

    private final RecommendedExpressionRepository recommendedExpressionRepository;
    private final DateTimeProvider dateTimeProvider;

    public RecommendedExpressionResponse getDaily() {
        final long count = recommendedExpressionRepository.count();
        if (count == 0L) {
            throw new NotFoundException(
                    ErrorCode.RECOMMENDED_EXPRESSION_NOT_FOUND,
                    "추천 표현이 등록되어 있지 않습니다."
            );
        }
        final long epochDay = dateTimeProvider.now().toLocalDate().toEpochDay();
        final int offset = (int) Math.floorMod(epochDay, count);

        final RecommendedExpression today = recommendedExpressionRepository
                .findAll(PageRequest.of(offset, 1, Sort.by(ASC, "id")))
                .getContent()
                .get(0);
        return RecommendedExpressionResponse.from(today);
    }
}
```

설계 결정 근거:
- `LocalDateTime.now()` 직접 호출 금지 룰을 따라 `DateTimeProvider` 주입.
- 사용자별 분기 없이 글로벌 결정론적 동작. "오늘의 추천 표현"이라는 기능명과 일치하며, endpoint에 사용자 컨텍스트가 필요 없다.
- `findAll(Pageable)`이 JPA 기본 메서드이므로 Repository에 커스텀 메서드 없이 처리 가능.

### 4.7 DTO

```java
package com.lingring.domain.recommendedexpression.dto.response;

import com.lingring.domain.userexpression.domain.RecommendedExpression;
import java.time.LocalDateTime;

public record RecommendedExpressionResponse(
        Long id,
        String expression,
        String meaning,
        LocalDateTime createdAt
) {

    public static RecommendedExpressionResponse from(
            final RecommendedExpression recommendedExpression
    ) {
        return new RecommendedExpressionResponse(
                recommendedExpression.getId(),
                recommendedExpression.getExpression().getValue(),
                recommendedExpression.getMeaning().getValue(),
                recommendedExpression.getCreatedAt()
        );
    }
}
```

### 4.8 API / Controller

```
GET /recommended-expressions/daily
  200 OK   → ApiResponse<RecommendedExpressionResponse>
              { id, expression, meaning, createdAt }
  404      → RECOMMENDED_EXPRESSION_NOT_FOUND (DB 0건)
```

```java
// RecommendedExpressionApi.java
@Tag(name = "RecommendedExpression", description = "추천 표현 API")
public interface RecommendedExpressionApi {

    @Operation(
            summary = "오늘의 추천 표현 조회",
            description = "Asia/Seoul 기준 날짜를 시드로 매일 1개 추천 표현을 반환한다. "
                    + "같은 날 호출하면 같은 결과가 반환된다 (모든 사용자 공통)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFound"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/recommended-expressions/daily")
    ApiResponse<RecommendedExpressionResponse> getDaily();
}
```

```java
@RestController
@RequiredArgsConstructor
public class RecommendedExpressionController implements RecommendedExpressionApi {

    private final RecommendedExpressionService recommendedExpressionService;

    @Override
    public ApiResponse<RecommendedExpressionResponse> getDaily() {
        return ApiResponse.success(HttpStatus.OK, recommendedExpressionService.getDaily());
    }
}
```

## 5. 테이블 매핑 검증

| 컬럼 | 테이블 스펙 | 엔티티 매핑 |
|---|---|---|
| `id BIGINT NOT NULL AUTO_INCREMENT` PK | `@Id @GeneratedValue(IDENTITY) @Column(nullable = false)` |
| `expression VARCHAR(500) NOT NULL` | `Expression` VO 내부 `@Column(name = "expression", nullable = false, length = 500)` |
| `meaning VARCHAR(500) NOT NULL` | `Meaning` VO 내부 `@Column(name = "meaning", nullable = false, length = 500)` |
| `created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | `BaseTimeEntity.createdAt` |
| `updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | `BaseTimeEntity.updatedAt` |

## 6. 테스트 계획

### 6.1 VO 단위 테스트 (`@Test`만, Spring 컨텍스트 미로드)

- `ExpressionTest`
    - 정상 입력은 trim된 값을 보관한다.
    - 공백만 입력은 `InvalidValueException` (`INVALID_RECOMMENDED_EXPRESSION`).
    - 501자 이상 입력은 `InvalidValueException`.
- `MeaningTest`: `ExpressionTest`와 동일한 시나리오. ErrorCode `INVALID_RECOMMENDED_MEANING`.

### 6.2 엔티티 단위 테스트

- `RecommendedExpressionTest`
    - `create("Hello", "안녕")` 호출 시 expression/meaning이 VO로 감싸져 보관된다.
    - VO 검증이 자동으로 실행된다 (예: 공백 입력 → `InvalidValueException`).

### 6.3 Repository 테스트

- 커스텀 메서드가 없으므로 별도 테스트 작성하지 않는다 (룰: JPA 기본 메서드는 테스트하지 않음).

### 6.4 Service 테스트 (`@SpringBootTest` + `@Transactional`)

- 데이터 세팅: `repository.save(RecommendedExpression.create(...))` 사용. 시나리오별로 갯수를 다르게 INSERT (count=0/1/3 등).
- `FixedDateTimeProvider` 주입으로 시각 고정.
- 시나리오:
    - **결정론성**: 같은 날짜로 두 번 호출하면 동일한 id를 반환한다.
    - **날짜에 따른 분기**: 데이터 3건 + 날짜 A → offset=A%3, 날짜 B → offset=B%3. 두 결과가 정확히 예상된 행과 일치한다.
    - **0건 케이스**: DB가 비어있으면 `NotFoundException` (`RECOMMENDED_EXPRESSION_NOT_FOUND`).
    - **count=1 케이스**: 데이터 1건이면 어떤 날짜로 호출해도 그 1건을 반환한다.

### 6.5 Controller 테스트 (`@WebMvcTest` + `@MockitoBean`)

- 200 응답: 서비스가 `RecommendedExpressionResponse`를 반환할 때 body 구조 (`status`, `data.id`, `data.expression`, `data.meaning`, `data.createdAt`) 검증.
- 404 응답: 서비스가 `NotFoundException`을 던질 때 status 404, errorCode `RECOMMENDED_EXPRESSION_NOT_FOUND` 검증.

## 7. 범위 밖

- CREATE / UPDATE / DELETE API: 어드민 전용. 추후 별도 설계.
- 인증/인가: 본 endpoint는 글로벌 공개. 추후 필요 시 보안 정책 적용.
- 사용자별 추천 큐레이션, 카테고리별 추천: YAGNI.
- 시드 기반 셔플 알고리즘 고도화 (단순 modulo면 데이터 추가 시 같은 날 결과가 바뀜): 운영자가 데이터 추가/삭제 빈도가 낮다는 가정. 필요 시 별도 스펙으로 다룸.

## 8. 체크리스트

- [ ] `com.lingring.domain.userexpression.domain.vo.recommendedexpression.Expression` 추가
- [ ] `com.lingring.domain.userexpression.domain.vo.recommendedexpression.Meaning` 추가
- [ ] `com.lingring.domain.userexpression.domain.RecommendedExpression` 추가
- [ ] `com.lingring.global.error.ErrorCode`에 `INVALID_RECOMMENDED_EXPRESSION`, `INVALID_RECOMMENDED_MEANING`, `RECOMMENDED_EXPRESSION_NOT_FOUND` 추가
- [ ] `RecommendedExpressionRepository` 추가
- [ ] `RecommendedExpressionService` 추가 (`getDaily()`)
- [ ] `RecommendedExpressionResponse` 추가
- [ ] `RecommendedExpressionApi` (Swagger interface) 추가
- [ ] `RecommendedExpressionController` 추가
- [ ] VO/Entity/Service/Controller 테스트 추가