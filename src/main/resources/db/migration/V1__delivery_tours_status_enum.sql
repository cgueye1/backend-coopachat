-- Ajoute les valeurs ASSIGNEE et ANNULEE à l'enum status de delivery_tours
-- (alignement avec l'enum Java DeliveryTourStatus)
-- À exécuter une fois sur la base MySQL.

ALTER TABLE `delivery_tours`
MODIFY COLUMN `status` ENUM(
  'CONFIRMEE', 'EN_COURS', 'PLANIFIEE', 'PROPOSEE', 'REFUSEE', 'TERMINEE',
  'ASSIGNEE', 'ANNULEE'
) NOT NULL;
