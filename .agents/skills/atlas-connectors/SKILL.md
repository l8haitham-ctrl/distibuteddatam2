---
name: atlas-connectors
description: "Maintenir, expliquer ou vérifier l’extension des bases relationnelles et la découverte des pilotes dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Plugins JDBC

## Portée et lecture ciblée

Appliquer ce skill pour l’extension des bases relationnelles et la découverte des pilotes. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [RelationalPlugin.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectors/connector/RelationalPlugin.java)
- [PluginRegistry.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectors/connector/PluginRegistry.java)
- [SourceDefinitions.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectors/connector/SourceDefinitions.java)
- [ConnectorService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectors/service/ConnectorService.java)

## Invariants propres à cette feature

Plugins actuels SQL Server et PostgreSQL. Une plateforme extensible ne signifie pas que tous les pilotes sont installés. Partager le plugin entre API et application Spark ; contrôler ServiceLoader, classpath du JAR et quoting des identifiants. available indique la présence du pilote, pas une connexion réseau réussie.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Découverte, pilote manquant, URLs TLS, métadonnées, extraction réelle et même plugin accessible dans Spark.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
