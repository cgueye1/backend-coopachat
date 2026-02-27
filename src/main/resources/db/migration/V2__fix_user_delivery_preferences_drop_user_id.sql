-- La table user_delivery_preferences avait une colonne user_id (legacy) alors que l'entité n'utilise que employee_id.
-- Sans cette colonne, l'INSERT échoue avec "Field 'user_id' doesn't have a default value".
-- On supprime user_id pour aligner la table sur l'entité EmployeeDeliveryPreference.
ALTER TABLE user_delivery_preferences DROP COLUMN user_id;
