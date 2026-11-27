---
name: atlas-api
description: "Documenter et expliquer tous les contrats API Atlas avec paramètres, corps de requête, validations, réponses complètes, erreurs, authentification et exemples ; vérifier leur correspondance au code."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Contrats API Atlas

Quand l’utilisateur dit « donne-moi les API », fournir toutes les entrées et toutes les sorties, pas seulement une liste de routes ou un lien Swagger. Pour un endpoint ciblé, fournir son contrat complet ; pour toutes les API, couvrir l’inventaire entier, en plusieurs sections si nécessaire, sans omettre silencieusement une famille.

Lire [la référence complète](references/contrats-api.md). Elle contient les schémas partagés et chaque méthode/route avec entrées, sorties, conditions, erreurs et exemples. [L’inventaire JSON](references/inventaire-api.json) permet de contrôler la couverture. C’est une observation du code au commit eee650a, pas une spécification générée depuis un serveur en marche.

Pour chaque route exposée : indiquer méthode, chemin, rôle, prérequis/features activées ; authentification/cookie/CSRF ; path/query/headers/body avec types, obligatoire, défaut, enum/limites ; statuts succès/erreur, headers utiles, schéma de sortie complet et exemple illustratif. Développer les schémas référencés dans la réponse utilisateur plutôt que masquer les champs derrière le nom d’un DTO.

Revalider controllers, DTO, services/connectors qui construisent les Map, handlers et AuthConfiguration. Inclure login/logout gérés par Security, SSE et endpoints de santé/doc framework. Ne pas supposer qu’un Map a un schéma uniforme entre SQL Server, PostgreSQL, Redis, MinIO et ClickHouse. Une sortie dynamique doit être décrite avec ses variantes et types ; les exemples ne sont pas des réponses capturées.

Ne pas inventer un POST /sources, DELETE de terme, POST /retry, route Iceberg, pagination des jobs ou moteur universel s’ils ne sont pas présents. Distinguer 401 session API et 400 authentification base source ; HTTP 200 et résultat métier failed ; 202 accepté et COMPLETED. Ne pas promettre que tous les 4xx sont normalisés si aucun handler ne le garantit.

Présenter les requêtes avec placeholders de credentials, jamais avec .env réel. Expliquer le handshake GET /auth/session → POST formulaire /auth/login + CSRF → GET /auth/session après login. Les appels mutateurs et même POST SQL lecture seule ont besoin du token CSRF.

Mise à jour : répertorier les méthodes HTTP du code, comparer à l’inventaire JSON et corriger la référence lorsqu’une route/DTO/sortie change. Les tests Docker n’ont pas été lancés pour cette création documentaire. Ne pas transformer la documentation en autorisation d’appeler les routes mutatrices.
