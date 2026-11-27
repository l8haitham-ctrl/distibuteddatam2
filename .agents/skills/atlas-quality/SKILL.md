---
name: atlas-quality
description: "Maintenir, expliquer ou vérifier la réconciliation du modèle Customer Invoice Payment dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Qualité des données

## Portée et lecture ciblée

Appliquer ce skill pour la réconciliation du modèle Customer Invoice Payment. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [QualityController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/quality/api/QualityController.java)
- [QualityService.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/quality/service/QualityService.java)
- [QualityRules.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/quality/service/QualityRules.java)
- [QualityQueries.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/quality/connector/QualityQueries.java)
- [QualityRequest.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/quality/dto/QualityRequest.java)

## Invariants propres à cette feature

Profil customer_invoice_payment_v1 spécifique, compatible SQL Server/PostgreSQL et jobs COMPLETED de la bonne source, table et schéma. Il attend aussi updated_at dans les colonnes requises du modèle enrichi. Compare comptes, identifiants, montants, contraintes et métriques source avant/après ; ne promet pas de checksum exhaustif ni de snapshot transactionnel. Un rapport avec status=failed peut être HTTP 200.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Écart source/lac, source instable, orphelins, paiements incohérents et jobs d’une autre source ; vérifier chaque observed/expected.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
