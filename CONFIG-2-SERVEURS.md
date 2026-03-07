# Configuration 2 serveurs – API + Fichiers

## Architecture

| Serveur | Rôle | URL | Port |
|---------|------|-----|------|
| **Serveur 1** | API (code métier, uploads) | 86.106.181.31 | 8083 |
| **Serveur 2** | Fichiers (images uniquement) | 88.100.101.51 | 8085 |

## Configuration requise

### Les deux serveurs doivent utiliser le MÊME MinIO

- **MINIO_URL** : https://minio.innovimpactafrica.cloud
- **MINIO_ACCESS_KEY** : (identique sur les 2)
- **MINIO_SECRET_KEY** : (identique sur les 2)
- **MINIO_BUCKET** : coop-achat

Le fichier `.env` sur le serveur fichiers (8085) doit contenir exactement les mêmes valeurs MinIO que sur le serveur API (8083).

### Frontend (environment.ts / environment.prod.ts)

- **apiUrl** : `http://86.106.181.31:8083/api` (appels API)
- **imageServerUrl** : 
  - Si serveur fichiers actif : `http://88.100.101.51:8085/api`
  - Si un seul serveur : `http://86.106.181.31:8083/api`

## 404 sur les images

Un **404** signifie que le fichier **n'existe pas dans MinIO** (le bucket coop-achat).

Causes possibles :
1. Images jamais uploadées (BDD avec des chemins mais fichiers absents dans MinIO)
2. Bucket MinIO vide ou réinitialisé
3. Chemins en BDD incorrects (ex. `uuid.jpg` au lieu de `products/uuid.jpg`)

Le backend construit correctement les URLs et interroge MinIO. Si MinIO ne trouve pas l’objet, il renvoie 404.

## Vérifications rapides

1. **.env** : MINIO_URL, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET renseignés
2. **Serveur fichiers (8085)** : application démarrée, même .env que l’API
3. **MinIO** : bucket `coop-achat` existant, objets présents (produits, profiles, etc.)
