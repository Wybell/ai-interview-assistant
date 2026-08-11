CREATE TABLE resume_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    extracted_content MEDIUMTEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_resume_document_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    INDEX idx_resume_document_user_created (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_interview_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    target_position VARCHAR(100) NOT NULL,
    interview_round VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    question_count TINYINT UNSIGNED NOT NULL DEFAULT 0,
    ai_model_id BIGINT NOT NULL,
    summary TEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_time DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mock_interview_session_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_mock_interview_session_resume FOREIGN KEY (resume_id) REFERENCES resume_document (id),
    CONSTRAINT fk_mock_interview_session_model FOREIGN KEY (ai_model_id) REFERENCES ai_model (id),
    CONSTRAINT chk_mock_interview_round CHECK (interview_round IN ('FIRST', 'SECOND', 'THIRD')),
    CONSTRAINT chk_mock_interview_status CHECK (status IN ('ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_mock_interview_question_count CHECK (question_count BETWEEN 0 AND 8),
    INDEX idx_mock_interview_session_user_created (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_interview_turn (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    sequence_no TINYINT UNSIGNED NOT NULL,
    question VARCHAR(2000) NOT NULL,
    user_answer VARCHAR(5000) NULL,
    score TINYINT UNSIGNED NULL,
    correct_answer TEXT NULL,
    suggestion TEXT NULL,
    focus_tag VARCHAR(100) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_mock_interview_turn_session FOREIGN KEY (session_id)
        REFERENCES mock_interview_session (id) ON DELETE CASCADE,
    CONSTRAINT uk_mock_interview_turn_sequence UNIQUE (session_id, sequence_no),
    CONSTRAINT chk_mock_interview_turn_score CHECK (score IS NULL OR score BETWEEN 0 AND 10),
    INDEX idx_mock_interview_turn_session_sequence (session_id, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
