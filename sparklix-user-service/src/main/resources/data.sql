-- Corrected data.sql for sparklix-user-service

-- STEP 1: Delete from the table that has foreign keys pointing to users and roles
DELETE FROM user_roles; -- This will delete ALL records from user_roles

-- STEP 2: Now you can safely delete from users and roles
DELETE FROM users;    -- This will delete ALL users
DELETE FROM roles;    -- This will delete ALL roles

-- STEP 3: Re-insert roles with specific IDs
INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_USER');

-- STEP 4: Re-insert admin user
-- Make sure this hash is correct for "adminpass"
INSERT INTO users (id, name, username, email, password, enabled, created_at, updated_at)
VALUES (100, 'Admin User From DataSQL', 'adminuser', 'admin@datasql.com', '$2a$10$m0bhiCM8E3CKujc8zxaWsukF3lRZl3ifZYuU7vwy2zharr57g1Wli', true, NOW(), NOW());
INSERT INTO user_roles (user_id, role_id) VALUES (100, 1); -- Assign ROLE_ADMIN (ID 1)

-- STEP 5: Re-insert catalog-sync-agent service account user
-- Make sure this hash is correct for "S3rv1c3AccP@sswOrd" or your chosen password
INSERT INTO users (id, name, username, email, password, enabled, created_at, updated_at)
VALUES (200, 'Show Catalog Sync Agent', 'catalog-sync-agent', 'catalog-sync@system.sparklix', '$2a$10$8gPDpNnqq0vZ3y.Vqh7bBeENB1o1ERN7T75NvbfS62HGLx5QDr3Bi', true, NOW(), NOW());
INSERT INTO user_roles (user_id, role_id) VALUES (200, 1); -- Assign ROLE_ADMIN (ID 1)

-- Note: pythontestuser will be created by the Python script if it doesn't exist