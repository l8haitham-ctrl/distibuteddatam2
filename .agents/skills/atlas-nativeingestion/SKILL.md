---
name: atlas-nativeingestion
description: "Maintenir, expliquer ou vérifier les lecteurs d’extraction non JDBC utilisés par Spark dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Extraction Redis et ClickHouse

## Portée et lecture ciblée

Appliquer ce skill pour les lecteurs d’extraction non JDBC utilisés par Spark. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [NativeIngestionSources.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativeingestion/connector/NativeIngestionSources.java)
- [ClickhouseExtraction.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativeingestion/connector/ClickhouseExtraction.java)
- [RedisHashExtraction.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativeingestion/connector/RedisHashExtraction.java)
- [RowStream.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativeingestion/connector/RowStream.java)

## Invariants propres à cette feature

Sources d’extraction identifiées redis et clickhouse, distinctes des profils UI redis-demo/clickhouse-demo. Lecteurs natifs à une partition ; partition_by non vide refusé actuellement. Redis demande un mapping explicite des hashes et une déduplication des clés ; SCAN n’est pas un snapshot transactionnel. MinIO est exploré et sert de destination ; pas de lecteur natif MinIO vers job publié.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Mapping invalide, types non supportés, mémoire bornée, doublons SCAN, expiration des clés, limites du transport et réconciliation après extraction.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
