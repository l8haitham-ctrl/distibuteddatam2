# Rétablissement des extractions — 27 novembre 2026

Branche : `feature/extraction-failure-monitoring`, basée sur
`feature/atlas-platform` au commit `31125eb`. Aucun travail en stash ni aucune
fusion dans `master` n'est inclus.

## Diagnostic et correction

Le conteneur MinIO répondait à sa sonde locale mais n'avait plus de raccordement
réseau Docker. Spark échouait avec `UnknownHostException: minio`. Le raccordement
au réseau Compose `distributed-data_default`, avec l'alias `minio`, a été rétabli
sans suppression de volumes. La cause de cette déconnexion n'est pas établie.

Le suivi consultait le manifeste avant l'état du driver. Une erreur de stockage
empêchait donc de constater un échec Spark jusqu'au timeout global. Désormais,
une indisponibilité du stockage déclenche aussi une consultation du driver :
`ERROR`, `FAILED`, `KILLED` et `LOST` donnent l'erreur `SPARK_<état>` existante.
`FINISHED`, `UNKNOWN` ou une panne du moteur ne prouvent pas l'échec pendant
l'indisponibilité du manifeste. Le timeout reste appliqué et aucun job n'est
resoumis automatiquement. Les contrats HTTP restent inchangés.

## Validation

- 21 tests `JobTests` et `CatalogTests` réussis dans Docker, dont les régressions
  échec Spark pendant panne stockage, états ambigus et panne simultanée.
- Backend corrigé construit dans Docker et déployé localement, image précédente
  conservée ; aucun redémarrage de Spark, de MinIO ou des bases pour ce déploiement.
- Deux extractions Parquet/Snappy sans partition, une lecture, via API authentifiée
  avec session/CSRF ; contrôle du compte et des sommes via Trino et du nombre de
  lignes annoncé dans le manifeste.

| Source | Lignes | Fichiers | Lecture + écriture | Catalogue |
|---|---:|---:|---:|---:|
| SQL Server / dbo.Invoice | 5 000 | 1 | 13 900 ms | 2 279 ms |
| ClickHouse / analytics.invoice | 5 000 | 1 | 12 916 ms | 1 176 ms |

Ce sont des contrôles ponctuels de rétablissement, pas une campagne comparative
ni une preuve de performance stable. Le job utilisateur partitionné par
`customer_id,status` a également terminé : 5 000 lignes mais 4 000 fichiers.
Cette organisation provoque un coût important d'écriture et de catalogue ; pour
ce volume, commencer sans partitionnement et évaluer ensuite une date mensuelle.

Pendant la vérification, un ancien point d'entrée d'image Docker a lancé par
erreur le runner de benchmark. Il a été arrêté après une seule soumission ; ce
job de 100 000 lignes avait déjà terminé au moment du contrôle. Le rapport partiel
est conservé uniquement dans `work/extraction-repair/`. Aucune nouvelle campagne
complète n'a été exécutée, et les résultats historiques restent inchangés.

Preuves synthétiques :
[extraction-recovery-2026-11-27.json](../validation/results/extraction-recovery-2026-11-27.json).

## Reproduire les tests unitaires

```powershell
docker compose --profile tools run --rm --no-deps --pull never -e RUN_DB_TESTS=false -e RUN_STORAGE_TESTS=false -e RUN_JOB_TESTS=false backend-tests mvn -B '-Dtest=JobTests,CatalogTests' test
```

Pour reconstruire un environnement depuis le dépôt, utiliser les commandes
Docker et les six fichiers Compose documentés dans le README. Les contrôles
réels créent des jobs et des objets ; ils ne doivent pas être lancés en boucle
ni resoumis automatiquement après une réponse incertaine.
