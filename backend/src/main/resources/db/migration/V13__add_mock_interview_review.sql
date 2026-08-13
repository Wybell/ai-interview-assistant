CREATE TABLE mock_interview_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    answered_turn_count SMALLINT UNSIGNED NOT NULL,
    main_question_count TINYINT UNSIGNED NOT NULL,
    follow_up_count TINYINT UNSIGNED NOT NULL,
    average_score DECIMAL(4,2) NULL,
    overall_feedback TEXT NOT NULL,
    strengths TEXT NOT NULL,
    improvement_areas TEXT NOT NULL,
    action_items TEXT NOT NULL,
    ai_model_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_mock_interview_review_session UNIQUE (session_id),
    CONSTRAINT fk_mock_interview_review_session FOREIGN KEY (session_id)
        REFERENCES mock_interview_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_mock_interview_review_model FOREIGN KEY (ai_model_id) REFERENCES ai_model (id),
    CONSTRAINT chk_mock_interview_review_average_score
        CHECK (average_score IS NULL OR (average_score >= 0 AND average_score <= 10)),
    INDEX idx_mock_interview_review_created (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
