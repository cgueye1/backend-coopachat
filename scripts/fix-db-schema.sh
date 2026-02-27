#!/bin/bash
# =============================================================================
# Corrige le schéma MySQL (addresses.id AUTO_INCREMENT + user_delivery_preferences sans user_id).
# À exécuter sur le SERVEUR (en SSH) ou en local si tu as accès MySQL au serveur.
# =============================================================================
# Usage :
#   1. Sur le serveur (SSH) :
#      mysql -u root -p coopachat < scripts/fix-db-schema.sql
#   2. Ou exécuter les commandes une par une (voir ci-dessous).
# =============================================================================

# Variables à adapter (utilisateur et mot de passe MySQL du serveur)
DB_USER="${DB_USER:-root}"
DB_NAME="${DB_NAME:-coopachat}"

echo "Connexion à MySQL (base: $DB_NAME, user: $DB_USER)..."
mysql -u "$DB_USER" -p "$DB_NAME" << 'SQL'
-- 1. addresses.id en AUTO_INCREMENT (évite "Field 'id' doesn't have a default value")
ALTER TABLE addresses MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

-- 2. Supprimer user_id de user_delivery_preferences si la colonne existe (évite "Field 'user_id' doesn't have a default value")
-- Si la colonne n'existe pas, MySQL renverra une erreur : ignorer ou exécuter seulement la ligne 1 ci-dessus.
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_delivery_preferences' AND COLUMN_NAME = 'user_id');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE user_delivery_preferences DROP COLUMN user_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SQL

echo "Schéma mis à jour."
