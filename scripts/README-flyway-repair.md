# Corriger l'historique Flyway (migration V2 en échec)

## Problème
L'app ne démarre pas : `Detected failed migration to version 2 (fix user delivery preferences drop user id)`.

## Étapes

### 1. Exécuter le script SQL sur la base du serveur
Sur le serveur (ou en connexion à la base MySQL utilisée par l'app) :

```bash
mysql -u root -p coopachat < scripts/fix-flyway-history-v2.sql
```

Ou dans phpMyAdmin : onglet SQL, coller le contenu de `fix-flyway-history-v2.sql`, exécuter.

Cela supprime l'entrée "échouée" de la migration V2 dans `flyway_schema_history`.

### 2. Redéployer et redémarrer l'app
- Rebuild : `mvn clean package`
- Déployer le nouveau JAR (la migration V2 a été rendue conditionnelle : elle ne plante plus si la colonne `user_id` n'existe pas).
- Redémarrer le conteneur : `docker restart coopachat-app` (ou équivalent).

Au démarrage, Flyway réessaiera la migration V2 ; elle réussira (soit elle supprime la colonne, soit elle ne fait rien si la colonne est absente).
