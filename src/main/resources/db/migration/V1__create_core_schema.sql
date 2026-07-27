-- Flyway V1: create the core AI Interview Assistant schema.
-- This migration intentionally contains no DROP statements.

CREATE TABLE `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE answer_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    question VARCHAR(2000) NOT NULL,
    user_answer VARCHAR(5000) NOT NULL,
    score TINYINT UNSIGNED NOT NULL,
    correct_answer TEXT NOT NULL,
    suggestion TEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_answer_record_user
        FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT chk_answer_record_score
        CHECK (score BETWEEN 0 AND 10),
    INDEX idx_answer_record_user_created (user_id, create_time),
    INDEX idx_answer_record_user_score (user_id, score),
    INDEX idx_answer_record_user_tag (user_id, tag)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
