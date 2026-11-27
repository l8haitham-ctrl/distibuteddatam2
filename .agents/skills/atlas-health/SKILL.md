---
name: atlas-health
description: "Maintenir, expliquer ou vérifier la santé HTTP et les sondes de disponibilité dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Santé du service

## Portée et lecture ciblée

Appliquer ce skill pour la santé HTTP et les sondes de disponibilité. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [HealthController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/health/HealthController.java)

## Invariants propres à cette feature

GET /health renvoie status=ok ; cela prouve la réponse du backend, pas le fonctionnement de SQL Server, Spark, MinIO et Trino. Distinguer cette route des sondes Actuator liveness/readiness et des diagnostics métier.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Réponse sans authentification, absence de secrets et pas de fausse promesse sur la santé de toutes les dépendances.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
