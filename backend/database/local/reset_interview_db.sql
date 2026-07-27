-- LOCAL DEVELOPMENT ONLY.
-- This script permanently deletes local AI Interview Assistant tables and data.
-- Do not run this script against a cloud, shared, test, or production database.
-- Select the local interview_db database in Navicat before executing.

DROP VIEW IF EXISTS mistake_view;
DROP TABLE IF EXISTS answer_record;
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS flyway_schema_history;
SELECT DATABASE() AS current_database;
SHOW TABLES;