# Organisation des branches Git

## Références à utiliser

- `feature/atlas-platform` : version complète de référence et documentation de présentation.
- `master` : socle historique, conservé sans fusion automatique.

## Corrections et validations récentes

- `feature/extraction-failure-monitoring` : correction du suivi lorsqu'un échec
  Spark coïncide avec une panne du stockage ; base `feature/atlas-platform`
  au commit `31125eb`.
- `feature/extraction-scenario-campaign` : runner, overlay Docker et résultats des
  40 scénarios d'extraction ; base `feature/extraction-failure-monitoring`
  au commit `dd339d6`.

Ces branches restent séparées et ne sont pas fusionnées automatiquement dans
la version de référence ou dans `master`.

Chaque fonctionnalité conserve sa branche sur GitHub. La branche de référence
sert à exécuter et présenter l'ensemble du projet ; elle ne remplace pas les
branches de fonctionnalités. Les 36 branches retirées lors du précédent
nettoyage ont été restaurées à leurs commits exacts. Après cette restauration,
le regroupement de trois branches de documentation et de livraison a été
autorisé : leur contenu figure déjà dans `feature/atlas-platform` et leurs
commits restent accessibles par les tags détaillés ci-dessous. Toutes les
branches techniques sont conservées.

L'optimisation porte sur le classement et la documentation. Aucune branche de
fonctionnalité ne doit être supprimée ou regroupée sans demande explicite.
Les fichiers applicatifs de la branche de référence restent identiques au
commit 9ee4f34 ; les changements d'organisation concernent la documentation.

## Repères par partie du projet

| Partie | Branches principales à consulter |
|---|---|
| Socle backend | `feature/springboot-foundation`, `backend-modularization` |
| Connexion et exploration | `feature/database-connections`, `feature/schema-exploration`, `feature/jdbc-connector-plugins` |
| Connecteurs | `feature/postgresql-connector`, `feature/redis-analytics`, `feature/clickhouse-native-exploration`, `feature/native-source-connections` |
| Extraction et stockage | `feature/storage-jobs`, `feature/minio-storage`, `feature/partitioning-csv`, `feature/redis-hash-extraction`, `feature/clickhouse-extraction` |
| SQL et catalogue | `feature/hive-catalog-trino` |
| Qualité et glossaire | `feature/data-quality-report`, `feature/business-glossary`, `feature/business-glossary-ui` |
| Interface | `feature/wireframe-frontend`, `feature/angular-database-explorer`, `feature/angular-jobs-sql`, `feature/unified-source-explorer` |
| Authentification | `feature/platform-authentication` |
| Benchmarks | `feature/large-dataset`, `feature/reproducible-benchmarks` |
| Déploiement et présentation | `feature/cloud-ready-containers`, `feature/atlas-platform` |

Ce tableau sert à naviguer ; la liste complète des branches conservées figure
ci-dessous. Les branches historiques représentent des étapes successives :
leur présence ne signifie pas qu'elles contiennent toutes la plateforme complète.

## Conservation des travaux en cours

Les branches locales `feature/configurable-spark-execution`,
`feature/timestamp-precision`, `feature/failed-job-retry` et
`feature/iceberg-tables` restent conservées. Leurs modifications non validées
sont sauvegardées séparément dans les stashes existants. Le prototype local
d'architecture live reste local. Aucun stash ni volume Docker n'est supprimé.

## Branches de fonctionnalités conservées et repères d'archive

Chaque branche ci-dessous est disponible à nouveau, au commit indiqué.
Chaque tag pointe vers ce même commit et reste un repère immuable.
La branche `feature/database-connections` a un ancien correctif Compose exclusif :
son commit 2596c9e est conservé sans être fusionné. Les autres références ci-dessous
sont des ancêtres de la version complète. Les archives conservent des références
Git aux commits, sans créer de copies des fichiers du projet.

| Branche conservée | Commit de restauration | Tag d'archive |
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
| `feature/kaggle-soccer-source` | `a855677` | `archive/2026-11-27/kaggle-soccer-source` |
| `feature/large-dataset` | `abef3df` | `archive/2026-11-27/large-dataset` |
| `feature/minio-storage` | `45acddd` | `archive/2026-11-27/minio-storage` |
| `feature/native-source-connections` | `e931c52` | `archive/2026-11-27/native-source-connections` |
| `feature/native-source-interface` | `7f844c4` | `archive/2026-11-27/native-source-interface` |
| `feature/partition-candidates` | `1ed4ce9` | `archive/2026-11-27/partition-candidates` |
| `feature/partitioning-csv` | `1562092` | `archive/2026-11-27/partitioning-csv` |
| `feature/platform-authentication` | `f09ff01` | `archive/2026-11-27/platform-authentication` |
| `feature/postgresql-connector` | `ba54c2b` | `archive/2026-11-27/postgresql-connector` |
| `feature/redis-analytics` | `20b2b18` | `archive/2026-11-27/redis-analytics` |
| `feature/redis-hash-extraction` | `31047fd` | `archive/2026-11-27/redis-hash-extraction` |
| `feature/reproducible-benchmarks` | `a16a09a` | `archive/2026-11-27/reproducible-benchmarks` |
| `feature/schema-exploration` | `7a00192` | `archive/2026-11-27/schema-exploration` |
| `feature/source-test-progress` | `466c3e6` | `archive/2026-11-27/source-test-progress` |
| `feature/springboot-foundation` | `149d406` | `archive/2026-11-27/springboot-foundation` |
| `feature/storage-jobs` | `e265884` | `archive/2026-11-27/storage-jobs` |
| `feature/unified-source-explorer` | `6130492` | `archive/2026-11-27/unified-source-explorer` |
| `feature/unified-source-list` | `c44b03f` | `archive/2026-11-27/unified-source-list` |
| `feature/wireframe-frontend` | `d79f35b` | `archive/2026-11-27/wireframe-frontend` |

## Documentation et livraison regroupées

Ces trois branches ne sont plus maintenues séparément. Leurs commits sont des
ancêtres de `feature/atlas-platform` : aucun changement de code ni fusion
supplémentaire n'est nécessaire pour conserver leur contenu. Leurs anciens
noms de branches sont retirés localement et sur GitHub ; les tags restent.

| Ancienne branche | Contenu conservé dans la version de référence | Tag immuable |
|---|---|---|
| `feature/job-api-documentation` | Documentation Swagger et corrections de validation de l'API jobs | `archive/2026-11-27/job-api-documentation` |
| `feature/release-readiness` | Configuration, scripts, guides et preuves de vérification de livraison | `archive/2026-11-27/release-readiness` |
| `feature/project-presentation` | Support de présentation pour le professeur | `archive/2026-11-27/project-presentation` |

Les anciens liens GitHub vers ces trois noms doivent utiliser la branche de
référence ou leur tag. Les autres branches de fonctionnalités restent disponibles,
y compris `feature/native-source-interface`, conservée comme étape technique.

## Consulter une fonctionnalité ou reprendre son développement

```powershell
git fetch origin --tags
git switch feature/reproducible-benchmarks
git show archive/2026-11-27/reproducible-benchmarks:docs/benchmark-report.md

# Recréer une branche de travail si une ancienne étape doit être reprise :
git switch -c feature/reprendre-benchmark archive/2026-11-27/reproducible-benchmarks
```

Les liens GitHub utilisant les noms des branches restaurées sont de nouveau
utilisables. Pour citer une version exacte dans un rapport, utiliser son tag ou
son commit. Les liens relatifs de la branche de référence restent utilisables.

Aucune fusion de code en cours n'a été effectuée dans master. La publication
de la branche complète ne signifie pas que les bonus non validés sont livrés.
