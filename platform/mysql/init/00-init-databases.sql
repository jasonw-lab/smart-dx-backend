-- Smart DX Backend - Database Initialization
-- This script creates databases for each service

-- Smart DX DB (unified database for property, auth, system services)
CREATE DATABASE IF NOT EXISTS smart_dx_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Retail DB (separate for DEMO purposes)
CREATE DATABASE IF NOT EXISTS retail_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Grant permissions (adjust user as needed)
-- GRANT ALL PRIVILEGES ON smart_dx_db.* TO 'app_user'@'%';
-- GRANT ALL PRIVILEGES ON retail_db.* TO 'app_user'@'%';
-- FLUSH PRIVILEGES;
