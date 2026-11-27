---
name: atlas-benchmarks
description: "Maintenir, expliquer ou vérifier les campagnes CSV Parquet compression partitionnement et lots JDBC dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Benchmarks reproductibles

## Portée et lecture ciblée

Appliquer ce skill pour les campagnes CSV Parquet compression partitionnement et lots JDBC. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [plan.mjs](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/benchmarks/plan.mjs)
- [run.mjs](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/benchmarks/run.mjs)
- [audit.mjs](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/benchmarks/audit.mjs)
- [report.mjs](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/benchmarks/report.mjs)
- [results](https://github.com/l8haitham-ctrl/distibuteddatam2/tree/eee650a12f1895f44bcc61951babd0423cba95e1/benchmarks/results)
- [benchmark-report.md](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docs/benchmark-report.md)

## Invariants propres à cette feature

Préserver campagne local-2026-11-22-100k. 8 scénarios, 3 répétitions : 24 extractions mesurées, 216 mesures SQL, 72 warmups SQL ; un warmup extraction supplémentaire exclu du résumé. extract_write_ms inclut lecture+conversion+écriture ; registration_ms concerne le catalogue, pas le seul transfert. Même fixture et mêmes requêtes. --run-id neuf ; --resume pour reprendre ; jamais relancer automatiquement un POST ambigu. La reprise statistique préparée dans work/benchmarks/active-rerun.json et la campagne fonctionnelle de 40 scénarios sont distinctes : vérifier leurs rapports respectifs au lieu de déduire leur état depuis un ancien incident Docker.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Le runner `scripts/extraction-scenarios.mjs`, sur `feature/extraction-scenario-campaign`, couvre 40 contrôles fonctionnels uniques et sept rejets de validation. Il ne constitue pas une campagne statistique répétée. Utiliser `docker-compose.scenarios.yml` avec point d'entrée Node explicite. Chaque job réel reste dans PostgreSQL ; les labels des scénarios et UUID sont dans le rapport persistant. Reprendre le même rapport et ne jamais resoumettre automatiquement un POST ambigu. Les demandes rejetées ne doivent pas être injectées comme faux jobs. Vérifier les métriques source avant/après et distinguer les contre-exemples historiques de petits fichiers des nouvelles extractions.

Sur cette branche, lire les [résultats et liens vers l'historique](../../../docs/extraction-scenarios-2026-11-27.md) pour préparer une démonstration ; ne pas annoncer un résultat simplement parce que son scénario figure dans le plan.

Valider comptes/sommes/groupes, audit de complétude et dispersion. Documenter caches et ordre ; pas de conclusion cluster multicœur à partir du test actuel limité à un cœur.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
