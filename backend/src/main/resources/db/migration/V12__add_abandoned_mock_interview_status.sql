ALTER TABLE mock_interview_session
    DROP CHECK chk_mock_interview_status,
    ADD CONSTRAINT chk_mock_interview_status_v12
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'ENDED_EARLY'));
