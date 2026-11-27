---
name: atlas-jobs
description: "Maintenir, expliquer ou vérifier la création, validation et surveillance des extractions asynchrones dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Jobs d’extraction

## Portée et lecture ciblée

Appliquer ce skill pour la création, validation et surveillance des extractions asynchrones. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java)
- [JobService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/service/JobService.java)
- [JobDispatcher.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/service/JobDispatcher.java)
- [JdbcJobRepository.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/connector/JdbcJobRepository.java)
- [SparkJobEngine.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/connector/SparkJobEngine.java)
- [CreateJobRequest.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/dto/CreateJobRequest.java)
- [StorageJob.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/dto/StorageJob.java)

## Invariants propres à cette feature

POST /storage/jobs renvoie 202, pas une extraction terminée. PostgreSQL conserve l’état. JobDispatcher réclame et surveille les jobs ; il ne transporte pas les lignes métier. Soumission incertaine SUBMISSION_UNCERTAIN : ne pas resoumettre automatiquement un POST. COMPLETED exige manifeste, _SUCCESS et catalogue enregistré. fetch_size, num_partitions et partition_by sont trois notions distinctes. Le retry en stash ne constitue pas une API publiée.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Transitions, soumission unique, limites de paramètres, erreurs Spark, timeout, résultat manquant et publication catalogue ; ne pas remplacer ces vérifications par un simple FINISHED Spark.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
