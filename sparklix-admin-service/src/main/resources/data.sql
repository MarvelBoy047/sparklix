-- Assuming tables are created by ddl-auto=update or create
-- Delete existing to make it idempotent for dev
DELETE FROM showtimes WHERE venue_id IN (SELECT id FROM venues WHERE name = 'Admin Test Venue');
DELETE FROM showtimes WHERE show_id IN (SELECT id FROM shows WHERE title = 'Admin Test Show');
DELETE FROM venues WHERE name = 'Admin Test Venue';
DELETE FROM shows WHERE title = 'Admin Test Show';

INSERT INTO venues (id, name, address, city, capacity) VALUES (101, 'Admin Test Venue', '789 Admin St', 'Adminton', 200);
INSERT INTO shows (id, title, description, genre, language, duration_minutes, release_date, poster_url)
VALUES (201, 'Admin Test Show', 'A show for admin testing.', 'Test Genre', 'TestLang', 90, '2025-06-01', 'http://example.com/adminposter.jpg');