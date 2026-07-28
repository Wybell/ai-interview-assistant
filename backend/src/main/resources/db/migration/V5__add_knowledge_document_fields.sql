ALTER TABLE knowledge_topic
    ADD COLUMN source_file_name VARCHAR(255) NULL AFTER title,
    ADD COLUMN document_content LONGTEXT NULL AFTER key_points;
