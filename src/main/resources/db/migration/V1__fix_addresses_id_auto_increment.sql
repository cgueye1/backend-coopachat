-- Corrige la colonne id de la table addresses pour que MySQL génère les id (AUTO_INCREMENT).
-- Exécuté automatiquement au démarrage de l'app (Flyway). Plus besoin de faire l'ALTER à la main sur le serveur.
ALTER TABLE addresses MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
