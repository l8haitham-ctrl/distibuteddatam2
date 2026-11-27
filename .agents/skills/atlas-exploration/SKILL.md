---
name: atlas-exploration
description: "Maintenir, expliquer ou vérifier les tables, colonnes, clés primaires et étrangères dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Exploration relationnelle

## Portée et lecture ciblée

Appliquer ce skill pour les tables, colonnes, clés primaires et étrangères. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java)
- [ExplorationService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/exploration/service/ExplorationService.java)
- [SqlServerConnector.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/exploration/connector/SqlServerConnector.java)
- [JdbcSchemaConnector.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/exploration/connector/JdbcSchemaConnector.java)

## Invariants propres à cette feature

Explorer la structure via métadonnées, sans charger les lignes métier. SQL Server utilise sys.* ; les autres plugins utilisent DatabaseMetaData. Les champs additionnels JDBC ne sont pas identiques à ceux SQL Server. Préserver noms exacts et schémas ; échapper les motifs JDBC %, _. L’utilisateur choisit explicitement la source avant exploration.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Tables homonymes sur deux schémas, colonnes nullables et types, PK/FK composites, table absente et droits de lecture ; SQL Server Compose obligatoire.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
