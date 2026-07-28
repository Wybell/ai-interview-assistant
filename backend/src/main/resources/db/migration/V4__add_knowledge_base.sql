ALTER TABLE `user` ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER' AFTER password;

CREATE TABLE knowledge_topic (
    id BIGINT NOT NULL AUTO_INCREMENT,
    direction VARCHAR(16) NOT NULL,
    language VARCHAR(64) NOT NULL,
    category VARCHAR(128) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    key_points JSON NOT NULL,
    published TINYINT UNSIGNED NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_knowledge_topic_filter (direction, language, published, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    difficulty VARCHAR(16) NOT NULL DEFAULT '中级',
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_knowledge_question_topic FOREIGN KEY (topic_id) REFERENCES knowledge_topic(id) ON DELETE CASCADE,
    INDEX idx_knowledge_question_topic (topic_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
