-- Smart DX Backend - Database Initialization
-- This script creates databases for each service

-- Property DB
CREATE DATABASE IF NOT EXISTS property_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Retail DB
CREATE DATABASE IF NOT EXISTS retail_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Grant permissions (adjust user as needed)
-- GRANT ALL PRIVILEGES ON property_db.* TO 'app_user'@'%';
-- GRANT ALL PRIVILEGES ON retail_db.* TO 'app_user'@'%';
-- FLUSH PRIVILEGES;
