-- ============================================
-- LingRing 운영 DB 스키마 (dev의 Hibernate 생성 스키마 기준 — prod 적용용)
--
-- 작성 기준
--  - dev(`ddl-auto: update`)가 실제로 생성한 스키마를 그대로 미러링 → prod `ddl-auto: validate` 통과 보장
--  - 테이블명: 단수 기본. SQL 예약어 USER/CALL만 복수(`users`, `calls`), 집합 명사 `user_stats`는 복수 유지
--  - enum은 @Enumerated(STRING) → Hibernate(MySQLDialect)가 **네이티브 ENUM**으로 생성 (값은 알파벳순)
--  - 시간 컬럼은 DATETIME(6). created_at/updated_at은 Spring Data Auditing이 앱에서 채우므로 DB DEFAULT 없음
--  - boolean은 BIT(1)로 매핑
--  - 연관관계가 전부 ID 간접참조라 Hibernate가 FK를 생성하지 않음 → 아래에도 FK 없음
--    (참조 무결성을 DB 레벨에서 강제하고 싶으면 별도 ALTER로 추가 — validate에는 영향 없음)
--  - charset/collation은 utf8mb4 가정 (서버 기본값에 맞춰 조정)
-- ============================================
SET NAMES utf8mb4;

-- ============================================
-- users : 사용자 (소셜 로그인) — USER 예약어 회피로 복수
-- ============================================
CREATE TABLE `users` (
  `id`                   BIGINT                  NOT NULL AUTO_INCREMENT,
  `provider`             ENUM('APPLE','KAKAO')   NOT NULL,
  `provider_user_id`     VARCHAR(255)            NOT NULL,
  `name`                 VARCHAR(30)             NOT NULL,
  `profile_image`        VARCHAR(500)            NULL,
  `agreed_terms_version` VARCHAR(20)             NULL,
  `agreed_at`            DATETIME(6)             NULL,
  `apple_refresh_token`  VARCHAR(1024)           NULL,
  `created_at`           DATETIME(6)             NOT NULL,
  `updated_at`           DATETIME(6)             NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_provider_provider_user_id` (`provider`, `provider_user_id`),
  UNIQUE KEY `uk_users_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_stats : 사용자 통계 (1:1) — 집합 명사라 복수 유지
-- ============================================
CREATE TABLE `user_stats` (
  `id`                  BIGINT                                       NOT NULL AUTO_INCREMENT,
  `user_id`             BIGINT                                       NOT NULL,
  `level`               ENUM('ADVANCED','BEGINNER','INTERMEDIATE')   NOT NULL,
  `manner_temperature`  DECIMAL(4,1)                                 NOT NULL,
  `total_call_count`    INT                                          NOT NULL,
  `current_streak_days` INT                                          NOT NULL,
  `expression_count`    INT                                          NOT NULL,
  `last_study_date`     DATE                                         NULL,
  `created_at`          DATETIME(6)                                  NOT NULL,
  `updated_at`          DATETIME(6)                                  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_stats_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_expression : 저장한 표현 (1:N)
-- ============================================
CREATE TABLE `user_expression` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `expression` VARCHAR(500) NOT NULL,
  `meaning`    VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6)  NOT NULL,
  `updated_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_expression_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- recommended_expression : 추천 표현 (오늘의 추천)
-- ============================================
CREATE TABLE `recommended_expression` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `expression` VARCHAR(500) NOT NULL,
  `meaning`    VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6)  NOT NULL,
  `updated_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- icebreaker : 아이스브레이커 (무작위 조회)
-- ============================================
CREATE TABLE `icebreaker` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `expression` VARCHAR(500) NOT NULL,
  `meaning`    VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6)  NOT NULL,
  `updated_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- calls : 통화 — CALL 예약어 회피로 복수
-- ============================================
CREATE TABLE `calls` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `room_id`      VARCHAR(36) NOT NULL,
  `user_a_id`    BIGINT      NULL,
  `user_b_id`    BIGINT      NULL,
  `started_at`   DATETIME(6) NOT NULL,
  `ended_at`     DATETIME(6) NULL,
  `duration_sec` BIGINT      NULL,
  `created_at`   DATETIME(6) NOT NULL,
  `updated_at`   DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calls_room_id` (`room_id`),
  KEY `idx_calls_user_a_id` (`user_a_id`),
  KEY `idx_calls_user_b_id` (`user_b_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_transcript : 통화 STT 결과 (1:1)
-- ============================================
CREATE TABLE `call_transcript` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `call_id`    BIGINT      NOT NULL,
  `content`    JSON        NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_transcript_call_id` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_analysis : 통화 분석 (참가자별 1개)
-- ============================================
CREATE TABLE `call_analysis` (
  `id`               BIGINT                                    NOT NULL AUTO_INCREMENT,
  `call_id`          BIGINT                                    NOT NULL,
  `user_id`          BIGINT                                    NOT NULL,
  `status`           ENUM('COMPLETED','FAILED','PROCESSING')   NOT NULL,
  `model_identifier` VARCHAR(128)                              NULL,
  `result`           JSON                                      NULL,
  `requested`        BIT(1)                                    NOT NULL DEFAULT b'0',
  `created_at`       DATETIME(6)                               NOT NULL,
  `updated_at`       DATETIME(6)                               NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_analysis_call_id_user_id` (`call_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_recording : 통화 녹음 (참가자별 1개)
-- ============================================
CREATE TABLE `call_recording` (
  `id`            BIGINT            NOT NULL AUTO_INCREMENT,
  `call_id`       BIGINT            NOT NULL,
  `user_id`       BIGINT            NOT NULL,
  `recording_key` VARCHAR(512)      NOT NULL,
  `content_type`  VARCHAR(100)      NOT NULL,
  `status`        ENUM('UPLOADED')  NOT NULL,
  `created_at`    DATETIME(6)       NOT NULL,
  `updated_at`    DATETIME(6)       NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_recording_call_user` (`call_id`, `user_id`),
  KEY `idx_call_recording_call_id` (`call_id`),
  KEY `idx_call_recording_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_block : 사용자 차단
-- ============================================
CREATE TABLE `user_block` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT      NOT NULL,
  `blocked_user_id` BIGINT      NOT NULL,
  `created_at`      DATETIME(6) NOT NULL,
  `updated_at`      DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_block_user_blocked` (`user_id`, `blocked_user_id`),
  KEY `idx_user_block_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_report : 사용자 신고
-- ============================================
CREATE TABLE `user_report` (
  `id`               BIGINT                                                     NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT                                                     NULL,
  `reported_user_id` BIGINT                                                     NOT NULL,
  `reason`           ENUM('BAD_MANNERS','INAPPROPRIATE_CONVERSATION','OTHER')   NOT NULL,
  `description`      VARCHAR(200)                                               NOT NULL,
  `created_at`       DATETIME(6)                                                NOT NULL,
  `updated_at`       DATETIME(6)                                                NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_report_user_id`          (`user_id`),
  KEY `idx_user_report_reported_user_id` (`reported_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- withdrawal_log : 탈퇴 로그 (익명 — 사용자 식별자 미보관)
-- ============================================
CREATE TABLE `withdrawal_log` (
  `id`          BIGINT                                                                              NOT NULL AUTO_INCREMENT,
  `reason`      ENUM('BUGGY','MISSING_FEATURE','NO_GOOD_MATCH','NO_PROGRESS','OTHER','RARELY_USE')  NOT NULL,
  `description` VARCHAR(200)                                                                        NULL,
  `created_at`  DATETIME(6)                                                                         NOT NULL,
  `updated_at`  DATETIME(6)                                                                         NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;