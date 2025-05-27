-- Clear potential old conflicting data
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username IN ('adminuser', 'catalog-sync-agent', 'pythontestuser'));
DELETE FROM users WHERE username IN ('adminuser', 'catalog-sync-agent', 'pythontestuser');
DELETE FROM roles WHERE id IN (1, 2) OR name IN ('ROLE_ADMIN', 'ROLE_USER');

-- Insert roles with specific IDs
INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_USER');

-- Create admin user (using your previously generated hash for 'adminpass')
INSERT INTO users (id, name, username, email, password, enabled, created_at, updated_at)
VALUES (100, 'Admin User From DataSQL', 'adminuser', 'admin@datasql.com', '$2a$10$m0bhiCM8E3CKujc8zxaWsukF3lRZl3ifZYuU7vwy2zharr57g1Wli', true, NOW(), NOW());
INSERT INTO user_roles (user_id, role_id) VALUES (100, 1);

-- Create catalog-sync-agent service account user
INSERT INTO users (id, name, username, email, password, enabled, created_at, updated_at)
VALUES (200, 'Show Catalog Sync Agent', 'catalog-sync-agent', 'catalog-sync@system.sparklix', '$2a$10$8gPDpNnqq0vZ3y.Vqh7bBeENB1o1ERN7T75NvbfS62HGLx5QDr3Bi', true, NOW(), NOW());
INSERT INTO user_roles (user_id, role_id) VALUES (200, 1); -- Assign ROLE_ADMIN
