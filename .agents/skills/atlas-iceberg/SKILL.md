---
name: atlas-iceberg
description: "Maintenir, expliquer ou vérifier la reprise de l’intégration locale Iceberg sans remplacer le pipeline Hive existant dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Tables Iceberg — en cours

## Portée et lecture ciblée

Appliquer ce skill pour la reprise de l’intégration locale Iceberg sans remplacer le pipeline Hive existant. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [CatalogService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/catalog/service/CatalogService.java)
- [trino](https://github.com/l8haitham-ctrl/distibuteddatam2/tree/eee650a12f1895f44bcc61951babd0423cba95e1/infra/trino)
- [roadmap.md](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docs/roadmap.md)

## Invariants propres à cette feature

Branche locale feature/iceberg-tables ; modifications en stash Iceberg integration. Aucune table Iceberg réelle validée dans la référence. Inspecter le stash en lecture seule pour connaître les fichiers proposés et dépendances. Iceberg est un format de table avec métadonnées/snapshots, pas un format de fichier remplaçant nécessairement Parquet. Garder le pipeline Hive et ses contrats existants.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Avant de déclarer terminé : compilation/tests Docker, table réelle, métadonnées et snapshots consultables, données identiques au snapshot source et régression Hive/MinIO/auth ; ne pas appeler une seule compilation preuve de fonctionnement Iceberg.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
