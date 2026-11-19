# Integration de la version existante

Verification du 19 novembre 2026, avant les nouvelles fonctionnalites.

- Avance rapide de master de `2284efe` a `0ec9b3c`, sans reecriture d'historique.
- Reconstruction et demarrage par `docker compose up -d --build` : reussis.
- `docker compose --profile tools run --rm backend-tests` : 41 tests reussis,
  aucun echec, aucune erreur, aucun test ignore.
- Tests Angular pendant la reconstruction : 7 tests reussis.
- Le prototype local d'architecture est sauvegarde dans un stash local ; il
  n'est pas inclus dans cette integration et n'est pas publie.

Branches distantes entierement contenues dans master apres integration :

```text
origin/backend-modularization
origin/codex/backend-modularization
origin/feature/angular-database-explorer
origin/feature/cloud-ready-containers
origin/feature/job-api-documentation
origin/feature/minio-storage
origin/feature/schema-exploration
origin/feature/springboot-foundation
origin/feature/storage-jobs
origin/master
```

Aucune branche n'est supprimee. `origin/feature/database-connections` n'est pas
entierement contenue dans master en raison du commit `2596c9e`. Son changement
est identique a celui de `7a00192`, deja integre : les deux donnent le meme
identifiant `git patch-id --stable`,
`30849263a63ba6ae0b2e091242b3cd0897e5ebc2`. Ce commit duplique la suppression
de la dependance du backend au frontend dans Compose et n'ajoute aucun changement.

Suite demandee : volume de donnees configurable, CSV et partitionnement,
catalogue Hive et Trino, interface jobs/SQL, puis benchmarks reels.
Le bonus Iceberg/incremental est exclu.
