-- Mise à jour de la colonne 'role' pour inclure les nouveaux rôles : SUPPLIER et COMPANY
ALTER TABLE users MODIFY COLUMN role ENUM(
    'ADMINISTRATOR',
    'COMMERCIAL',
    'DELIVERY_DRIVER',
    'EMPLOYEE',
    'LOGISTICS_MANAGER',
    'SUPPLIER',
    'COMPANY'
) NOT NULL;

-- 2. Ajout du champ de suspension (Boolean en Java = TINYINT(1) en MySQL)
ALTER TABLE users ADD COLUMN disabled_by_admin TINYINT(1) NOT NULL DEFAULT 0;
