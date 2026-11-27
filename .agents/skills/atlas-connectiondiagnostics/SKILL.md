---
name: atlas-connectiondiagnostics
description: "Maintenir, expliquer ou vérifier les étapes visibles du test de connexion et le flux SSE dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Diagnostic des connexions

## Portée et lecture ciblée

Appliquer ce skill pour les étapes visibles du test de connexion et le flux SSE. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [ConnectionDiagnosticsController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectiondiagnostics/api/ConnectionDiagnosticsController.java)
- [ConnectionDiagnostics.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectiondiagnostics/service/ConnectionDiagnostics.java)
- [DiagnosticEvent.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectiondiagnostics/dto/DiagnosticEvent.java)

## Invariants propres à cette feature

POST /sources/{id}/test/events produit text/event-stream : événements progress avec step/state/message/duration_ms. Étapes dns, tcp, jdbc, validation, complete ; états running/passed/failed/skipped. Le HTTP peut être 200 alors que complete signale failed. TLS et authentification sont une étape JDBC, pas des tests indépendants. Respecter annulation et timeout 90 s.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Ordre des étapes, échec anticipé et étapes skipped, déconnexion du client, CSRF pour le POST SSE.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
