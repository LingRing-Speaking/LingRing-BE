# Match → CallHistory 리네임 및 도메인 분리 설계

- 작성일: 2026-05-02
- 스콥: 현재 `domain/matching/`에 섞여 있는 통화 record(`Match` 엔티티 + 부속 메서드)를 신규 `domain/call/` 도메인으로 분리하면서 `CallHistory`로 rename. 동작은 변경하지 않는다(순수 리네임 + 의존 재배선). 매칭 큐/정책/스케줄러는 `matching` 도메인에 그대로 둔다.

## 1. 배경 및 목표

- `table.md`에는 `call_history` 테이블이 정의되어 있으나 코드에는 `match_call` 테이블 + `Match` 엔티티가 존재한다. 두 정의는 컬럼이 거의 동일하며(차이는 `room_id` 유무, `status` enum 세분화, `duration_seconds`) 데이터 의미는 같다.
- `MatchingService`가 큐 진입/탈출/상태 조회와 통화 record 생성·종료를 한꺼번에 들고 있어 책임 경계가 모호하다. 매칭(짝짓기) 행위와 통화(call session) record는 라이프사이클 단계가 다르므로 분리한다.
- 본 스펙은 **순수 리네임 + 도메인 분리**만 다룬다. 동작/스키마 의미 변경(예: `status` 의미 세분화, FAILED 처리 정책)은 후속 PR에서 별도 결정.
- 프로젝트 규칙(`.claude/rules/code-style.md`, `.claude/rules/package-structure.md`, `.claude/rules/exception-code.md`, `.claude/rules/test-code.md`)을 준수한다.

## 2. Aggregate 경계

- `CallHistory`가 단일 Aggregate Root.
- `domain/matching/`은 영속 데이터 없는 큐 + 정책 + 스케줄러로 한정. RDB 영속 객체는 모두 `domain/call/`이 소유한다.
- 의존 방향: `matching → call`, `signaling → call` (단방향).
- 매칭 commit 직후 `MatchingExecutor`가 `CallHistoryRepository`에 직접 저장한다. `ApplicationEventPublisher` 기반으로 분리하는 작업은 후속 별도 스펙.

## 3. 패키지 구조

분리 후 모습:

```
domain/
├── matching/                      ← 유지
│   ├── api/MatchingApi, MatchingController
│   ├── service/MatchingService           ← 큐 메서드만 남김 (enterQueue/leaveQueue/getStatus)
│   ├── service/MatchingExecutor          ← MatchRepository 의존을 CallHistoryRepository로
│   ├── service/RoomIdGenerator, UuidRoomIdGenerator
│   ├── scheduler/MatchingWorker
│   ├── dao/MatchingQueueRepository
│   ├── domain/MatchingCandidate, MatchingQueue, MatchingPollStatus, MatchingResult
│   ├── domain/policy/*
│   └── dto/response/MatchingStatusResponse
└── call/                          ← 신규
    ├── service/CallHistoryService
    ├── dao/CallHistoryRepository
    ├── domain/CallHistory
    └── exception/
        ├── CallHistoryNotFoundException
        └── CallParticipantMismatchException
```

삭제 대상:

```
domain/matching/
├── domain/Match.java                                       ← 삭제
├── domain/MatchStatus.java                                 ← 삭제
├── dao/MatchRepository.java                                ← 삭제
├── exception/MatchNotFoundException.java                   ← 삭제
└── exception/MatchParticipantMismatchException.java        ← 삭제
```

## 4. 컴포넌트

### 4.1 `CallHistory` 엔티티

- `Match`를 rename하면서 `status` 컬럼/필드 제거, `isActive()` 추가.
- 필드: `id`, `roomId(UUID)`, `userAId`, `userBId`, `startedAt`, `endedAt(nullable)`. BaseTimeEntity 상속.
- 종료 여부의 단일 진실원은 `endedAt`. `endedAt == null`이면 진행 중.
- `start(...)`에서 `userAId = min(firstUserId, secondUserId)`, `userBId = max(...)` 정렬은 기존 `Match`와 동일하게 유지(블록/조회 인덱스 일관성).
- `end(LocalDateTime)`은 `!isActive()` 가드로 멱등.

```java
package com.lingring.domain.call.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "call_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_call_room_id",
                columnNames = "room_id"
        ),
        indexes = {
                @Index(name = "idx_call_user_a_id", columnList = "user_a_id"),
                @Index(name = "idx_call_user_b_id", columnList = "user_b_id")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CallHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "room_id", nullable = false, length = 36)
    private UUID roomId;

    @Column(name = "user_a_id", nullable = false)
    private Long userAId;

    @Column(name = "user_b_id", nullable = false)
    private Long userBId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    private CallHistory(
            @NonNull final UUID roomId,
            @NonNull final Long userAId,
            @NonNull final Long userBId,
            @NonNull final LocalDateTime startedAt
    ) {
        this.roomId = roomId;
        this.userAId = userAId;
        this.userBId = userBId;
        this.startedAt = startedAt;
    }

    public static CallHistory start(
            @NonNull final Long firstUserId,
            @NonNull final Long secondUserId,
            @NonNull final UUID roomId,
            @NonNull final LocalDateTime startedAt
    ) {
        final Long userAId = Math.min(firstUserId, secondUserId);
        final Long userBId = Math.max(firstUserId, secondUserId);
        return new CallHistory(roomId, userAId, userBId, startedAt);
    }

    public void end(@NonNull final LocalDateTime endedAt) {
        if (!isActive()) {
            return;
        }
        this.endedAt = endedAt;
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public boolean involves(@NonNull final Long userId) {
        return userAId.equals(userId) || userBId.equals(userId);
    }

    public Long callerUserId() {
        return userAId;
    }

    public Long calleeUserId() {
        return userBId;
    }

    public Long counterpartOf(@NonNull final Long userId) {
        if (userAId.equals(userId)) {
            return userBId;
        }
        if (userBId.equals(userId)) {
            return userAId;
        }
        throw new CallParticipantMismatchException(roomId, userId);
    }
}
```

### 4.2 `CallHistoryRepository`

```java
package com.lingring.domain.call.dao;

import com.lingring.domain.call.domain.CallHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallHistoryRepository extends JpaRepository<CallHistory, Long> {

    Optional<CallHistory> findByRoomId(UUID roomId);
}
```

### 4.3 `CallHistoryService`

`MatchingService`에서 통화 record 관련 메서드 3개를 이 서비스로 이전. 트랜잭션 어노테이션은 메서드 단위로 명시(룰 준수).

```java
package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallHistoryRepository;
import com.lingring.domain.call.domain.CallHistory;
import com.lingring.domain.call.exception.CallHistoryNotFoundException;
import com.lingring.global.util.DateTimeProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallHistoryService {

    private final CallHistoryRepository callHistoryRepository;
    private final DateTimeProvider dateTimeProvider;

    @Transactional(readOnly = true)
    public Optional<CallHistory> findByRoomId(final UUID roomId) {
        return callHistoryRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public CallHistory getByRoomId(final UUID roomId) {
        return callHistoryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CallHistoryNotFoundException(roomId));
    }

    @Transactional
    public void endCall(final UUID roomId) {
        final CallHistory callHistory = callHistoryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CallHistoryNotFoundException(roomId));
        callHistory.end(dateTimeProvider.now());
    }
}
```

### 4.4 예외 클래스

```java
package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.util.UUID;

public class CallHistoryNotFoundException extends NotFoundException {

    public CallHistoryNotFoundException(final UUID roomId) {
        super(ErrorCode.CALL_HISTORY_NOT_FOUND,
                "roomId가 %s인 통화 기록을 찾을 수 없습니다.".formatted(roomId));
    }
}
```

```java
package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;
import java.util.UUID;

public class CallParticipantMismatchException extends DomainException {

    public CallParticipantMismatchException(final UUID roomId, final Long userId) {
        super(
                ErrorCode.CALL_PARTICIPANT_MISMATCH,
                "userId %d는 roomId %s 통화의 참여자가 아닙니다.".formatted(userId, roomId)
        );
    }
}
```

### 4.5 `ErrorCode` 변경

기존 `Matching Error` 그룹을 `Call Error` 그룹으로 rename(주석)하고 enum 상수도 rename. 기존 두 상수는 다른 도메인에서 참조하지 않으므로 안전.

```java
// Call Error
CALL_HISTORY_NOT_FOUND(NOT_FOUND, "통화 기록을 찾을 수 없습니다."),
CALL_PARTICIPANT_MISMATCH(FORBIDDEN, "해당 통화의 참여자가 아닙니다."),
```

### 4.6 호출자 변경

#### 4.6.1 `MatchingService` (큐 메서드만 남김)

다음 메서드 제거(이동):
- `findByRoomId(UUID)` → `CallHistoryService.findByRoomId`
- `getByRoomId(UUID)` → `CallHistoryService.getByRoomId`
- `endMatch(UUID)` → `CallHistoryService.endCall`

`MatchRepository` 필드 제거. 남는 책임은 큐 enter/leave/status 조회뿐.

#### 4.6.2 `MatchingExecutor`

```diff
- private final MatchRepository matchRepository;
+ private final CallHistoryRepository callHistoryRepository;
  ...
- matchRepository.save(Match.start(self.userId(), partnerId, roomId, now));
+ callHistoryRepository.save(CallHistory.start(self.userId(), partnerId, roomId, now));
```

#### 4.6.3 `SignalingFacade`

```diff
- private final MatchingService matchingService;
+ private final CallHistoryService callHistoryService;
  ...
- final Match match = matchingService.getByRoomId(roomId);
+ final CallHistory callHistory = callHistoryService.getByRoomId(roomId);
  ...
- matchingService.endMatch(roomId);
+ callHistoryService.endCall(roomId);
  ...
- final Optional<Match> matchOpt = matchingService.findByRoomId(roomId);
+ final Optional<CallHistory> callHistoryOpt = callHistoryService.findByRoomId(roomId);
  ...
- if (match.getStatus() == MatchStatus.ENDED) { return; }
+ if (!callHistory.isActive()) { return; }
```

`Match` import 전부 `CallHistory`로 교체. `MatchStatus` import 제거.

#### 4.6.4 `SignalingMessageRouter`, `SignalingReadyCoordinator`

`Match` 매개변수/지역 변수 타입을 `CallHistory`로 변경. 호출 메서드 시그니처(`involves`, `counterpartOf`, `callerUserId/calleeUserId`)는 동일 → 본문 무변경.

## 5. DB 마이그레이션

운영 배포 시 적용. PR에는 SQL 스니펫만 명시(자동 마이그레이션 도구는 현 프로젝트에 없음).

```sql
-- 1) 테이블명 rename
ALTER TABLE match_call RENAME TO call_history;

-- 2) status 컬럼 제거 (endedAt이 단일 진실원)
ALTER TABLE call_history DROP COLUMN status;

-- 3) 인덱스/유니크 제약 이름 정리
ALTER TABLE call_history RENAME INDEX uk_match_room_id TO uk_call_room_id;
ALTER TABLE call_history RENAME INDEX idx_match_user_a_id TO idx_call_user_a_id;
ALTER TABLE call_history RENAME INDEX idx_match_user_b_id TO idx_call_user_b_id;
```

`@Table(name = "call_history")`로 매핑되므로 마이그레이션 미적용 환경에서는 기동 시 테이블을 찾지 못해 실패. 배포 순서 합의 필요.

## 6. `table.md` 보정

`call_history` 섹션을 코드 기준으로 갱신:

```sql
CREATE TABLE `call_history` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `room_id`    VARCHAR(36)  NOT NULL,
  `user_a_id`  BIGINT       NOT NULL,
  `user_b_id`  BIGINT       NOT NULL,
  `started_at` DATETIME     NOT NULL,
  `ended_at`   DATETIME     NULL,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_room_id` (`room_id`),
  KEY `idx_call_user_a_id` (`user_a_id`),
  KEY `idx_call_user_b_id` (`user_b_id`),
  CONSTRAINT `fk_call_history_user_a` FOREIGN KEY (`user_a_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_call_history_user_b` FOREIGN KEY (`user_b_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

원래 `table.md`에 있던 `status`/`duration_seconds`/`(user_a_id, started_at DESC)` 복합 인덱스는 제거. 향후 통화 목록 조회 API 도입 시 복합 인덱스를 별도 스펙에서 다룸.

## 7. 테스트 계획

룰: API·도메인 로직 추가/수정 시 대응 테스트를 함께 작성.

### 7.1 `CallHistoryTest` (단위, `MatchTest` rename + isActive 케이스 추가)

- `start(...)`: `firstUserId > secondUserId`로 호출해도 `userAId < userBId`로 정렬된다.
- `start(...)` 직후: `isActive() == true`, `endedAt == null`.
- `end(t)` 호출: `isActive() == false`, `endedAt == t`.
- `end(t)` 두 번 호출(멱등): 두 번째 시각으로 덮어쓰지 않고 첫 `endedAt` 유지.
- `involves(userId)`: 참여자 두 명 각각 true, 미참여자 false.
- `counterpartOf(userId)`: 참여자에 대해 상대 ID 반환, 미참여자에 대해 `CallParticipantMismatchException` (ErrorCode `CALL_PARTICIPANT_MISMATCH`).

### 7.2 `CallHistoryServiceTest` (`@SpringBootTest` + `@Transactional`)

기존 `MatchingServiceTest`에서 통화 record 관련 케이스를 분리해 옮김.
- `findByRoomId`: 존재하면 `Optional.of(...)`, 없으면 `Optional.empty()`.
- `getByRoomId`: 존재하면 엔티티, 없으면 `CallHistoryNotFoundException` (ErrorCode `CALL_HISTORY_NOT_FOUND`).
- `endCall`: 진행 중인 record를 종료(`isActive() == false`, `endedAt == FixedDateTimeProvider.now()`).
- `endCall` 미존재 roomId: `CallHistoryNotFoundException`.
- `endCall` 두 번 호출(멱등): 두 번째 호출은 `endedAt` 변경 없음.

### 7.3 `MatchingServiceTest` 잔여

- 큐 enter/leave/status 시나리오만 남기고, 이전된 메서드 케이스 삭제.
- `MatchRepository` 의존 제거에 따른 setup 단순화.

### 7.4 `MatchingExecutorTest`

- 기존 케이스 유지, `matchRepository.save(Match...)` 호출 검증 → `callHistoryRepository.save(CallHistory...)` 검증으로 변경.

### 7.5 `SignalingFacadeTest`, `SignalingMessageRouterTest`, `SignalingReadyCoordinatorTest`, `SignalingWebSocketHandlerIntegrationTest`

- `Match` 타입을 `CallHistory`로 교체.
- `matchingService.endMatch / getByRoomId / findByRoomId` 모킹을 `callHistoryService` 동등 메서드로 교체.
- `match.getStatus() == ENDED` 검증을 `!callHistory.isActive()` 검증으로 변경.

## 8. 범위 밖 (후속 별도 스펙)

- **이벤트 기반 분리**: `MatchingExecutor → CallHistoryRepository` 직접 의존을 `ApplicationEventPublisher`로 교체. 도메인 이벤트(`MatchCommittedEvent` 등) 도입.
- **`status` 의미 세분화**: FAILED/COMPLETED 같은 상태를 도입하려면 먼저 "통화 record가 정확히 무엇을 가리키는가"(매칭 commit 시점 vs 실제 P2P 성립 시점)를 정의해야 함.
- **`call_content`, `call_analyze`** 도입: 이번 PR에서 다루지 않음.
- **`duration_seconds`, 통화 통계 API**: 사용처가 정해질 때 derived 컬럼 또는 derived 쿼리로 별도 결정.
- **통화 목록 조회 API용 인덱스**: 사용 패턴이 정해질 때 추가.

## 9. 체크리스트

- [ ] `domain/call/domain/CallHistory.java` 신규
- [ ] `domain/call/dao/CallHistoryRepository.java` 신규
- [ ] `domain/call/service/CallHistoryService.java` 신규
- [ ] `domain/call/exception/CallHistoryNotFoundException.java` 신규
- [ ] `domain/call/exception/CallParticipantMismatchException.java` 신규
- [ ] `global/error/ErrorCode`에서 `MATCH_NOT_FOUND` → `CALL_HISTORY_NOT_FOUND`, `MATCH_PARTICIPANT_MISMATCH` → `CALL_PARTICIPANT_MISMATCH` rename + 그룹 주석 변경
- [ ] `domain/matching/domain/Match.java` 삭제
- [ ] `domain/matching/domain/MatchStatus.java` 삭제
- [ ] `domain/matching/dao/MatchRepository.java` 삭제
- [ ] `domain/matching/exception/MatchNotFoundException.java` 삭제
- [ ] `domain/matching/exception/MatchParticipantMismatchException.java` 삭제
- [ ] `MatchingService`에서 `findByRoomId/getByRoomId/endMatch` 제거 + `MatchRepository` 의존 제거
- [ ] `MatchingExecutor` 의존을 `CallHistoryRepository`로
- [ ] `SignalingFacade` 의존을 `CallHistoryService`로 + 타입 교체 + `isActive()` 사용
- [ ] `SignalingMessageRouter` 매개변수 타입 교체
- [ ] `SignalingReadyCoordinator` 매개변수 타입 교체
- [ ] 테스트: `CallHistoryTest` rename + `isActive` 케이스 추가
- [ ] 테스트: `CallHistoryServiceTest` 신규(이전 + 분리)
- [ ] 테스트: `MatchingServiceTest` 잔여 정리
- [ ] 테스트: `MatchingExecutorTest`, Signaling 테스트들 타입 교체
- [ ] `table.md` `call_history` 섹션 코드 기준으로 갱신
- [ ] DB 마이그레이션 SQL을 PR 본문에 명시
