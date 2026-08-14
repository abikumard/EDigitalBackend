-- =========================================================
-- Run this ONCE against your EXISTING contenthub_db database
-- to move from email+OTP login to email/mobile+password login.
--
--   mysql -u root -p contenthub_db < migration_password_auth.sql
--
-- Safe to run even if you have existing rows in `users` — the
-- new columns are added as nullable, so nothing existing breaks.
-- Any old accounts (created via the old email+OTP flow) will have
-- password_hash = NULL and simply won't be able to log in until
-- they sign up again with a password, or you delete/reset them.
-- =========================================================

ALTER TABLE users MODIFY COLUMN email VARCHAR(150) NULL;
ALTER TABLE users ADD COLUMN mobile VARCHAR(15) NULL UNIQUE AFTER email;
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL AFTER mobile;

-- Optional: if this is still a test/dev database and you don't need to
-- keep the old email-only test accounts, clearing them out is simpler
-- than migrating them (they had no password to begin with):
--
-- DELETE FROM purchases WHERE user_id IN (SELECT id FROM users WHERE password_hash IS NULL);
-- DELETE FROM users WHERE password_hash IS NULL;
