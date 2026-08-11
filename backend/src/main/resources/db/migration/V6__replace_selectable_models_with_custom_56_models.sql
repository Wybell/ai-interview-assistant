-- The custom Responses-compatible provider exposes only the approved 5.6 models.
-- Existing answer records retain their historical model foreign keys.

ALTER TABLE ai_model DROP CHECK chk_ai_model_provider;

ALTER TABLE ai_model
    ADD CONSTRAINT chk_ai_model_provider
        CHECK (provider IN ('dashscope', 'change2proapi', 'deepseek', 'custom'));

UPDATE ai_model
SET enabled = 0,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO ai_model (provider, model_code, display_name, enabled, sort_order)
VALUES
    ('custom', 'gpt-5.6-terra', '5.6 Terra', 1, 10),
    ('custom', 'gpt-5.6-luna', '5.6 Luna', 1, 20);

UPDATE ai_model_policy
SET default_ai_model_id = (
        SELECT id
        FROM ai_model
        WHERE provider = 'custom' AND model_code = 'gpt-5.6-luna'
    ),
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
