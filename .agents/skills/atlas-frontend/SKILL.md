---
name: atlas-frontend
description: "Maintenir, expliquer ou vérifier les pages Angular, les services HTTP et le parcours utilisateur dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Interface Angular

## Portée et lecture ciblée

Appliquer ce skill pour les pages Angular, les services HTTP et le parcours utilisateur. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [app.routes.ts](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/frontend/src/app/app.routes.ts)
- [features](https://github.com/l8haitham-ctrl/distibuteddatam2/tree/eee650a12f1895f44bcc61951babd0423cba95e1/frontend/src/app/features)
- [core](https://github.com/l8haitham-ctrl/distibuteddatam2/tree/eee650a12f1895f44bcc61951babd0423cba95e1/frontend/src/app/core)

## Invariants propres à cette feature

Login puis shell protégé par authGuard. Sources unifiées, sélection explicite, explorateur commun, assistant extraction, stockage/jobs, SQL, glossaire. /sources/native est une redirection de compatibilité. Préserver le wireframe et distinguer id de profil natif et source_id d’extraction. Afficher les vrais événements SSE et états ; pas de faux progrès. L’API peut limiter les données renvoyées : afficher truncated et erreurs.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Tests Angular dans Docker ; parcours login → sources → test → explorer → extraction → suivi → SQL ; tester SQL Server, Redis, ClickHouse et MinIO sans rendre leurs concepts artificiellement identiques.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
