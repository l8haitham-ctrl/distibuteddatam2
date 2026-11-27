# Contexte de la branche feature/business-glossary

Base applicative avant ajout des skills : `6c399f35bf25b329f1adf04ada3e59e36e70ca10`. Les commits de cette publication ajoutent seulement les skills et un lien README ; ils ne fusionnent aucune fonctionnalité et ne publient aucun stash.

La référence API complète jointe décrit le snapshot global `eee650a12f1895f44bcc61951babd0423cba95e1`. Cette branche est une étape historique : ne pas supposer que toutes les routes, la sécurité ou les fonctionnalités décrites dans cette référence y existent.

Backend observé : Java / Spring Boot. Modules verticaux observés : catalog, connection, connectors, exploration, glossary, health, jobs, nativesources, quality, sql, storage.

## Routes trouvées dans cette branche

Inventaire des annotations/décorateurs et login/logout Security présents ; il ne remplace pas la lecture des DTO, services, handlers et configuration. Il ne prouve pas qu’un profil conditionnel est activé. Les routes framework (Actuator, Swagger, OpenAPI) ne sont pas incluses dans ce tableau.

| Méthode | Route | Source à cette étape |
|---|---|---|
| DELETE | `/glossary/terms/{id}/links/{linkId}` | [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java) |
| GET | `/catalog/tables` | [CatalogController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/catalog/api/CatalogController.java) |
| GET | `/connectors` | [ConnectorController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/connectors/api/ConnectorController.java) |
| GET | `/glossary/links` | [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java) |
| GET | `/glossary/terms` | [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java) |
| GET | `/glossary/terms/{id}` | [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java) |
| GET | `/health` | [HealthController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/health/HealthController.java) |
| GET | `/native/connectors` | [NativeSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeSourceController.java) |
| GET | `/native/sources` | [NativeConfiguredSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeConfiguredSourceController.java) |
| GET | `/sources` | [SourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/connection/api/SourceController.java) |
| GET | `/sources/{id}/tables` | [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java) |
| GET | `/sources/{id}/tables/{table}` | [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java) |
| GET | `/storage/jobs` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| GET | `/storage/jobs/partition-candidates` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| GET | `/storage/jobs/{id}` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| GET | `/storage/jobs/{id}/result` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| GET | `/storage/status` | [StorageController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/storage/api/StorageController.java) |
| POST | `/connections/explore` | [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java) |
| POST | `/connections/test` | [ConnectionController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/connection/api/ConnectionController.java) |
| POST | `/glossary/terms` | [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java) |
| POST | `/glossary/terms/{id}/links` | [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java) |
| POST | `/native/connections/explore` | [NativeSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeSourceController.java) |
| POST | `/native/connections/test` | [NativeSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeSourceController.java) |
| POST | `/native/sources/{id}/explore` | [NativeConfiguredSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeConfiguredSourceController.java) |
| POST | `/native/sources/{id}/test` | [NativeConfiguredSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeConfiguredSourceController.java) |
| POST | `/quality/reports` | [QualityController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/quality/api/QualityController.java) |
| POST | `/sources/{id}/test` | [SourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/connection/api/SourceController.java) |
| POST | `/sql/query` | [SqlController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/sql/api/SqlController.java) |
| POST | `/storage/jobs` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| PUT | `/glossary/terms/{id}` | [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/6c399f35bf25b329f1adf04ada3e59e36e70ca10/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java) |

## Contrats détaillés

Pour documenter cette branche, lire ses propres DTO/modèles et sorties ; les références détaillées du skill API concernent la version complète et ne sont pas un contrat rétroactif. Si une route diffère de la référence globale, expliquer la différence au lieu de proposer un endpoint absent.
