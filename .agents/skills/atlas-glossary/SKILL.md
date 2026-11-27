---
name: atlas-glossary
description: "Maintenir, expliquer ou vérifier les termes métier et leurs liens vers tables et colonnes dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Glossaire métier

## Portée et lecture ciblée

Appliquer ce skill pour les termes métier et leurs liens vers tables et colonnes. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java)
- [GlossaryService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/service/GlossaryService.java)
- [JdbcGlossaryRepository.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/connector/JdbcGlossaryRepository.java)
- [TermRequest.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/dto/TermRequest.java)
- [LinkRequest.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/dto/LinkRequest.java)

## Invariants propres à cette feature

Le glossaire décrit le sens métier ; il ne génère ni SQL ni transformation automatique. Métadonnées persistées dans PostgreSQL. Les liens source/schéma/table/colonne sont vérifiés contre la structure relationnelle avec casse exacte. DRAFT et VALIDATED sont des statuts éditoriaux. Ne pas étendre silencieusement aux sources natives.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

CRUD disponible : création/lecture/modification de termes, ajout/suppression de liens ; pas de suppression de terme exposée. Doublons, cible absente, pagination et XSS côté UI.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
