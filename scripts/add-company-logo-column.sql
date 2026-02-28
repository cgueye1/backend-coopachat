-- Migration : ajout de la colonne logo à la table companies
-- À exécuter si la table a été créée avant l'ajout du champ logo dans l'entité Company.
-- Si la colonne existe déjà (ex. après un CREATE par JPA), cette requête échouera : ignorer l'erreur.

ALTER TABLE companies
  ADD COLUMN logo VARCHAR(255) DEFAULT NULL
  AFTER note;
