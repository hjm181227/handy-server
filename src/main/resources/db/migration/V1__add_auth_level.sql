-- Add auth_level column if not exists
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_level INT;

-- Set default value for existing records
UPDATE users SET auth_level = 100 WHERE auth_level IS NULL;

-- Make auth_level column not null
ALTER TABLE users MODIFY COLUMN auth_level INT NOT NULL; 