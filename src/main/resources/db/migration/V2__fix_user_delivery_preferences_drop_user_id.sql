-- Supprime la colonne user_id de user_delivery_preferences si elle existe (legacy).
-- Si la colonne n'existe pas, ne fait rien pour éviter l'échec Flyway.
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_delivery_preferences'
      AND COLUMN_NAME = 'user_id'
);
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE user_delivery_preferences DROP COLUMN user_id',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
