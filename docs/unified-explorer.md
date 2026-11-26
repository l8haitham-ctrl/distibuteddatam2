# Explorateur commun

Branche `feature/unified-source-explorer`, base `feature/redis-hash-extraction`.

Depuis **Data Sources**, le bouton **Explorer** ouvre maintenant **Source Explorer**
pour tous les moteurs. Le cadre reprend celui de SQL Server : bandeau source,
arborescence avec recherche a gauche, selection et panneau de details a droite,
onglets et action d'extraction pour les sources compatibles.

| Source | Arborescence | Details |
|---|---|---|
| SQL Server / PostgreSQL | Schemas et tables | Colonnes, PK/FK, relations et glossaire existants |
| ClickHouse | Base et tables | Types exacts, indicateurs de tri et de partition, moteur de table |
| Redis | Index et cles echantillonnees | Type de cle et noms de champs des hashes ; types non declares par Redis |
| MinIO | Bucket et objets | Nom, type et taille du fichier |

Les moteurs conservent leurs semantiques : pas de fausses cles etrangeres SQL
pour Redis, ClickHouse ou MinIO. Les limites d'echantillonnage sont visibles.
Le filtre de recherche s'applique a la liste chargee, pas a toute la source.
L'extraction Redis porte sur le dataset mappe et le prefixe entier, jamais
uniquement sur la cle selectionnee. L'extraction ClickHouse concerne la table
selectionnee. Les deux gardent Parquet Snappy sans partitionnement des fichiers.
MinIO propose la consultation, sans nouvelle action d'extraction.

## Navigation

Une seule route `/explorer` : `source_id=sqlserver` pour le relationnel,
`source_id=native:redis-demo` (ou autre ID natif) pour les profils natifs.
Ce prefixe evite les collisions d'identifiants et permet de recharger les liens.
Aucune source n'est connectee au seul chargement de Data Sources.

Le bouton et la page d'ajout Redis/ClickHouse/MinIO sont retires. Le formulaire
relationnel **Ajouter une source** reste disponible. Les profils natifs affiches
restent configures cote serveur ; l'ajout natif temporaire n'est plus expose
dans l'interface. Les API natives existantes restent disponibles.

Les anciens liens `/sources/native?source_id=redis-demo` redirigent vers le
nouvel explorateur du meme profil. `/sources/native` sans profil revient a
`/sources`. Actualiser relit uniquement le profil selectionne. Changer de source
annule la lecture precedente. Les soumissions d'extraction incertaines restent
bloquees pour eviter les jobs dupliques.

## Validation Docker

Utiliser les six fichiers Compose du [guide Redis/ClickHouse](redis-clickhouse-exploration.md),
puis construire et mettre a jour uniquement le frontend :

```powershell
docker compose @compose build frontend
docker compose @compose up -d --no-deps --wait frontend
```

Le Dockerfile lance les tests Angular puis le build de production. Les tests
couvrent les details propres aux trois moteurs, selection/recherche, les
requêtes d'extraction existantes, erreurs et annulation, ainsi que l'acces
commun depuis la liste. Aucun changement backend, de volume ou de donnees.

Controle local du 26 novembre 2026 : **67 tests Angular passes dans 14 fichiers**,
build de production reussi et frontend Docker sain. Le navigateur confirme
l'ouverture directe des trois moteurs, la selection des tables ClickHouse,
la recherche Redis et son actualisation, les proprietes MinIO et la conservation
de la structure Invoice SQL Server (6 colonnes, PK/FK et glossaire).
Les deux redirections de l'ancien ecran sont verifiees. Les extractions ne sont
pas relancees pour cette modification d'interface ; leurs corps de requete
et la protection contre les doubles soumissions sont couverts par les tests.

Captures : [liste commune](evidence/unified-explorer-sources.jpg),
[ClickHouse](evidence/unified-explorer-clickhouse.jpg),
[Redis](evidence/unified-explorer-redis.jpg), [MinIO](evidence/unified-explorer-minio.jpg).
