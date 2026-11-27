---
name: atlas-spark-resources
description: "Maintenir, expliquer ou vérifier la reprise du travail local de configuration CPU et mémoire Spark dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Ressources Spark configurables — en cours

## Portée et lecture ciblée

Appliquer ce skill pour la reprise du travail local de configuration CPU et mémoire Spark. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [SparkJobEngine.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/connector/SparkJobEngine.java)
- [docker-compose.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.yml)

## Invariants propres à cette feature

Branche locale feature/configurable-spark-execution ; modifications conservées dans le stash dont le message mentionne Spark resource configuration. Inspecter git stash list puis git stash show sur la référence sélectionnée, car les indices changent. Ne pas appliquer tous les stashes ni supposer la feature validée. Référence publiée limitée à un cœur ; examiner la configuration proposée avant de l’expliquer comme disponible.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

À la reprise demandée : valider propriétés CPU/mémoire, comportement sans nouvelle option, limites matérielles, Compose et application réellement lancée avec ces ressources ; tests Docker avant livraison.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
