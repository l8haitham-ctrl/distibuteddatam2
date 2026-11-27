---
name: atlas-job-retry
description: "Maintenir, expliquer ou vérifier la reprise du travail local de retry des extractions en échec dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Relance des jobs — en cours

## Portée et lecture ciblée

Appliquer ce skill pour la reprise du travail local de retry des extractions en échec. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [JobService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/service/JobService.java)
- [JobDispatcher.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/service/JobDispatcher.java)
- [JdbcJobRepository.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/connector/JdbcJobRepository.java)

## Invariants propres à cette feature

Branche locale feature/failed-job-retry ; modifications en stash failed-job retry. Pas de POST retry dans la référence publiée. Avant toute reprise, inspecter la proposition pour UUID/préfixe distinct, lien parent/enfant et règles d’éligibilité. SUBMISSION_UNCERTAIN ne signifie pas que Spark n’a rien exécuté ; ne pas relancer automatiquement une demande ambiguë.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Valider demande concurrente, limite des relances, état du parent, absence d’écrasement de résultat et absence de doublon de soumission ; une simulation SQL seule ne valide pas le parcours Java/UI.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
