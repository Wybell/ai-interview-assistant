-- Extend mock interviews without changing the already-applied V1-V9 migrations.

ALTER TABLE mock_interview_session
    DROP CHECK chk_mock_interview_round,
    DROP CHECK chk_mock_interview_question_count,
    ADD COLUMN question_limit TINYINT UNSIGNED NOT NULL DEFAULT 8 AFTER question_count,
    ADD CONSTRAINT chk_mock_interview_round
        CHECK (interview_round IN ('FIRST', 'SECOND', 'THIRD', 'HR')),
    ADD CONSTRAINT chk_mock_interview_question_count
        CHECK (question_count BETWEEN 0 AND question_limit AND question_limit IN (4, 8, 10, 12));

ALTER TABLE mock_interview_turn
    DROP INDEX uk_mock_interview_turn_sequence,
    ADD COLUMN turn_type VARCHAR(16) NOT NULL DEFAULT 'MAIN' AFTER sequence_no,
    ADD COLUMN parent_turn_id BIGINT NULL AFTER turn_type,
    ADD COLUMN follow_up_no TINYINT UNSIGNED NULL AFTER parent_turn_id,
    ADD CONSTRAINT chk_mock_interview_turn_type
        CHECK (turn_type IN ('MAIN', 'FOLLOW_UP')),
    ADD CONSTRAINT chk_mock_interview_follow_up_no
        CHECK (follow_up_no IS NULL OR follow_up_no BETWEEN 1 AND 2),
    ADD CONSTRAINT fk_mock_interview_turn_parent
        FOREIGN KEY (parent_turn_id) REFERENCES mock_interview_turn (id),
    ADD INDEX idx_mock_interview_turn_parent (parent_turn_id);
