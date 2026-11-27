---
name: atlas-storage
description: "Maintenir, expliquer ou vérifier le bucket privé, les objets et les permissions S3 dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Stockage MinIO

## Portée et lecture ciblée

Appliquer ce skill pour le bucket privé, les objets et les permissions S3. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [StorageController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/storage/api/StorageController.java)
- [StorageService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/storage/service/StorageService.java)
- [StorageProvisioner.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/storage/config/StorageProvisioner.java)
- [MinioObjectStorage.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/storage/connector/MinioObjectStorage.java)

## Invariants propres à cette feature

GET /storage/status est la seule route HTTP de cette feature ; pas de téléchargement arbitraire exposé. Comptes ingestion et Trino séparés, bucket privé. Contrôler chemins relatifs, segments . et .. et caractères de contrôle. Ne pas publier les identifiants root ni assimiler vérification du bucket à la validité de tous les jobs.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Permissions minimales, clés invalides, objet absent, bucket indisponible et accès de lecture Trino ; préserver volumes et sauvegardes.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
