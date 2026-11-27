---
name: atlas-maintenance-flow
description: "Maintenir Atlas de bout en bout : choix de branche, diagnostic, modification, tests Docker, documentation et livraison sans perdre les fonctionnalités existantes."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Workflow de maintenance Atlas

Lire [AGENTS.md](../../../AGENTS.md) avant les opérations Git et [support de présentation versionné](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docs/presentation-professeur.md) pour le contexte.

1. Relever branche, git status, changements utilisateur, stashes et périmètre demandé. La référence complète est feature/atlas-platform ; master n’est pas automatiquement fusionnée. Les branches historiques sont des étapes dépendantes, pas toutes des versions complètes.
2. Pour une feature, choisir une branche feature/ descriptive et préciser sa base. Une fonctionnalité garde sa branche ; toute suppression/regroupement doit être demandé explicitement. Le précédent regroupement autorisé concerne seulement job-api-documentation, release-readiness, project-presentation. Ne pas transformer cette exception en règle générale.
3. Lire le skill atlas- de la feature, puis tracer Angular → route HTTP → service → connector → dépendance. Utiliser le catalogue API pour préserver entrées/sorties/authentification. Les services ne dépendent pas de HTTP ; shared contient seulement les erreurs et handlers transverses.
4. Vérifier Docker et les ressources avant les travaux qui en dépendent. Ne pas installer Java/Maven/Node sur l’hôte. Un moteur indisponible bloque les tests, pas la lecture du code. Ne pas appeler une validation statique une validation d’intégration.
5. Implémenter la demande avec migration non destructive si nécessaire. Les credentials restent dans .env/variables, jamais dans Git, logs, arguments de job ni guides. Ne pas effacer volumes, stashes, sauvegardes ou données de référence. Limiter le nettoyage aux objets exacts du job concerné.
6. Exécuter les tests pertinents via les services/profils Docker existants, lus dans README et Compose. Pour connexion/exploration : vérifier contre SQL Server Compose. Pour ingestion : comparer données source et lac et vérifier la table Trino. Pour UI : vérifier le parcours concerné et les contrats réseau. Ne pas lancer de gros jeu de données sans demande.
7. Mettre à jour README si setup/architecture/workflow change et les références API si le contrat change. Examiner le diff, git diff --check et les fichiers à publier ; ne jamais ajouter globalement des fichiers locaux sans revue.
8. Après les checks pertinents réussis, commit/push de la branche selon AGENTS.md ; pas de fusion dans master sans demande. Si tests bloqués, annoncer ce qui n’est pas validé. Les skills sont versionnés sur chaque branche par demande explicite. Le guide personnel de révision et les fichiers work/ restent locaux et ne doivent pas être publiés.
9. Rapporter comportement final, vérifications, limitations et branche/commit. Mettre à jour les guides locaux si la compréhension du projet change.

## État et reprise

Les travaux Spark configurable, timestamp-precision, failed-job-retry, iceberg-tables et le prototype live local sont conservés en stashes. Lire le stash ciblé sans l’appliquer à l’aveugle ; ne pas confondre branche créée et feature terminée. Aucune table Iceberg réelle n’est démontrée dans la référence publiée. La campagne de reprise se trouve dans work/benchmarks/active-rerun.json : son état peut changer ; le vérifier avant de parler d’exécution.

## Diagnostic d’incident

Commencer par les preuves en lecture seule : état Compose, logs bornés et état des jobs. Une soumission incertaine ne doit pas être resoumise automatiquement ; déterminer si Spark a accepté le job. Un redémarrage de service n’autorise ni suppression de volume ni réinitialisation WSL. Si l’opération dépasse la demande, préparer une action précise avant de demander l’autorisation nécessaire.
