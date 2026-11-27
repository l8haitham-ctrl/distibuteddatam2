---
name: atlas-catalog
description: "Maintenir, expliquer ou vérifier la publication SQL des fichiers et les tables enregistrées dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Catalogue Hive et enregistrement

## Portée et lecture ciblée

Appliquer ce skill pour la publication SQL des fichiers et les tables enregistrées. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [CatalogController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/catalog/api/CatalogController.java)
- [CatalogService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/catalog/service/CatalogService.java)
- [TableDefinition.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/catalog/service/TableDefinition.java)
- [TrinoConnections.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/catalog/connector/TrinoConnections.java)

## Invariants propres à cette feature

Déposer du Parquet dans MinIO ne suffit pas. Enregistrement externe via Trino vers Hive Metastore, synchronisation des partitions et COUNT de vérification. Une table par exécution ; vue courante publiée pour Parquet, pas pour CSV. Contrôler emplacement UUID avant d’adopter ou supprimer une métadonnée. discard ne supprime pas les fichiers du stockage.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Table réellement interrogeable, comptes identiques, partitions découvertes, collision d’emplacement, ancienne vue préservée et indisponibilité catalogue.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
