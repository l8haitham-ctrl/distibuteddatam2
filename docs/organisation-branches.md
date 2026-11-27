# Organisation des branches Git

## Références à utiliser

- `feature/atlas-platform` : version complète de référence et documentation de présentation.
- `master` : socle historique, conservé sans fusion automatique.

Cette organisation remplace les anciennes branches de progression par des tags
d'archive. Les fichiers applicatifs de la branche complète restent identiques
au commit 9ee4f34 ; la consolidation modifie seulement la documentation.

## Conservation des travaux en cours

Les branches locales `feature/configurable-spark-execution`,
`feature/timestamp-precision`, `feature/failed-job-retry` et
`feature/iceberg-tables` restent conservées. Leurs modifications non validées
sont sauvegardées séparément dans les stashes existants. Le prototype local
d'architecture live reste local. Aucun stash ni volume Docker n'est supprimé.

## Archives des anciennes branches

Chaque tag pointe vers le commit exact de la branche avant son retrait.
La branche `feature/database-connections` a un ancien correctif Compose exclusif :
son commit 2596c9e est archivé sans être fusionné. Les autres références ci-dessous
sont des ancêtres de la version complète. Les archives conservent des références
Git aux commits, sans créer de copies des fichiers du projet.

| Ancienne branche | Commit conservé | Tag d'archive |
|---|---|---|
| `backend-modularization` | `d25179e` | `archive/2026-11-27/backend-modularization` |
| `feature/angular-database-explorer` | `43bee49` | `archive/2026-11-27/angular-database-explorer` |
| `feature/angular-jobs-sql` | `c42e3e6` | `archive/2026-11-27/angular-jobs-sql` |
| `feature/business-glossary` | `6c399f3` | `archive/2026-11-27/business-glossary` |
| `feature/business-glossary-ui` | `69bd133` | `archive/2026-11-27/business-glossary-ui` |
| `feature/clickhouse-extraction` | `6dbac4f` | `archive/2026-11-27/clickhouse-extraction` |
| `feature/clickhouse-native-exploration` | `0db84a6` | `archive/2026-11-27/clickhouse-native-exploration` |
| `feature/cloud-ready-containers` | `858cfd0` | `archive/2026-11-27/cloud-ready-containers` |
| `feature/connector-aware-frontend` | `443ed22` | `archive/2026-11-27/connector-aware-frontend` |
| `feature/data-quality-report` | `9e58927` | `archive/2026-11-27/data-quality-report` |
| `feature/database-connections` | `2596c9e` | `archive/2026-11-27/database-connections` |
| `feature/explicit-source-selection` | `13cad98` | `archive/2026-11-27/explicit-source-selection` |
| `feature/hive-catalog-trino` | `783e2b2` | `archive/2026-11-27/hive-catalog-trino` |
| `feature/jdbc-connector-plugins` | `4b99327` | `archive/2026-11-27/jdbc-connector-plugins` |
| `feature/job-api-documentation` | `0ec9b3c` | `archive/2026-11-27/job-api-documentation` |
| `feature/kaggle-soccer-source` | `a855677` | `archive/2026-11-27/kaggle-soccer-source` |
| `feature/large-dataset` | `abef3df` | `archive/2026-11-27/large-dataset` |
| `feature/minio-storage` | `45acddd` | `archive/2026-11-27/minio-storage` |
| `feature/native-source-connections` | `e931c52` | `archive/2026-11-27/native-source-connections` |
| `feature/native-source-interface` | `7f844c4` | `archive/2026-11-27/native-source-interface` |
| `feature/partition-candidates` | `1ed4ce9` | `archive/2026-11-27/partition-candidates` |
| `feature/partitioning-csv` | `1562092` | `archive/2026-11-27/partitioning-csv` |
| `feature/platform-authentication` | `f09ff01` | `archive/2026-11-27/platform-authentication` |
| `feature/postgresql-connector` | `ba54c2b` | `archive/2026-11-27/postgresql-connector` |
| `feature/project-presentation` | `9ee4f34` | `archive/2026-11-27/project-presentation` |
| `feature/redis-analytics` | `20b2b18` | `archive/2026-11-27/redis-analytics` |
| `feature/redis-hash-extraction` | `31047fd` | `archive/2026-11-27/redis-hash-extraction` |
| `feature/release-readiness` | `a3e4b39` | `archive/2026-11-27/release-readiness` |
| `feature/reproducible-benchmarks` | `a16a09a` | `archive/2026-11-27/reproducible-benchmarks` |
| `feature/schema-exploration` | `7a00192` | `archive/2026-11-27/schema-exploration` |
| `feature/source-test-progress` | `466c3e6` | `archive/2026-11-27/source-test-progress` |
| `feature/springboot-foundation` | `149d406` | `archive/2026-11-27/springboot-foundation` |
| `feature/storage-jobs` | `e265884` | `archive/2026-11-27/storage-jobs` |
| `feature/unified-source-explorer` | `6130492` | `archive/2026-11-27/unified-source-explorer` |
| `feature/unified-source-list` | `c44b03f` | `archive/2026-11-27/unified-source-list` |
| `feature/wireframe-frontend` | `d79f35b` | `archive/2026-11-27/wireframe-frontend` |

## Consulter ou restaurer une étape

```powershell
git fetch origin --tags
git show archive/2026-11-27/reproducible-benchmarks:docs/benchmark-report.md

# Recréer une branche de travail si une ancienne étape doit être reprise :
git switch -c feature/reprendre-benchmark archive/2026-11-27/reproducible-benchmarks
```

Les anciens liens GitHub utilisant les noms de branches supprimées peuvent
devenir invalides. Utiliser le tag correspondant ou un commit immuable. Les liens
relatifs de la documentation de la branche de référence restent utilisables.

Aucune fusion de code en cours n'a été effectuée dans master. La publication
de la branche complète ne signifie pas que les bonus non validés sont livrés.
