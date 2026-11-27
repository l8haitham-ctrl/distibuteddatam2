# Contexte de la branche feature/large-dataset

Base applicative avant ajout des skills : `abef3dff9b93dca893791b7e4a32b4ce8f695d02`. Les commits de cette publication ajoutent seulement les skills et un lien README ; ils ne fusionnent aucune fonctionnalité et ne publient aucun stash.

La référence API complète jointe décrit le snapshot global `eee650a12f1895f44bcc61951babd0423cba95e1`. Cette branche est une étape historique : ne pas supposer que toutes les routes, la sécurité ou les fonctionnalités décrites dans cette référence y existent.

Backend observé : Java / Spring Boot. Modules verticaux observés : connection, exploration, health, jobs, storage.

## Routes trouvées dans cette branche

Inventaire des annotations/décorateurs et login/logout Security présents ; il ne remplace pas la lecture des DTO, services, handlers et configuration. Il ne prouve pas qu’un profil conditionnel est activé. Les routes framework (Actuator, Swagger, OpenAPI) ne sont pas incluses dans ce tableau.

| Méthode | Route | Source à cette étape |
|---|---|---|
| GET | `/health` | [HealthController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/health/HealthController.java) |
| GET | `/sources` | [SourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/connection/api/SourceController.java) |
| GET | `/sources/{id}/tables` | [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java) |
| GET | `/sources/{id}/tables/{table}` | [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java) |
| GET | `/storage/jobs` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| GET | `/storage/jobs/{id}` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| GET | `/storage/jobs/{id}/result` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |
| GET | `/storage/status` | [StorageController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/storage/api/StorageController.java) |
| POST | `/connections/explore` | [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java) |
| POST | `/connections/test` | [ConnectionController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/connection/api/ConnectionController.java) |
| POST | `/sources/{id}/test` | [SourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/connection/api/SourceController.java) |
| POST | `/storage/jobs` | [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/abef3dff9b93dca893791b7e4a32b4ce8f695d02/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java) |

## Contrats détaillés

Pour documenter cette branche, lire ses propres DTO/modèles et sorties ; les références détaillées du skill API concernent la version complète et ne sont pas un contrat rétroactif. Si une route diffère de la référence globale, expliquer la différence au lieu de proposer un endpoint absent.
