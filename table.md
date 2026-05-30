-- ============================================
-- LingRing 운영 DB 스키마 (엔티티 기준, prod 적용용)
--
-- 규칙
--  - 테이블명은 단수형 기본. SQL 예약어인 USER/CALL만 복수로 회피 → `users`, `calls`
--  - enum은 @Enumerated(STRING) 매핑이므로 네이티브 ENUM이 아닌 VARCHAR (값은 주석 참고)
--  - 시간 컬럼은 DATETIME(6) (MySQL 마이크로초)
--  - created_at/updated_at은 Spring Data Auditing이 채움. DB DEFAULT는 수동 INSERT 대비용
--  - 연관관계는 모두 ID 간접참조라 JPA가 FK를 생성하지 않음. 아래 FK는 참조 무결성을 위해 수동 추가
--  - prod는 ddl-auto: validate → 컬럼 타입이 엔티티와 정확히 일치해야 부팅됨
-- ============================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- users : 사용자 (소셜 로그인) — USER 예약어 회피로 복수
-- ============================================
CREATE TABLE `users` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
  `provider`             VARCHAR(20)   NOT NULL,                    -- KAKAO / APPLE
  `provider_user_id`     VARCHAR(255)  NOT NULL,
  `name`                 VARCHAR(30)   NOT NULL,
  `profile_image`        VARCHAR(500)  NULL,
  `agreed_terms_version` VARCHAR(20)   NULL,
  `agreed_at`            DATETIME(6)   NULL,
  `apple_refresh_token`  VARCHAR(1024) NULL,                        -- 애플리케이션 레벨 암호화 저장
  `created_at`           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_provider_provider_user_id` (`provider`, `provider_user_id`),
  UNIQUE KEY `uk_users_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_stats : 사용자 통계 (1:1)
-- ============================================
CREATE TABLE `user_stats` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`             BIGINT       NOT NULL,
  `level`               VARCHAR(255) NOT NULL,                      -- BEGINNER / INTERMEDIATE / ADVANCED
  `manner_temperature`  DECIMAL(4,1) NOT NULL,
  `total_call_count`    INT          NOT NULL,
  `current_streak_days` INT          NOT NULL,
  `expression_count`    INT          NOT NULL,
  `last_study_date`     DATE         NULL,
  `created_at`          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_stats_user_id` (`user_id`),
  CONSTRAINT `fk_user_stats_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_expression : 저장한 표현 (1:N)
-- ============================================
CREATE TABLE `user_expression` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `expression` VARCHAR(500) NOT NULL,
  `meaning`    VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_user_expression_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_user_expression_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- recommended_expression : 추천 표현 (오늘의 추천)
-- ============================================
CREATE TABLE `recommended_expression` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `expression` VARCHAR(500) NOT NULL,
  `meaning`    VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- icebreaker : 아이스브레이커 (무작위 조회)
-- ============================================
CREATE TABLE `icebreaker` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `expression` VARCHAR(500) NOT NULL,
  `meaning`    VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
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
  `created_at`   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calls_room_id` (`room_id`),
  KEY `idx_calls_user_a_id` (`user_a_id`),
  KEY `idx_calls_user_b_id` (`user_b_id`),
  CONSTRAINT `fk_calls_user_a` FOREIGN KEY (`user_a_id`) REFERENCES `users`(`id`),
  CONSTRAINT `fk_calls_user_b` FOREIGN KEY (`user_b_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_transcript : 통화 STT 결과 (1:1)
-- ============================================
CREATE TABLE `call_transcript` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `call_id`    BIGINT      NOT NULL,
  `content`    JSON        NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_transcript_call_id` (`call_id`),
  CONSTRAINT `fk_call_transcript_call` FOREIGN KEY (`call_id`) REFERENCES `calls`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_analysis : 통화 분석 (참가자별 1개)
-- ============================================
CREATE TABLE `call_analysis` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `call_id`          BIGINT       NOT NULL,
  `user_id`          BIGINT       NOT NULL,
  `status`           VARCHAR(32)  NOT NULL,                         -- PROCESSING / COMPLETED / FAILED
  `model_identifier` VARCHAR(128) NULL,
  `result`           JSON         NULL,
  `requested`        BIT(1)       NOT NULL DEFAULT b'0',            -- boolean (JPA boolean → bit)
  `created_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_analysis_call_id_user_id` (`call_id`, `user_id`),
  CONSTRAINT `fk_call_analysis_call` FOREIGN KEY (`call_id`) REFERENCES `calls`(`id`),
  CONSTRAINT `fk_call_analysis_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_recording : 통화 녹음 (참가자별 1개)
-- ============================================
CREATE TABLE `call_recording` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `call_id`       BIGINT       NOT NULL,
  `user_id`       BIGINT       NOT NULL,
  `recording_key` VARCHAR(512) NOT NULL,
  `content_type`  VARCHAR(100) NOT NULL,
  `status`        VARCHAR(32)  NOT NULL,                            -- UPLOADED
  `created_at`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_recording_call_user` (`call_id`, `user_id`),
  KEY `idx_call_recording_call_id` (`call_id`),
  KEY `idx_call_recording_user_id` (`user_id`),
  CONSTRAINT `fk_call_recording_call` FOREIGN KEY (`call_id`) REFERENCES `calls`(`id`),
  CONSTRAINT `fk_call_recording_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_block : 사용자 차단
-- ============================================
CREATE TABLE `user_block` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT      NOT NULL,
  `blocked_user_id` BIGINT      NOT NULL,
  `created_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_block_user_blocked` (`user_id`, `blocked_user_id`),
  KEY `idx_user_block_user_id` (`user_id`),
  CONSTRAINT `fk_user_block_user`         FOREIGN KEY (`user_id`)         REFERENCES `users`(`id`),
  CONSTRAINT `fk_user_block_blocked_user` FOREIGN KEY (`blocked_user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_report : 사용자 신고
-- ============================================
CREATE TABLE `user_report` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT       NULL,
  `reported_user_id` BIGINT       NOT NULL,
  `reason`           VARCHAR(255) NOT NULL,                         -- INAPPROPRIATE_CONVERSATION / BAD_MANNERS / OTHER
  `description`      VARCHAR(200) NOT NULL,
  `created_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_user_report_user_id`          (`user_id`),
  KEY `idx_user_report_reported_user_id` (`reported_user_id`),
  CONSTRAINT `fk_user_report_user`          FOREIGN KEY (`user_id`)          REFERENCES `users`(`id`),
  CONSTRAINT `fk_user_report_reported_user` FOREIGN KEY (`reported_user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- withdrawal_log : 탈퇴 로그 (익명 — 사용자 식별자 미보관)
-- ============================================
CREATE TABLE `withdrawal_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `reason`      VARCHAR(30)  NOT NULL,                              -- NO_GOOD_MATCH / NO_PROGRESS / BUGGY / RARELY_USE / MISSING_FEATURE / OTHER
  `description` VARCHAR(200) NULL,
  `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
