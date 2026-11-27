# Contexte de la branche feature/springboot-foundation

Base applicative avant ajout des skills : `149d4060cae90db76978d3131a9d727627df4efe`. Les commits de cette publication ajoutent seulement les skills et un lien README ; ils ne fusionnent aucune fonctionnalité et ne publient aucun stash.

La référence API complète jointe décrit le snapshot global `eee650a12f1895f44bcc61951babd0423cba95e1`. Cette branche est une étape historique : ne pas supposer que toutes les routes, la sécurité ou les fonctionnalités décrites dans cette référence y existent.

Backend observé : Java / Spring Boot. Modules verticaux observés : health.

## Routes trouvées dans cette branche

Inventaire des annotations/décorateurs et login/logout Security présents ; il ne remplace pas la lecture des DTO, services, handlers et configuration. Il ne prouve pas qu’un profil conditionnel est activé. Les routes framework (Actuator, Swagger, OpenAPI) ne sont pas incluses dans ce tableau.

| Méthode | Route | Source à cette étape |
|---|---|---|
| GET | `/health` | [HealthController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/149d4060cae90db76978d3131a9d727627df4efe/backend/src/main/java/com/atlas/explorer/features/health/HealthController.java) |

## Contrats détaillés

Pour documenter cette branche, lire ses propres DTO/modèles et sorties ; les références détaillées du skill API concernent la version complète et ne sont pas un contrat rétroactif. Si une route diffère de la référence globale, expliquer la différence au lieu de proposer un endpoint absent.
