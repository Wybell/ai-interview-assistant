-- Keep the official DeepSeek model as the stable system default.
-- Terra and Luna remain optional models backed by the external custom provider.
-- This migration repairs databases that already applied V6 without rewriting V6.

UPDATE ai_model
SET enabled = CASE
        WHEN provider = 'deepseek' AND model_code = 'deepseek-v4-flash' THEN 1
        WHEN provider = 'custom' AND model_code IN ('gpt-5.6-terra', 'gpt-5.6-luna') THEN 1
        ELSE 0
    END,
    sort_order = CASE
        WHEN provider = 'deepseek' AND model_code = 'deepseek-v4-flash' THEN 10
        WHEN provider = 'custom' AND model_code = 'gpt-5.6-terra' THEN 20
        WHEN provider = 'custom' AND model_code = 'gpt-5.6-luna' THEN 30
        ELSE sort_order
    END,
    update_time = CURRENT_TIMESTAMP;

UPDATE ai_model_policy
SET default_ai_model_id = (
        SELECT id
        FROM ai_model
        WHERE provider = 'deepseek' AND model_code = 'deepseek-v4-flash'
    ),
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
