-- DeepSeek's direct Chat Completions API requires the full official model identifier.
-- Keep the existing ai_model row ID so policy, preference, and score foreign keys remain valid.
UPDATE ai_model
SET model_code = 'deepseek-v4-flash',
    update_time = CURRENT_TIMESTAMP
WHERE provider = 'deepseek'
  AND model_code = 'v4-flash';
