---
name: atlas-sql
description: "Maintenir, expliquer ou vérifier les requêtes SQL en lecture seule et leurs statistiques dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Console SQL Trino

## Portée et lecture ciblée

Appliquer ce skill pour les requêtes SQL en lecture seule et leurs statistiques. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [SqlController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/sql/api/SqlController.java)
- [SqlService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/sql/service/SqlService.java)
- [SqlRequest.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/sql/dto/SqlRequest.java)
- [SqlExceptionHandler.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/sql/api/SqlExceptionHandler.java)

## Invariants propres à cette feature

SELECT/WITH/SHOW/DESCRIBE admis, point-virgule final retiré ; multi-instructions et commentaires refusés. La sécurité dépend aussi du compte Trino lecteur et de ses règles, pas seulement de la validation textuelle. max_rows limite les lignes retournées, pas nécessairement le volume scanné ; timeout 60 s. Spark ne traite pas ces requêtes.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Écriture refusée, commentaires et multi-instructions refusés, rows/columns cohérents, max_rows+1 pour détecter truncated, erreur 422 ou service 503.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
