-- Migration : élargir la colonne status de la table orders
-- Erreur corrigée : "Data truncated for column 'status' at row 1"
-- Les valeurs OrderStatus (EN_ATTENTE, EN_PREPARATION, ECHEC_LIVRAISON...) nécessitent au moins 16 caractères.
-- À exécuter une seule fois si la colonne était trop courte.

ALTER TABLE orders MODIFY COLUMN status VARCHAR(32) NOT NULL;
