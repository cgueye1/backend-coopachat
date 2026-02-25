# Importer les données en local vers le serveur (Docker MySQL)

Sur le serveur, MySQL dans Docker démarre **vide**. Les tables sont créées par Hibernate, mais il n’y a pas de données (utilisateurs, produits, catégories, etc.). Ce document décrit comment copier les données de ta base locale vers le serveur.

---

## 1. Exporter la base depuis ta machine locale

Sur ton PC (où tourne MySQL avec les données) :

```bash
mysqldump -u TON_USER -p NOM_DE_TA_BASE > backup_coopachat.sql
```

Exemple si ta base s’appelle `coopachat` et l’utilisateur `root` :

```bash
mysqldump -u root -p coopachat > backup_coopachat.sql
```

Tu obtiens un fichier `backup_coopachat.sql` à copier sur le serveur (scp, SFTP, etc.).

---

## 2. Sur le serveur : importer dans le MySQL Docker

Une fois `backup_coopachat.sql` sur le serveur, avec Docker Compose déjà lancé (`docker compose up -d` ou équivalent) :

```bash
# Importer le backup dans le conteneur MySQL
docker exec -i coopachat-db mysql -u coopachat -pcoopachat coopachat < backup_coopachat.sql
```

Si tu as défini d’autres identifiants dans ton `.env` (DB_USER, DB_PASSWORD, DB_NAME), adapte la commande :

```bash
docker exec -i coopachat-db mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME < backup_coopachat.sql
```

Après l’import, l’application (conteneur `coopachat-app`) voit les mêmes données que en local. Pas besoin de rebuild l’image : les données sont dans le volume `db_data`.

---

## Résumé

| Où | Situation |
|----|-----------|
| **Local** | MySQL avec tes données (export → `backup_coopachat.sql`) |
| **Serveur** | MySQL Docker vide au premier déploiement → importer `backup_coopachat.sql` pour avoir les données |

Ensuite, ta copine pourra appeler l’API sur le serveur et voir les mêmes données que toi en local.
