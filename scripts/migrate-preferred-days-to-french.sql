-- Migration : jours de préférence de livraison (anglais → français)
-- Table : preferred_delivery_days, colonne : day_of_week
-- À exécuter une seule fois pour convertir les anciennes valeurs (MONDAY, FRIDAY...) en français (LUNDI, VENDREDI...).

UPDATE preferred_delivery_days SET day_of_week = 'LUNDI'   WHERE day_of_week = 'MONDAY';
UPDATE preferred_delivery_days SET day_of_week = 'MARDI'   WHERE day_of_week = 'TUESDAY';
UPDATE preferred_delivery_days SET day_of_week = 'MERCREDI' WHERE day_of_week = 'WEDNESDAY';
UPDATE preferred_delivery_days SET day_of_week = 'JEUDI'   WHERE day_of_week = 'THURSDAY';
UPDATE preferred_delivery_days SET day_of_week = 'VENDREDI' WHERE day_of_week = 'FRIDAY';
UPDATE preferred_delivery_days SET day_of_week = 'SAMEDI'   WHERE day_of_week = 'SATURDAY';
UPDATE preferred_delivery_days SET day_of_week = 'DIMANCHE' WHERE day_of_week = 'SUNDAY';

-- Vérification (optionnel) : lister les valeurs restantes
-- SELECT DISTINCT day_of_week FROM preferred_delivery_days ORDER BY day_of_week;
