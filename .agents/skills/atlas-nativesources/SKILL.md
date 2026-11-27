---
name: atlas-nativesources
description: "Maintenir, expliquer ou vérifier les profils et métadonnées Redis, ClickHouse et MinIO dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Exploration des sources natives

## Portée et lecture ciblée

Appliquer ce skill pour les profils et métadonnées Redis, ClickHouse et MinIO. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [NativeSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeSourceController.java)
- [NativeConfiguredSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeConfiguredSourceController.java)
- [NativeSourceProfiles.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/service/NativeSourceProfiles.java)
- [RedisNativeConnector.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/connector/RedisNativeConnector.java)
- [ClickhouseNativeConnector.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/connector/ClickhouseNativeConnector.java)
- [MinioNativeConnector.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/connector/MinioNativeConnector.java)

## Invariants propres à cette feature

Contrats /native/* distincts des routes relationnelles, même présentation Angular. Redis expose noms/types/champs, pas valeurs ; MinIO liste des objets, pas des tables SQL automatiques ; ClickHouse expose tables/colonnes/clés de tri, ces clés ne garantissent pas unicité. Exploration bornée à 100 objets ; ClickHouse 200 colonnes/table. Les capacités annoncées par /native/connectors ne décrivent pas toutes les capacités du lecteur natif d’extraction.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Troncature visible, profil inconnu, TLS, échappement préfixe Redis, requêtes ClickHouse paramétrées et aucune valeur métier chargée dans l’exploration Redis.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
