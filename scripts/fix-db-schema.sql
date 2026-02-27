-- Corrige le schéma pour éviter les erreurs au démarrage et à l'INSERT.
-- Exécuter : mysql -u root -p coopachat < scripts/fix-db-schema.sql

USE coopachat;

-- 1. addresses.id en AUTO_INCREMENT
ALTER TABLE addresses MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

-- 2. Supprimer user_id si elle existe (sinon MySQL peut afficher une erreur : à ignorer si la colonne n'existe pas)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_delivery_preferences' AND COLUMN_NAME = 'user_id');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE user_delivery_preferences DROP COLUMN user_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
