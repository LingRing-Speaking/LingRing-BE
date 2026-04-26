
-- ============================================
-- 공통 설정
-- ============================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- user: 사용자
-- ============================================
CREATE TABLE `user` (
`id`            BIGINT       NOT NULL AUTO_INCREMENT,
`name`          VARCHAR(500) NOT NULL,
`profile_image` VARCHAR(500) NULL,
`created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_stats: 사용자 통계 (1:1)
-- ============================================
CREATE TABLE `user_stats` (
`id`                     BIGINT        NOT NULL AUTO_INCREMENT,
`user_id`                BIGINT        NOT NULL,
`level`                  ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NOT NULL DEFAULT 'BEGINNER',
`manner_temperature`     DECIMAL(4,1)  NOT NULL DEFAULT 36.5,
`total_call_count`       INT           NOT NULL DEFAULT 0,
`current_streak_days`    INT           NOT NULL DEFAULT 0,
`expression_count`       INT           NOT NULL DEFAULT 0,
`last_study_date`        DATE          NULL,
`created_at`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_stats_user_id` (`user_id`),
CONSTRAINT `fk_user_stats_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_expression: 저장한 표현 (1:N)
-- ============================================
CREATE TABLE `user_expression` (
`id`         BIGINT       NOT NULL AUTO_INCREMENT,
`user_id`    BIGINT       NOT NULL,
`expression` VARCHAR(500) NOT NULL,
`meaning`    VARCHAR(500) NOT NULL,
`created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `idx_user_expression_user_created` (`user_id`, `created_at` DESC),
CONSTRAINT `fk_user_expression_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_history: 통화 기록
-- ============================================
CREATE TABLE `call_history` (
`id`               BIGINT   NOT NULL AUTO_INCREMENT,
`user_a_id`        BIGINT   NOT NULL,
`user_b_id`        BIGINT   NOT NULL,
`status`           ENUM('IN_PROGRESS', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'IN_PROGRESS',
`duration_seconds` INT      NULL,
`started_at`       DATETIME NOT NULL,
`ended_at`         DATETIME NULL,
`created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `idx_call_history_user_a_started` (`user_a_id`, `started_at` DESC),
KEY `idx_call_history_user_b_started` (`user_b_id`, `started_at` DESC),
CONSTRAINT `fk_call_history_user_a` FOREIGN KEY (`user_a_id`) REFERENCES `user`(`id`),
CONSTRAINT `fk_call_history_user_b` FOREIGN KEY (`user_b_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_content: 통화 내용 (1:1)
-- ============================================
CREATE TABLE `call_content` (
`id`              BIGINT   NOT NULL AUTO_INCREMENT,
`call_history_id` BIGINT   NOT NULL,
`content`         LONGTEXT NOT NULL,
`created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE KEY `uk_call_content_call_history_id` (`call_history_id`),
CONSTRAINT `fk_call_content_call_history` FOREIGN KEY (`call_history_id`) REFERENCES `call_history`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_block: 사용자 차단
-- ============================================
CREATE TABLE `user_block` (
`id`              BIGINT   NOT NULL AUTO_INCREMENT,
`user_id`         BIGINT   NOT NULL,
`blocked_user_id` BIGINT   NOT NULL,
`created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_block_user_blocked` (`user_id`, `blocked_user_id`),
KEY `idx_user_block_user_id` (`user_id`),
CONSTRAINT `fk_user_block_user`         FOREIGN KEY (`user_id`)         REFERENCES `user`(`id`),
CONSTRAINT `fk_user_block_blocked_user` FOREIGN KEY (`blocked_user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- user_report: 사용자 신고
-- ============================================
CREATE TABLE `user_report` (
`id`               BIGINT       NOT NULL AUTO_INCREMENT,
`user_id`          BIGINT       NOT NULL,
`reported_user_id` BIGINT       NOT NULL,
`reason`           ENUM('INAPPROPRIATE_CONVERSATION', 'BAD_MANNERS', 'OTHER') NOT NULL,
`description`      VARCHAR(200) NOT NULL,
`created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `idx_user_report_user_id`          (`user_id`),
KEY `idx_user_report_reported_user_id` (`reported_user_id`),
CONSTRAINT `fk_user_report_user`          FOREIGN KEY (`user_id`)          REFERENCES `user`(`id`),
CONSTRAINT `fk_user_report_reported_user` FOREIGN KEY (`reported_user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- call_analyze: 통화 분석 (참가자별 1개)
-- ============================================
CREATE TABLE `call_analyze` (
`id`               BIGINT   NOT NULL AUTO_INCREMENT,
`user_id`          BIGINT   NOT NULL,
`call_history_id`  BIGINT   NOT NULL,
`good_points_json` JSON     NULL,
`feedback_json`    JSON     NULL,
`status`           ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
`created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE KEY `uk_call_analyze_call_user` (`call_history_id`, `user_id`),
KEY `idx_call_analyze_user_created` (`user_id`, `created_at` DESC),
CONSTRAINT `fk_call_analyze_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
CONSTRAINT `fk_call_analyze_call_history` FOREIGN KEY (`call_history_id`) REFERENCES `call_history`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
