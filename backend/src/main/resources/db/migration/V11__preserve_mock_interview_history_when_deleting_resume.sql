-- Preserve completed interview history after its source resume is removed.
ALTER TABLE mock_interview_session
    DROP FOREIGN KEY fk_mock_interview_session_resume,
    MODIFY COLUMN resume_id BIGINT NULL,
    ADD COLUMN resume_file_name_snapshot VARCHAR(255) NULL AFTER resume_id,
    ADD CONSTRAINT fk_mock_interview_session_resume_set_null
        FOREIGN KEY (resume_id) REFERENCES resume_document (id) ON DELETE SET NULL;

UPDATE mock_interview_session session
JOIN resume_document resume ON resume.id = session.resume_id
SET session.resume_file_name_snapshot = resume.original_file_name
WHERE session.resume_file_name_snapshot IS NULL;
