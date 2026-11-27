---
name: atlas-timestamp-precision
description: "Maintenir, expliquer ou vérifier la fidélité temporelle entre source, Spark, CSV et Trino dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Précision des timestamps — en cours

## Portée et lecture ciblée

Appliquer ce skill pour la fidélité temporelle entre source, Spark, CSV et Trino. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [IngestionJob.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/spark/src/main/java/com/atlas/ingestion/IngestionJob.java)
- [TableDefinition.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/catalog/service/TableDefinition.java)

## Invariants propres à cette feature

Branche locale feature/timestamp-precision ; correctif conservé dans le stash timestamp fidelity. Le CSV publié utilise un format à la seconde. Une date affichée ne prouve pas une conservation de la fraction ; distinguer DATE, TIMESTAMP, timezone et précision du catalogue. Examiner le correctif et sa politique de rejet avant de promettre la fidélité milliseconde ou submilliseconde.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Comparer valeurs source et SQL lac avec fractions et valeurs limites ; vérifier CSV et Parquet, schéma catalogue, timezone et messages explicites lorsque la précision ne peut pas être conservée.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
