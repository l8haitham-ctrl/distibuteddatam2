---
name: atlas-spark
description: "Maintenir, expliquer ou vérifier le driver, les executors et l’écriture des extractions Spark dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Exécution Spark

## Portée et lecture ciblée

Appliquer ce skill pour le driver, les executors et l’écriture des extractions Spark. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [IngestionJob.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/spark/src/main/java/com/atlas/ingestion/IngestionJob.java)
- [NativeSparkReader.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/spark/src/main/java/com/atlas/ingestion/NativeSparkReader.java)
- [SparkJobEngine.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/connector/SparkJobEngine.java)
- [docker-compose.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.yml)

## Invariants propres à cette feature

Spark Master alloue les ressources ; worker héberge driver/executors ; driver planifie les tâches. Application standalone Java en mode cluster. JDBC découpe sur PK entière simple si possible, sinon lecture unique. fetch_size ne contrôle pas la taille d’un fichier. maxRecordsPerFile=100000 est un plafond de lignes. Limites publiées : driver/executor 512m, 1 cœur executor et spark.cores.max=1, un worker Compose 2 cœurs/2g. Le code de ressources configurables est en stash non validé. Écriture ErrorIfExists sous UUID, relecture, count, _SUCCESS puis manifest.json.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Vérifier résultats et partitions effectives ; ne pas présenter des partitions logiques comme preuve de tâches simultanées. Pour prouver le distribué, collecter executors/hôtes/tâches qui se chevauchent, sans déclencher de charge non demandée.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
