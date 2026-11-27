# Contexte de la branche backend-modularization

Base applicative avant ajout des skills : `d25179ed33be3fa6344af14024ba017c0a8dc199`. Les commits de cette publication ajoutent seulement les skills et un lien README ; ils ne fusionnent aucune fonctionnalité et ne publient aucun stash.

La référence API complète jointe décrit le snapshot global `eee650a12f1895f44bcc61951babd0423cba95e1`. Cette branche est une étape historique : ne pas supposer que toutes les routes, la sécurité ou les fonctionnalités décrites dans cette référence y existent.

Backend observé : historique Python / FastAPI. Modules verticaux observés : pas de modules Java features/ à cette étape.

## Routes trouvées dans cette branche

Inventaire des annotations/décorateurs et login/logout Security présents ; il ne remplace pas la lecture des DTO, services, handlers et configuration. Il ne prouve pas qu’un profil conditionnel est activé. Les routes framework (Actuator, Swagger, OpenAPI) ne sont pas incluses dans ce tableau.

| Méthode | Route | Source à cette étape |
|---|---|---|
| GET | `/health` | [health.py](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/d25179ed33be3fa6344af14024ba017c0a8dc199/backend/app/api/routes/health.py) |
| GET | `/sources` | [sources.py](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/d25179ed33be3fa6344af14024ba017c0a8dc199/backend/app/api/routes/sources.py) |
| GET | `/sources/{source_id}/tables` | [exploration.py](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/d25179ed33be3fa6344af14024ba017c0a8dc199/backend/app/api/routes/exploration.py) |
| GET | `/sources/{source_id}/tables/{table}` | [exploration.py](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/d25179ed33be3fa6344af14024ba017c0a8dc199/backend/app/api/routes/exploration.py) |
| POST | `/connections/explore` | [connections.py](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/d25179ed33be3fa6344af14024ba017c0a8dc199/backend/app/api/routes/connections.py) |
| POST | `/connections/test` | [connections.py](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/d25179ed33be3fa6344af14024ba017c0a8dc199/backend/app/api/routes/connections.py) |
| POST | `/sources/{source_id}/test` | [sources.py](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/d25179ed33be3fa6344af14024ba017c0a8dc199/backend/app/api/routes/sources.py) |

## Contrats détaillés

Pour documenter cette branche, lire ses propres DTO/modèles et sorties ; les références détaillées du skill API concernent la version complète et ne sont pas un contrat rétroactif. Si une route diffère de la référence globale, expliquer la différence au lieu de proposer un endpoint absent.
