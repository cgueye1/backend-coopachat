-- =============================================================================
-- À exécuter UNE FOIS sur la base MySQL du serveur (celle utilisée par l'app).
-- Corrige l'historique Flyway après une migration V2 en échec.
-- =============================================================================
-- Connexion exemple : mysql -u root -p coopachat < fix-flyway-history-v2.sql
-- Ou dans phpMyAdmin : onglet SQL, coller le contenu ci-dessous, Exécuter.
-- =============================================================================

USE coopachat;

-- Supprimer l'entrée de la migration V2 en échec pour que Flyway puisse redémarrer.
-- Au prochain démarrage, Flyway ne verra plus de "failed migration" et l'app démarrera.
DELETE FROM flyway_schema_history
WHERE version = '2'
  AND success = 0;

-- Vérification (optionnel) : afficher les migrations restantes
-- SELECT * FROM flyway_schema_history ORDER BY installed_rank;
