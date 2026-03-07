# Configuration images – 2 serveurs (API + fichiers)

## Architecture cible

| Serveur | Rôle | Port | MinIO |
|---------|------|------|-------|
| **API** | Logique métier, uploads, base de données | 8083 | DOIT pointer vers le même bucket |
| **Fichiers** | Servir les images uniquement (FileController) | 8085 | MÊME config MinIO |

Les deux serveurs doivent utiliser **exactement la même configuration MinIO** (même URL, bucket, credentials).

---

## Configuration actuelle

### application.properties (local)
- MinIO : `https://minio.innovimpactafrica.cloud`
- Bucket : `coop-achat`

### application-docker.properties (Docker)
- Variables : `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`
- Valeurs prises depuis `.env` → docker-compose

### .env
```env
MINIO_URL=https://minio.innovimpactafrica.cloud
MINIO_ACCESS_KEY=rHIPBAzBQUtBA08aptVz
MINIO_SECRET_KEY=wh6bFwyW0aCBlYlJu7EuV5U9l8fWPUKueTYo30eA
MINIO_BUCKET=coop-achat
```

---

## Base de données – colonne `product.image`

- **Stockage** : chemin relatif (sans domaine)
- **Exemples** :
  - `115b4ea0-ba5e-4ecc-b206-595add6f006a.jpeg` → MinIO cherche `products/115b4ea0...` (fallback)
  - `products/b36fc32d-3e45-4d82-86a2-1ff554ce1957.jpg` → chemin complet

### Comportement backend
- `MinioServiceImpl.getFile()` :
  1. Cherche l’objet tel qu’en BDD
  2. Si non trouvé et pas de `/`, réessaie avec `products/` + nom

### Construction de l’URL côté frontend
- Base : `imageServerUrl` (ex. `http://86.106.181.31:8083/api`)
- URL finale : `{imageServerUrl}/files/{image}`
- Exemple : `http://86.106.181.31:8083/api/files/115b4ea0-ba5e-4ecc-b206-595add6f006a.jpeg`

---

## Pourquoi des 404 ?

1. **Fichiers absents dans MinIO**  
   Les chemins en BDD peuvent être corrects, mais les fichiers n’ont jamais été uploadés ou le bucket a été vidé.

2. **Config MinIO différente sur le serveur**  
   Sur le VPS, les variables d’environnement (ou équivalent `.env`) peuvent pointer vers un autre MinIO ou un autre bucket.

3. **Serveur fichiers (8085)**  
   S’il existe, il doit avoir exactement la même config MinIO que l’API.

---

## Checklist déploiement

- [ ] API (8083) : `.env` ou variables d’environnement avec `MINIO_*`
- [ ] Serveur fichiers (8085) : même `.env` / même config MinIO
- [ ] Les fichiers produits sont bien présents dans le bucket `coop-achat` (MinIO)
- [ ] Frontend : `imageServerUrl` pointe vers le serveur qui sert les fichiers (8083 ou 8085)
