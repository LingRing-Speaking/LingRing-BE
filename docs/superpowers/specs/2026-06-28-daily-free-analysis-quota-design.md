# 분석 쿼터(AnalysisQuota) 설계 — 일일 무료 + 추가 잔액

- 작성일: 2026-06-28
- 이슈: #156 `feat: 피드백 분석 횟수 추가 및 분석 시 차감 로직 구현`
- 스콥: 통화 피드백 분석을 **하루 N회 무료**(기본 N=1)로 제한하고, 분석 요청 시 횟수를 **차감**한다. 무료 횟수는 **매일 00:00(Asia/Seoul)에 초기화**된다. 무료와 별개로 **유저별 추가 잔액(`extraBalance`)** 을 두어, 무료 소진 시 잔액에서 차감한다(테스터 지급 / 향후 결제 충전의 공통 메커니즘). 클라이언트 표시를 위한 **잔여 횟수 조회 API**(`GET /me/analysis-quota`)를 포함한다.
- 스코프 외 (후속 티켓):
    - **결제 연동**: 결제 성공 시 금액을 횟수로 환산해 `extraBalance`에 더하는 로직. 본 스펙은 잔액 **차감/보유 구조**만 만들고, 충전은 운영자 DB 조작으로 대신한다(YAGNI).
    - **관리자 지급 API**: 테스터 지급은 운영자가 DB를 직접 UPDATE 한다(이 프로젝트의 `RecommendedExpression` 적재 선례와 동일). 별도 API를 만들지 않는다.
- 프로젝트 규칙(`.claude/rules/code-style.md`, `package-structure.md`, `exception-code.md`, `test-code.md`)을 준수한다.

## 1. 배경 및 목표

- 현재 통화 분석은 `POST /calls/{callId}/analysis` → `CallAnalysisRequestFacade.request(callId, userId)`로 진입하며, 횟수 제한이 없다.
- 사용자별로 **하루 N회 무료 분석**을 보장하고, 무료를 모두 쓰면 **추가 잔액**에서 차감한다. 둘 다 없으면 분석 요청을 거부한다.
- **횟수는 항상 "건수(int)" 단위로 차감**한다. 분석 1건 = 1 차감. 무료/잔액 어느 버킷에서 빠지든 차감 단위는 동일하다. 결제는 *금액 → 건수* 환산 후 `extraBalance`에 더하는 충전 수단일 뿐, 차감 경로를 바꾸지 않는다.
- **일일 무료 데이터 모델은 "날짜"와 "그 날 쓴 횟수"를 분리**한다(`freeUsedDate`, `freeUsedCount`). 일일 한도는 `AnalysisQuota.DAILY_FREE_LIMIT` 상수(기본 1)다. 한도를 1→N으로 바꿔도 **상수 한 줄만 수정**하면 된다.
- "00:00 초기화"는 **스케줄러 없이** 날짜 비교(lazy)로 처리한다(무료 버킷에만 적용 — 잔액은 초기화되지 않음). 날이 바뀌면 `freeUsedDate != today`가 되어 그 날 무료 사용 횟수가 0으로 간주된다. 이미 코드베이스에 동일 패턴(`UserStats.lastStudyDate` + `hasStudiedOn`)이 있다.
- lazy 방식은 00:00에 서버 푸시가 없다. 클라이언트는 화면을 그리는 시점에 `GET /me/analysis-quota`로 현재 잔여를 **pull**하고, 화면 포커스/앱 복귀 시 refetch한다. 실제 차감 가부는 분석 요청 시 서버가 `now()` 기준으로 최종 판정하므로, 표시가 일시적으로 어긋나도 정확성은 서버가 보장한다.
- 결제/지급이 예정되어 있으므로, 무료/잔액을 담을 **전용 Aggregate**를 두어 `UserStats`(학습 통계)와 책임을 분리한다.

## 2. 설계 결정 요약

| 결정 | 선택 | 이유 |
|---|---|---|
| 초기화 방식 | **Lazy 날짜 비교** (스케줄러 없음) | 자정 대량 UPDATE/ShedLock 불필요, cron 누락 리스크 없음, 기존 선례 존재 |
| 무료 데이터 모델 | **(freeUsedDate, freeUsedCount) + `DAILY_FREE_LIMIT` 상수** | 한도를 데이터 모양에 박지 않음. 1→N 변경이 상수 한 줄 |
| 유저별 추가분 | **`extraBalance`(int, 건수) 버킷** | 테스터 지급·결제 충전의 공통 메커니즘. 무료와 분리, 초기화 없음 |
| 차감 단위 | **항상 1건** | 결제 금액이 아니라 건수로 차감. 결제는 충전 수단일 뿐 |
| 저장 위치 | **신규 `AnalysisQuota` aggregate** (전용 테이블) | 결제 확장 시 잔액/거래내역을 여기에 흡수, `UserStats` 비대화 방지, cross-domain 의존 회피 |
| 도메인 위치 | `review` 도메인 | 쿼터는 "분석을 생성할 권리"이고 분석(`CallAnalysis`)을 review가 소유 |
| 차감 시점 | `CallAnalysis.requested` **false→true 최초 전환 시** | 유저별·통화별 1회만 과금, 재열람 무료, 자연스러운 멱등 hook |
| 소진 시 응답 | `FORBIDDEN(403)` | ErrorCode에 429 미사용. 기존 `BAD_REQUEST`/`FORBIDDEN` 컨벤션을 따름 |

## 3. Aggregate 경계

- `AnalysisQuota`는 독립 Aggregate Root. 사용자당 1행(`user_id` UNIQUE).
- `User`/`CallAnalysis`와는 **식별자(`userId`, `callId`)로 간접 참조**한다(다른 Aggregate 직접 참조 금지 규칙).
- 일일 무료 한도(`DAILY_FREE_LIMIT`), 무료/잔액 차감 순서, 가용 판정 규칙은 모두 엔티티 안에 둔다. 한도는 외부에서 주입하지 않고 엔티티가 소유한다.

## 4. 패키지 구조

```
com.lingring.domain.review/
├── api/
│   ├── AnalysisQuotaApi.java                       (신규 · interface, Swagger)
│   └── AnalysisQuotaController.java                (신규)
├── domain/quota/
│   └── AnalysisQuota.java                          (신규 · 엔티티 + 도메인 로직)
├── dao/
│   └── AnalysisQuotaRepository.java                (신규)
├── dto/response/
│   └── AnalysisQuotaResponse.java                  (신규 · record)
├── service/
│   └── AnalysisQuotaService.java                   (신규)
└── exception/
    └── AnalysisQuotaExhaustedException.java        (신규)
```

수정할 파일:
- `domain/review/domain/analysis/CallAnalysis.java` — `markRequested()`(현재 `requestForUser` 한 곳에서만 사용)를 전환 여부를 반환하는 `markRequestedIfAbsent()`로 대체
- `domain/review/service/CallAnalysisService.java` — `requestForUser` 반환을 `RequestResult(analysis, freshlyRequested)`로
- `domain/review/facade/CallAnalysisRequestFacade.java` — 쿼터 차감 오케스트레이션 추가
- `global/error/ErrorCode.java` — `ANALYSIS_QUOTA_EXHAUSTED` 추가

기존 테스트 수정(시그니처 변경 영향):
- `test/.../review/service/CallAnalysisServiceTest.java` — `requestForUser`가 `CallAnalysis` 대신 `RequestResult`를 반환하므로 `result.analysis().isRequested()` 형태로 갱신
- `test/.../review/facade/CallAnalysisRequestFacadeTest.java` — facade에 `AnalysisQuotaService` 의존이 추가되므로 셋업 보강, 차감/소진 케이스 추가

## 5. 컴포넌트

### 5.1 `AnalysisQuota` 엔티티

- 테이블 `analysis_quota` (단수형 규칙).
- 일일 무료 한도: `private static final int DAILY_FREE_LIMIT = 1;`
- 필드:
    - `id` (PK, IDENTITY)
    - `userId` (BIGINT NOT NULL, UNIQUE `uk_analysis_quota_user_id`)
    - `freeUsedDate` (DATE NULL) — 무료를 마지막으로 사용한 날짜. `null`이면 아직 한 번도 안 씀.
    - `freeUsedCount` (INT NOT NULL DEFAULT 0) — `freeUsedDate` 당일 사용한 무료 횟수.
    - `extraBalance` (INT NOT NULL DEFAULT 0) — 무료와 별개의 추가 분석 가능 건수. 초기화 없음. 테스터 지급/결제 충전으로 증가.
    - `createdAt` / `updatedAt` (`BaseTimeEntity` 상속)
- 팩토리: `initial(Long userId)` → `freeUsedDate = null`, `freeUsedCount = 0`, `extraBalance = 0`
- 도메인 메서드 (긍정문 네이밍, 삼항/else 미사용):
    - `int usedOn(LocalDate today)` — `today`가 `freeUsedDate`와 같으면 `freeUsedCount`, 아니면 0(날 바뀜)
    - `int dailyFreeRemainingOn(LocalDate today)` — `DAILY_FREE_LIMIT - usedOn(today)`
    - `boolean hasFreeAnalysisOn(LocalDate today)` — `usedOn(today) < DAILY_FREE_LIMIT`
    - `void consumeOn(LocalDate today)` — **차감 순서: 무료 → 잔액 → 소진 throw** (아래 코드)
- **결제 확장 포인트(미구현)**: 결제 성공 시 호출할 `grant(int count)`(= `extraBalance += count`)는 결제 티켓에서 추가한다. 본 스펙에서는 충전을 운영자 DB UPDATE로 대신하므로 `grant`를 만들지 않는다. 차감 구조(`consumeOn`)는 결제가 붙어도 그대로다.

```java
// 핵심 도메인 로직
private static final int DAILY_FREE_LIMIT = 1;

public int usedOn(final LocalDate today) {
    if (freeUsedDate == null || !freeUsedDate.isEqual(today)) {
        return 0;
    }
    return freeUsedCount;
}

public int dailyFreeRemainingOn(final LocalDate today) {
    return DAILY_FREE_LIMIT - usedOn(today);
}

public boolean hasFreeAnalysisOn(final LocalDate today) {
    return usedOn(today) < DAILY_FREE_LIMIT;
}

public void consumeOn(final LocalDate today) {
    if (hasFreeAnalysisOn(today)) {        // 1) 일일 무료 우선
        consumeDailyFree(today);
        return;
    }
    if (extraBalance > 0) {                // 2) 추가 잔액
        this.extraBalance -= 1;
        return;
    }
    throw new AnalysisQuotaExhaustedException(userId);   // 3) 둘 다 소진
}

private void consumeDailyFree(final LocalDate today) {
    if (freeUsedDate == null || !freeUsedDate.isEqual(today)) {   // 날 바뀜 → 리셋
        this.freeUsedDate = today;
        this.freeUsedCount = 0;
    }
    this.freeUsedCount += 1;
}
```

### 5.2 `AnalysisQuotaRepository`

- `Optional<AnalysisQuota> findByUserId(Long userId)`

### 5.3 `AnalysisQuotaService`

- `DateTimeProvider` 주입(시스템 시계 직접 호출 금지 규칙).
- `@Transactional void consumeFreeDaily(Long userId)`:
    1. `final LocalDate today = dateTimeProvider.now().toLocalDate();`
    2. `findByUserId(userId)` → 없으면 `AnalysisQuota.initial(userId)` 저장 (**lazy 생성** — 가입 시점이 아니라 최초 분석 시점에 행 생성)
    3. `quota.consumeOn(today)` — 무료·잔액 모두 소진이면 throw
- `@Transactional(readOnly = true) AnalysisQuotaResponse getStatus(Long userId)`:
    1. `final LocalDate today = dateTimeProvider.now().toLocalDate();`
    2. `findByUserId(userId)` → 없으면 `AnalysisQuota.initial(userId)`를 **저장하지 않은 임시 객체**로 사용(행 미생성). 한도 상수가 엔티티에 캡슐화돼 있으므로 잔여 계산을 임시 객체로 수행.
    3. `final int freeRemaining = quota.dailyFreeRemainingOn(today);`
    4. `final int extraRemaining = quota.getExtraBalance();`
    5. `final LocalDateTime nextResetAt = today.plusDays(1).atStartOfDay();` (Asia/Seoul 기준 다음 0시)
    6. **읽기 전용 — 어떤 행도 생성/수정하지 않는다.**
- 동시성: 같은 유저가 최초 분석을 동시에 두 번 요청하면 `user_id` UNIQUE 제약으로 한쪽이 실패한다. 멱등 재시도/낙관적 락은 본 스펙 범위 밖(YAGNI).

### 5.4 차감 통합 (`requested` 전환 hook)

`CallAnalysis`:
```java
public boolean markRequestedIfAbsent() {   // 기존 markRequested() 대체
    if (this.requested) {
        return false;
    }
    this.requested = true;
    return true;
}
```

`CallAnalysisService.requestForUser`:
```java
public record RequestResult(CallAnalysis analysis, boolean freshlyRequested) {}

@Transactional
public RequestResult requestForUser(final Long callId, final Long userId) {
    final CallAnalysis analysis = callAnalysisRepository.findByCallIdAndUserId(callId, userId)
            .orElseGet(() -> callAnalysisRepository.save(CallAnalysis.processing(callId, userId)));
    final boolean fresh = analysis.markRequestedIfAbsent();
    return new RequestResult(analysis, fresh);
}
```

`CallAnalysisRequestFacade.request`:
```java
@Transactional
public CallAnalysisStartResponse request(final Long callId, final Long userId) {
    final StartTranscriptResult result = callTranscriptService.startTranscript(callId, userId);

    final RequestResult self = callAnalysisService.requestForUser(callId, userId);
    if (self.freshlyRequested()) {
        analysisQuotaService.consumeFreeDaily(userId);   // 소진 시 throw → 트랜잭션 전체 롤백
    }

    final Long otherUserId = result.userAId().equals(userId) ? result.userBId() : result.userAId();
    callAnalysisService.ensureExistsForUser(callId, otherUserId);

    if (result.freshlyCreated()) {
        eventPublisher.publishEvent(new CallAnalysisRequestedEvent(callId, result.recordings()));
    }
    return new CallAnalysisStartResponse(self.analysis().getId());
}
```

### 5.5 조회 API

- 인터페이스 `AnalysisQuotaApi`(Swagger 명세) + 구현 `AnalysisQuotaController`(controller에 swagger 직접 명시 금지 규칙).
- `@GetMapping("/me/analysis-quota")` — user-level 리소스는 `/me/stats` 컨벤션을 따름. `@AuthUser Long userId`로 인증 사용자 식별.
- 반환: `ApiResponse<AnalysisQuotaResponse>`.

```java
public record AnalysisQuotaResponse(
        int freeRemaining,      // 오늘 남은 무료 횟수
        int extraRemaining,     // 추가 잔액(건수)
        LocalDateTime nextResetAt   // 무료가 충전되는 다음 0시(Asia/Seoul)
) {}
```
- 클라는 `freeRemaining + extraRemaining`으로 "분석 가능 N회"를 표시하거나, "무료 1회 + 보너스 M회"처럼 분리 표시할 수 있다.
- `nextResetAt`은 무료 버킷 기준. 추가 잔액은 초기화되지 않으므로 reset 표시 대상이 아니다.

## 6. 동작 정의 (차감 시맥)

1. **유저별·통화별 최초 요청 시 1건 차감**: `requested`가 false→true로 전환될 때만 `consumeFreeDaily` 호출.
2. **차감 순서 = 무료 먼저, 그 다음 추가 잔액**: 같은 날 `DAILY_FREE_LIMIT`까지는 무료에서, 무료 소진 후엔 `extraBalance`에서 1씩 차감. 둘 다 0이면 `consumeOn`이 throw.
3. **재요청·재열람은 무료**: 이미 `requested=true`인 통화는 `freshlyRequested=false`라 차감하지 않으며, 잔여가 0이어도 재열람은 막지 않는다.
4. **두 참여자는 각자 차감**: A가 요청하면 A만 차감되고 B의 분석은 piggyback 생성(`requested=false`). B가 자기 기록에서 분석을 열면 그때 B가 차감된다.
5. **검증 통과 후 차감**: `startTranscript`의 기존 방어(통화 활성/1분 미만/녹음 준비)를 모두 통과한 뒤에만 차감한다.
6. **원자성 보장**: 분석 파이프라인을 트리거하는 `CallAnalysisRequestedListener`는 `@TransactionalEventListener(AFTER_COMMIT)`다. 소진으로 throw하면 커밋되지 않아 SQS 발행·transcript 생성·`requested` 마킹이 **모두 롤백**된다. 쿼터 없는 유저는 분석 파이프라인 자체가 돌지 않는다.
7. **00:00 초기화(자연 발생)**: 날짜가 바뀌면 `usedOn(today)`가 0이 되어 무료 한도가 다시 가용해진다. 추가 잔액은 영향 없음. 별도 초기화 작업·스케줄러 없음. 기준 타임존은 `DateTimeProvider`(Asia/Seoul).
8. **테스터 지급/결제(운영)**: `UPDATE analysis_quota SET extra_balance = extra_balance + N WHERE user_id = ?`로 충전. 행이 없으면 먼저 INSERT. 결제 도입 시 이 UPDATE를 결제 성공 핸들러가 대신한다.

## 7. 예외 / ErrorCode

- `AnalysisQuotaExhaustedException extends DomainException` (도메인 규칙 위반 → `DomainException` 하위, exception-code.md 규칙).
    - 필드/생성자 표준형(`errorCode`, `message`, `@Getter`만).
    - 메시지는 로그 식별성을 위해 구체적으로: `"userId %d의 분석 횟수(무료+잔액)를 모두 사용했습니다.".formatted(userId)`
- `ErrorCode.ANALYSIS_QUOTA_EXHAUSTED(FORBIDDEN, "분석 가능 횟수를 모두 사용했습니다.")` — 클라이언트 노출 메시지는 고정.

## 8. DDL

```sql
CREATE TABLE analysis_quota (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    free_used_date  DATE         NULL,
    free_used_count INT          NOT NULL DEFAULT 0,
    extra_balance   INT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_analysis_quota_user_id UNIQUE (user_id)
);
```

## 9. 테스트 계획

- **`AnalysisQuota` 단위**:
    - `usedOn` — `freeUsedDate`가 오늘/다른 날/null일 때 각각 카운트/0/0
    - `dailyFreeRemainingOn` / `hasFreeAnalysisOn` — 한도 경계
    - `consumeOn` 무료 — 정상 증가, 한도 도달 후 **잔액 0이면** throw, **날 바뀌면 카운트 리셋 후 다시 가용**
    - `consumeOn` 잔액 — 무료 소진 상태에서 `extraBalance`에서 1씩 차감, 잔액까지 0이면 throw
    - 차감 순서 — 무료가 남아있으면 잔액은 줄지 않음
- **`AnalysisQuotaService`**: lazy 생성, 소진 throw, `getStatus`가 행 없을 때 행을 만들지 않고 `freeRemaining = 한도`·`extraRemaining = 0` 반환 — `FixedDateTimeProvider`로 시각 고정.
- **`CallAnalysisRequestFacade` 통합**:
    - 최초 요청 → 무료 차감
    - 같은 통화 재요청 → 무차감
    - 무료 소진 + 잔액 있음 → *다른* 통화 fresh 요청 시 잔액에서 차감(성공)
    - 무료·잔액 모두 소진 → fresh 요청 403 + 롤백(이벤트/transcript 미생성)
    - 소진 상태에서 *이미 요청한* 통화 재열람 → 허용
    - 두 참여자 각자 1건씩 차감
    - 자정 롤오버 후 무료 재가용(`FixedDateTimeProvider` 날짜 이동)
- **조회 API**(`AnalysisQuotaController`): 미사용 유저 `freeRemaining = 한도`·`extraRemaining = 0`, 무료 소진+잔액 보유 유저 `freeRemaining = 0`·`extraRemaining > 0`, `nextResetAt`이 다음 0시.
- 규칙: API·도메인 로직 추가 시 대응 테스트를 함께 작성(`test-code.md`).