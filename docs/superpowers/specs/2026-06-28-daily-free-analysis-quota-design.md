# 일일 무료 분석 쿼터(AnalysisQuota) 설계

- 작성일: 2026-06-28
- 이슈: #156 `feat: 피드백 분석 횟수 추가 및 분석 시 차감 로직 구현`
- 스콥: 통화 피드백 분석을 **하루 1회 무료**로 제한하고, 분석 요청 시 무료 횟수를 **차감**한다. 무료 횟수는 **매일 00:00(Asia/Seoul)에 초기화**된다. 본 스펙은 *차감/방어 로직*까지만 다룬다.
- 스코프 외 (후속 티켓):
    - **무료 잔여 횟수 조회 API** (`GET`): 클라이언트가 "오늘 무료 1회 남음/소진"을 표시하려면 필요하지만, 이슈 제목이 "횟수 추가 및 차감 로직"이므로 본 스펙에서 제외한다. 검토 중 포함이 필요하면 알려주면 스코프에 넣는다.
    - **유료 결제 횟수**: 결제 도입 시 `AnalysisQuota`에 잔액 필드를 추가하는 방향으로 설계해 두되, 본 스펙에서는 만들지 않는다(YAGNI).
- 프로젝트 규칙(`.claude/rules/code-style.md`, `package-structure.md`, `exception-code.md`, `test-code.md`)을 준수한다.

## 1. 배경 및 목표

- 현재 통화 분석은 `POST /calls/{callId}/analysis` → `CallAnalysisRequestFacade.request(callId, userId)`로 진입하며, 횟수 제한이 없다.
- 사용자별로 **하루 1회 무료 분석**을 보장하고, 무료 횟수를 모두 사용하면 분석 요청을 거부한다.
- "00:00 초기화"는 **스케줄러 없이** 날짜 비교(lazy)로 처리한다. 이미 코드베이스에 동일 패턴(`UserStats.lastStudyDate` + `hasStudiedOn`)이 있다.
- 결제 도입이 예정되어 있으므로, 무료/유료 횟수를 담을 **전용 Aggregate**를 두어 `UserStats`(학습 통계)와 책임을 분리한다.

## 2. 설계 결정 요약

| 결정 | 선택 | 이유 |
|---|---|---|
| 초기화 방식 | **Lazy 날짜 비교** (스케줄러 없음) | 자정 대량 UPDATE/ShedLock 불필요, cron 누락 리스크 없음, 기존 선례 존재 |
| 저장 위치 | **신규 `AnalysisQuota` aggregate** (전용 테이블) | 결제 확장 시 잔액/거래내역을 여기에 흡수, `UserStats` 비대화 방지, cross-domain 의존 회피 |
| 도메인 위치 | `review` 도메인 | 쿼터는 "분석을 생성할 권리"이고 분석(`CallAnalysis`)을 review가 소유 |
| 차감 시점 | `CallAnalysis.requested` **false→true 최초 전환 시** | 유저별·통화별 1회만 과금, 재열람 무료, 자연스러운 멱등 hook |
| 소진 시 응답 | `FORBIDDEN(403)` | ErrorCode에 429 미사용. 기존 `BAD_REQUEST`/`FORBIDDEN` 컨벤션을 따름 |

## 3. Aggregate 경계

- `AnalysisQuota`는 독립 Aggregate Root. 사용자당 1행(`user_id` UNIQUE).
- `User`/`CallAnalysis`와는 **식별자(`userId`, `callId`)로 간접 참조**한다(다른 Aggregate 직접 참조 금지 규칙).
- 무료 일일 한도 상수(`DAILY_FREE_LIMIT = 1`)는 `AnalysisQuota`가 보유한다. 도메인 규칙(가용 여부 판정·차감)은 모두 엔티티 안에 둔다.

## 4. 패키지 구조

```
com.lingring.domain.review/
├── domain/quota/
│   └── AnalysisQuota.java                          (신규 · 엔티티 + 도메인 로직)
├── dao/
│   └── AnalysisQuotaRepository.java                (신규)
├── service/
│   └── AnalysisQuotaService.java                   (신규)
└── exception/
    └── DailyFreeAnalysisExhaustedException.java    (신규)
```

수정할 파일:
- `domain/review/domain/analysis/CallAnalysis.java` — `markRequested()`(현재 `requestForUser` 한 곳에서만 사용)를 전환 여부를 반환하는 `markRequestedIfAbsent()`로 대체
- `domain/review/service/CallAnalysisService.java` — `requestForUser` 반환을 `RequestResult(analysis, freshlyRequested)`로
- `domain/review/facade/CallAnalysisRequestFacade.java` — 쿼터 차감 오케스트레이션 추가
- `global/error/ErrorCode.java` — `DAILY_FREE_ANALYSIS_EXHAUSTED` 추가

기존 테스트 수정(시그니처 변경 영향):
- `test/.../review/service/CallAnalysisServiceTest.java` — `requestForUser`가 `CallAnalysis` 대신 `RequestResult`를 반환하므로 `result.analysis().isRequested()` 형태로 갱신
- `test/.../review/facade/CallAnalysisRequestFacadeTest.java` — facade에 `AnalysisQuotaService` 의존이 추가되므로 셋업 보강, 차감/소진 케이스 추가

## 5. 컴포넌트

### 5.1 `AnalysisQuota` 엔티티

- 테이블 `analysis_quota` (단수형 규칙).
- 필드:
    - `id` (PK, IDENTITY)
    - `userId` (BIGINT NOT NULL, UNIQUE `uk_analysis_quota_user_id`)
    - `lastFreeAnalysisDate` (DATE NULL) — 마지막으로 무료 분석을 소비한 날짜. `null`이면 아직 무료를 한 번도 안 씀.
    - `createdAt` / `updatedAt` (`BaseTimeEntity` 상속)
- 팩토리: `initial(Long userId)` → `lastFreeAnalysisDate = null`
- 도메인 메서드:
    - `boolean hasFreeAnalysisOn(LocalDate today)` — `lastFreeAnalysisDate == null || !lastFreeAnalysisDate.isEqual(today)` (긍정문 네이밍 규칙)
    - `void consumeOn(LocalDate today)` — `hasFreeAnalysisOn(today)`이면 `lastFreeAnalysisDate = today`로 갱신, 아니면 `DailyFreeAnalysisExhaustedException` throw
- **결제 확장 포인트(미구현)**: 향후 `paidBalance` 필드를 추가하고 `consumeOn`을 "무료 가용 → 무료 차감, 아니면 유료 잔액 차감, 둘 다 없으면 throw"로 확장한다. 본 스펙에서는 무료 분기만 구현한다.

```java
// 핵심 도메인 로직 (의사 코드)
public boolean hasFreeAnalysisOn(final LocalDate today) {
    return lastFreeAnalysisDate == null || !lastFreeAnalysisDate.isEqual(today);
}

public void consumeOn(final LocalDate today) {
    if (!hasFreeAnalysisOn(today)) {
        throw new DailyFreeAnalysisExhaustedException(userId);
    }
    this.lastFreeAnalysisDate = today;
}
```

### 5.2 `AnalysisQuotaRepository`

- `Optional<AnalysisQuota> findByUserId(Long userId)`

### 5.3 `AnalysisQuotaService`

- `DateTimeProvider` 주입(시스템 시계 직접 호출 금지 규칙).
- `@Transactional void consumeFreeDaily(Long userId)`:
    1. `final LocalDate today = dateTimeProvider.now().toLocalDate();`
    2. `findByUserId(userId)` → 없으면 `AnalysisQuota.initial(userId)` 저장 (**lazy 생성** — 가입 시점이 아니라 최초 분석 시점에 행 생성)
    3. `quota.consumeOn(today)` — 소진이면 throw
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

## 6. 동작 정의 (차감 시맥)

1. **유저별·통화별 최초 요청 시 1회 차감**: `requested`가 false→true로 전환될 때만 `consumeFreeDaily` 호출.
2. **재요청·재열람은 무료**: 이미 `requested=true`인 통화는 `freshlyRequested=false`라 차감하지 않으며, 무료를 다 써도 재열람은 막지 않는다.
3. **두 참여자는 각자 차감**: A가 요청하면 A만 차감되고 B의 분석은 piggyback 생성(`requested=false`). B가 자기 기록에서 분석을 열면 그때 B가 차감된다.
4. **검증 통과 후 차감**: `startTranscript`의 기존 방어(통화 활성/1분 미만/녹음 준비)를 모두 통과한 뒤에만 쿼터를 소비한다.
5. **원자성 보장**: 분석 파이프라인을 트리거하는 `CallAnalysisRequestedListener`는 `@TransactionalEventListener(AFTER_COMMIT)`다. 쿼터 소진으로 throw하면 커밋되지 않아 SQS 발행·transcript 생성·`requested` 마킹이 **모두 롤백**된다. 쿼터 없는 유저는 분석 파이프라인 자체가 돌지 않는다.
6. **00:00 초기화(자연 발생)**: 날짜가 바뀌면 `hasFreeAnalysisOn(today)`가 `lastFreeAnalysisDate != today`로 다시 true가 된다. 별도 초기화 작업·스케줄러 없음. 기준 타임존은 `DateTimeProvider`(Asia/Seoul).

## 7. 예외 / ErrorCode

- `DailyFreeAnalysisExhaustedException extends DomainException` (도메인 규칙 위반 → `DomainException` 하위, exception-code.md 규칙).
    - 필드/생성자 표준형(`errorCode`, `message`, `@Getter`만).
    - 메시지는 로그 식별성을 위해 구체적으로: `"userId %d의 오늘 무료 분석 횟수를 모두 사용했습니다.".formatted(userId)`
- `ErrorCode.DAILY_FREE_ANALYSIS_EXHAUSTED(FORBIDDEN, "오늘 무료 분석 횟수를 모두 사용했습니다.")` — 클라이언트 노출 메시지는 고정.

## 8. DDL

```sql
CREATE TABLE analysis_quota (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT       NOT NULL,
    last_free_analysis_date DATE         NULL,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_analysis_quota_user_id UNIQUE (user_id)
);
```

## 9. 테스트 계획

- **`AnalysisQuota` 단위**: `hasFreeAnalysisOn`(null/같은 날/다른 날), `consumeOn` 정상 갱신, 같은 날 두 번째 `consumeOn`은 throw, 자정 롤오버(소비 후 다음 날 다시 가용).
- **`AnalysisQuotaService`**: lazy 생성(행 없을 때 생성 후 소비), 소진 시 throw — `FixedDateTimeProvider`로 시각 고정.
- **`CallAnalysisRequestFacade` 통합**:
    - 최초 요청 → 차감 발생
    - 같은 통화 재요청 → 무차감
    - 무료 소진 후 *다른* 통화 fresh 요청 → 403 차단 + 롤백(이벤트/transcript 미생성)
    - 무료 소진 상태에서 *이미 요청한* 통화 재열람 → 허용
    - 두 참여자 각자 1회씩 차감
    - 자정 롤오버 후 재가용(`FixedDateTimeProvider` 날짜 이동)
- 규칙: API·도메인 로직 추가 시 대응 테스트를 함께 작성(`test-code.md`).