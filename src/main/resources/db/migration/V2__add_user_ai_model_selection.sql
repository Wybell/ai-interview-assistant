-- Flyway V2: add the catalog and per-user preference required for runtime AI model selection.
-- This migration intentionally contains no API keys, endpoints, or destructive statements.

CREATE TABLE ai_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(32) NOT NULL,
    model_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_model_provider_code UNIQUE (provider, model_code),
    CONSTRAINT chk_ai_model_provider
        CHECK (provider IN ('dashscope', 'change2proapi', 'deepseek')),
    CONSTRAINT chk_ai_model_enabled CHECK (enabled IN (0, 1)),
    INDEX idx_ai_model_enabled_sort (enabled, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO ai_model (provider, model_code, display_name, enabled, sort_order)
VALUES
    ('deepseek', 'v4-flash', 'DeepSeek V4 Flash', 1, 10),
    ('change2proapi', 'gpt-5.6-luna', 'GPT-5.6 Luna', 1, 20);

CREATE TABLE ai_model_policy (
    id TINYINT UNSIGNED NOT NULL,
    default_ai_model_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_ai_model_policy_singleton CHECK (id = 1),
    CONSTRAINT fk_ai_model_policy_default_model
        FOREIGN KEY (default_ai_model_id) REFERENCES ai_model (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO ai_model_policy (id, default_ai_model_id)
SELECT 1, id
FROM ai_model
WHERE provider = 'deepseek' AND model_code = 'v4-flash';

CREATE TABLE user_ai_preference (
    user_id BIGINT NOT NULL,
    ai_model_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_ai_preference_user
        FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_user_ai_preference_model
        FOREIGN KEY (ai_model_id) REFERENCES ai_model (id),
    INDEX idx_user_ai_preference_model (ai_model_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

ALTER TABLE answer_record
    ADD COLUMN score_ai_model_id BIGINT NULL AFTER suggestion,
    ADD CONSTRAINT fk_answer_record_score_ai_model
        FOREIGN KEY (score_ai_model_id) REFERENCES ai_model (id),
    ADD INDEX idx_answer_record_score_ai_model (score_ai_model_id);
