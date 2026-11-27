---
name: atlas-deployment
description: "Maintenir, expliquer ou vérifier les services Compose, overlays et contraintes locales de déploiement dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Docker et cible cloud

## Portée et lecture ciblée

Appliquer ce skill pour les services Compose, overlays et contraintes locales de déploiement. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [docker-compose.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.yml)
- [docker-compose.dev.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.dev.yml)
- [docker-compose.analytics.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.analytics.yml)
- [docker-compose.connectors.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.connectors.yml)
- [docker-compose.soccer.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.soccer.yml)
- [docker-compose.clickhouse.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.clickhouse.yml)
- [setup-env.ps1](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/scripts/setup-env.ps1)
- [infra](https://github.com/l8haitham-ctrl/distibuteddatam2/tree/eee650a12f1895f44bcc61951babd0423cba95e1/infra)

## Invariants propres à cette feature

Construire/exécuter/tests via Docker. Lire README pour la combinaison exacte des six fichiers Compose. PostgreSQL metadata et SQL Server source ont des rôles différents ; Hive utilise une base metastore dédiée. Cloud AWS/Azure reste une cible d’architecture, pas une preuve de déploiement. Ne pas démarrer tous les profils tools/benchmark implicitement. Docker bloqué ne justifie pas down -v, reset usine, suppression VHD ou régénération des données.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Valider configuration sans afficher la sortie résolue contenant des secrets ; santé par service, persistance après redémarrage autorisé et accès uniquement aux ports requis.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
