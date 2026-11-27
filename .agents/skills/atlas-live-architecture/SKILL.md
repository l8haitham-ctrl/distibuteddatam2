---
name: atlas-live-architecture
description: "Maintenir, expliquer ou vérifier le prototype local de visualisation temps réel de l’architecture Atlas dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Architecture live locale — prototype

## Portée et lecture ciblée

Appliquer ce skill pour le prototype local de visualisation temps réel de l’architecture Atlas. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [app.routes.ts](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/frontend/src/app/app.routes.ts)
- [docker-compose.yml](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docker-compose.yml)

## Invariants propres à cette feature

Branche feature/local-live-architecture et stash du prototype, demandé pour test local uniquement. Ne pas publier sur GitHub ni l’ajouter au parcours principal sans demande. Une animation n’est une preuve de trafic que si les données viennent de métriques/événements réels ; signaler explicitement les états simulés.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Vérifier provenance des états, arrêt des abonnements et absence de secrets ; pas de requête lourde ou extraction déclenchée pour animer le diagramme.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
