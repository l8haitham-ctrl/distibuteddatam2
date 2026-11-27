# Scénarios d’extraction — 27 novembre 2026

Branche `feature/extraction-scenario-campaign`, basée sur
`feature/extraction-failure-monitoring` (`dd339d6`). La campagne utilise la même
API authentifiée que l’interface, sans insertion directe de jobs dans la base.

40 scénarios représentatifs des options disponibles ont été exécutés une fois,
et les 40 ont été vérifiés jusqu’à Trino. Il ne s’agit pas du produit cartésien
de toutes les valeurs possibles ni d’un benchmark statistique avec répétitions.
Les références source avant/après sont identiques pour les métriques contrôlées.
Les variantes sans partitionnement restent distinctes des contre-exemples
historiques à plusieurs milliers de petits fichiers, qui n’ont pas été rejoués.

## Ce qui a été vérifié

- Formats CSV et Parquet, toutes leurs compressions admises sur SQL Server,
  PostgreSQL, ClickHouse et Redis.
- Modèle Customer / Invoice / Payment sur les trois moteurs qui l’exposent ;
  mapping Redis customer_segments, fixture benchmark de 100000 lignes et Country
  de la source soccer.
- Partitions annuelles, mensuelles, quotidiennes, catégorielles et combinées ;
  CSV partitionné ; tailles de lots 100/1000/10000 et lectures JDBC 1/2/4.
- Comptes, unicité des identifiants, sommes des identifiants, sommes des montants
  lorsque présents ; pour Redis, clés uniques, sommes et répartition des segments.
- Cohérence du compte du manifeste avec les résultats SQL, table catalogue
  réellement interrogeable et absence de troncature du résultat de contrôle.

Chaque job reste dans l’historique PostgreSQL de la plateforme. Le tableau donne
son UUID ; les liens locaux ouvrent la page de détail de l’interface sur le PC
où Atlas s’exécute. GET /storage/jobs renvoie les 100 jobs les plus récents ; les
jobs plus anciens restent consultables par UUID et ne sont pas effacés.

## Résultats mesurés

La durée est finished_at − started_at et exclut l’attente initiale. Les durées
lecture/écriture et catalogue ont leurs champs séparés dans le fichier JSON.
Une seule observation par scénario ne permet pas d’affirmer un gain universel.

| # | Scénario | Lignes | Fichiers | Octets | Durée (s) | Job dans Atlas |
|---|---|---:|---:|---:|---:|---|
| 1 | `sqlserver_parquet_snappy` | 5000 | 1 | 61181 | 41.6 | [001742d3-2363-4791-8b18-0d6fc1000599](http://127.0.0.1:3000/jobs/001742d3-2363-4791-8b18-0d6fc1000599) |
| 2 | `sqlserver_parquet_uncompressed` | 5000 | 1 | 98264 | 38.4 | [2226c1bb-1caf-46ad-83ec-d8be02b303a3](http://127.0.0.1:3000/jobs/2226c1bb-1caf-46ad-83ec-d8be02b303a3) |
| 3 | `sqlserver_parquet_zstd` | 5000 | 1 | 30397 | 30.0 | [38144f51-1ae6-4141-abee-208d5ba49aff](http://127.0.0.1:3000/jobs/38144f51-1ae6-4141-abee-208d5ba49aff) |
| 4 | `sqlserver_csv_uncompressed` | 5000 | 1 | 264509 | 27.6 | [4bc51a91-8b39-4775-90b2-0e2092a22518](http://127.0.0.1:3000/jobs/4bc51a91-8b39-4775-90b2-0e2092a22518) |
| 5 | `sqlserver_csv_gzip` | 5000 | 1 | 65763 | 29.3 | [19eb1cf1-c500-4777-9e23-c1f97e95bac7](http://127.0.0.1:3000/jobs/19eb1cf1-c500-4777-9e23-c1f97e95bac7) |
| 6 | `postgresql_parquet_snappy` | 5000 | 1 | 53621 | 29.5 | [3824553f-89de-4584-b0b8-9d27d02283e2](http://127.0.0.1:3000/jobs/3824553f-89de-4584-b0b8-9d27d02283e2) |
| 7 | `postgresql_parquet_uncompressed` | 5000 | 1 | 83306 | 29.6 | [13ef121f-0d63-4320-aac6-4e7a3bd5aa4c](http://127.0.0.1:3000/jobs/13ef121f-0d63-4320-aac6-4e7a3bd5aa4c) |
| 8 | `postgresql_parquet_zstd` | 5000 | 1 | 25469 | 27.6 | [82cc1f10-b215-40a9-8c65-ebbe5d9c68d8](http://127.0.0.1:3000/jobs/82cc1f10-b215-40a9-8c65-ebbe5d9c68d8) |
| 9 | `postgresql_csv_uncompressed` | 5000 | 1 | 262358 | 33.8 | [40ea31cb-1852-4da9-af21-1973f00fbc04](http://127.0.0.1:3000/jobs/40ea31cb-1852-4da9-af21-1973f00fbc04) |
| 10 | `postgresql_csv_gzip` | 5000 | 1 | 49404 | 31.6 | [d174aabf-c20e-4d6a-be6f-51a71066c1c2](http://127.0.0.1:3000/jobs/d174aabf-c20e-4d6a-be6f-51a71066c1c2) |
| 11 | `clickhouse_parquet_snappy` | 5000 | 1 | 61064 | 27.4 | [59748881-1562-45ee-b5c2-cba38c4e70d1](http://127.0.0.1:3000/jobs/59748881-1562-45ee-b5c2-cba38c4e70d1) |
| 12 | `clickhouse_parquet_uncompressed` | 5000 | 1 | 135469 | 27.7 | [66b4dd66-b9ac-44bb-b724-844b510a4a9a](http://127.0.0.1:3000/jobs/66b4dd66-b9ac-44bb-b724-844b510a4a9a) |
| 13 | `clickhouse_parquet_zstd` | 5000 | 1 | 26287 | 31.5 | [e2b3e2bc-61e6-40e3-b6cc-1f5f7db92192](http://127.0.0.1:3000/jobs/e2b3e2bc-61e6-40e3-b6cc-1f5f7db92192) |
| 14 | `clickhouse_csv_uncompressed` | 5000 | 1 | 165789 | 27.1 | [fb77de06-988a-4197-985d-bcaa1910a974](http://127.0.0.1:3000/jobs/fb77de06-988a-4197-985d-bcaa1910a974) |
| 15 | `clickhouse_csv_gzip` | 5000 | 1 | 44062 | 29.3 | [9c39b7aa-27dc-4466-a0f4-9f5f1988ae77](http://127.0.0.1:3000/jobs/9c39b7aa-27dc-4466-a0f4-9f5f1988ae77) |
| 16 | `redis_parquet_snappy` | 1000 | 1 | 13629 | 34.0 | [60893704-f40f-417c-ab9f-345a2daaf109](http://127.0.0.1:3000/jobs/60893704-f40f-417c-ab9f-345a2daaf109) |
| 17 | `redis_parquet_uncompressed` | 1000 | 1 | 51181 | 31.6 | [936160d3-a996-47f8-bea2-3cc15d163300](http://127.0.0.1:3000/jobs/936160d3-a996-47f8-bea2-3cc15d163300) |
| 18 | `redis_parquet_zstd` | 1000 | 1 | 3255 | 31.7 | [c968eec0-7935-4ac1-80a0-2a744fbdae80](http://127.0.0.1:3000/jobs/c968eec0-7935-4ac1-80a0-2a744fbdae80) |
| 19 | `redis_csv_uncompressed` | 1000 | 1 | 48679 | 36.1 | [d2eb6bdd-4f31-4a45-8bef-56850cab9725](http://127.0.0.1:3000/jobs/d2eb6bdd-4f31-4a45-8bef-56850cab9725) |
| 20 | `redis_csv_gzip` | 1000 | 1 | 7335 | 31.2 | [25adf0f6-08b0-4b46-b4b5-1b61e2dcf421](http://127.0.0.1:3000/jobs/25adf0f6-08b0-4b46-b4b5-1b61e2dcf421) |
| 21 | `sqlserver_partition_year` | 5000 | 2 | 77983 | 32.0 | [74f5d347-831d-4ccf-a8f7-31dc44831c3b](http://127.0.0.1:3000/jobs/74f5d347-831d-4ccf-a8f7-31dc44831c3b) |
| 22 | `sqlserver_partition_month` | 5000 | 24 | 133216 | 34.6 | [47c46ebb-4770-409d-96c4-5e1dd7794360](http://127.0.0.1:3000/jobs/47c46ebb-4770-409d-96c4-5e1dd7794360) |
| 23 | `sqlserver_partition_day` | 5000 | 730 | 1890177 | 94.3 | [5d361883-63ae-4c1e-b6b6-67b3a837a33a](http://127.0.0.1:3000/jobs/5d361883-63ae-4c1e-b6b6-67b3a837a33a) |
| 24 | `sqlserver_partition_status` | 5000 | 3 | 70084 | 31.7 | [fc5ce42b-732c-4776-912b-3b9eb415bee9](http://127.0.0.1:3000/jobs/fc5ce42b-732c-4776-912b-3b9eb415bee9) |
| 25 | `sqlserver_csv_gzip_month` | 5000 | 24 | 54537 | 39.2 | [c155ee84-f9bd-4d5c-a07c-78bb9d86b9fb](http://127.0.0.1:3000/jobs/c155ee84-f9bd-4d5c-a07c-78bb9d86b9fb) |
| 26 | `sqlserver_month_and_status` | 5000 | 72 | 219504 | 43.3 | [6bf18071-547a-4e0f-a626-29ca604658b9](http://127.0.0.1:3000/jobs/6bf18071-547a-4e0f-a626-29ca604658b9) |
| 27 | `postgresql_partition_month` | 5000 | 24 | 128159 | 33.2 | [b6c3aa45-1b33-4ee2-afab-e73b114e4601](http://127.0.0.1:3000/jobs/b6c3aa45-1b33-4ee2-afab-e73b114e4601) |
| 28 | `sqlserver_fetch_100` | 5000 | 1 | 61181 | 41.0 | [8ec7ea7f-4e4d-40e1-b827-d0af4ed0b31c](http://127.0.0.1:3000/jobs/8ec7ea7f-4e4d-40e1-b827-d0af4ed0b31c) |
| 29 | `sqlserver_fetch_10000` | 5000 | 1 | 61181 | 45.9 | [ac544d9b-61fb-4aa0-9268-6f09e47d902b](http://127.0.0.1:3000/jobs/ac544d9b-61fb-4aa0-9268-6f09e47d902b) |
| 30 | `sqlserver_readers_2` | 5000 | 2 | 78291 | 40.3 | [983d434e-5424-48a3-9189-41376a1b8966](http://127.0.0.1:3000/jobs/983d434e-5424-48a3-9189-41376a1b8966) |
| 31 | `sqlserver_readers_4` | 5000 | 4 | 105302 | 33.6 | [7483f8dc-2279-4544-8472-dc3200bb5030](http://127.0.0.1:3000/jobs/7483f8dc-2279-4544-8472-dc3200bb5030) |
| 32 | `postgresql_readers_2` | 5000 | 2 | 66125 | 44.8 | [ae95dcc7-dd87-4e13-9d69-bf76367600db](http://127.0.0.1:3000/jobs/ae95dcc7-dd87-4e13-9d69-bf76367600db) |
| 33 | `sqlserver_customer` | 1000 | 1 | 26071 | 31.4 | [73ce50e8-0e88-4f8a-a068-4ed268b7ee08](http://127.0.0.1:3000/jobs/73ce50e8-0e88-4f8a-a068-4ed268b7ee08) |
| 34 | `sqlserver_payment` | 8000 | 1 | 108096 | 29.2 | [26b3a0ab-8923-4fbb-bebb-5c888bf90072](http://127.0.0.1:3000/jobs/26b3a0ab-8923-4fbb-bebb-5c888bf90072) |
| 35 | `postgresql_customer` | 1000 | 1 | 36719 | 27.2 | [f6987136-2ebe-4ca5-a4b9-f4834ce02bb9](http://127.0.0.1:3000/jobs/f6987136-2ebe-4ca5-a4b9-f4834ce02bb9) |
| 36 | `postgresql_payment` | 8000 | 1 | 103128 | 29.2 | [00f08413-afce-4170-9a49-dd0852c30ff0](http://127.0.0.1:3000/jobs/00f08413-afce-4170-9a49-dd0852c30ff0) |
| 37 | `clickhouse_customer` | 1000 | 1 | 18709 | 27.2 | [ffd615d9-715a-4148-acf3-7377c7117991](http://127.0.0.1:3000/jobs/ffd615d9-715a-4148-acf3-7377c7117991) |
| 38 | `clickhouse_payment` | 8000 | 1 | 102021 | 27.3 | [b7a93c34-89ae-4d9b-8d95-13a8beb8a0f3](http://127.0.0.1:3000/jobs/b7a93c34-89ae-4d9b-8d95-13a8beb8a0f3) |
| 39 | `benchmark_100k` | 100000 | 2 | 1289934 | 31.6 | [eca8427c-a8c3-4abc-8ed2-4da0ccbc3eb3](http://127.0.0.1:3000/jobs/eca8427c-a8c3-4abc-8ed2-4da0ccbc3eb3) |
| 40 | `soccer_country` | 11 | 1 | 1114 | 31.4 | [4cecbdb4-101e-4667-87d2-3c096a58dc0e](http://127.0.0.1:3000/jobs/4cecbdb4-101e-4667-87d2-3c096a58dc0e) |

## Demandes invalides

Ces sept demandes ont été refusées avant création d’un job. Elles sont conservées
dans les preuves de validation ; aucun faux échec n’a été ajouté à l’historique.

| Cas | Statut attendu | Statut observé |
|---|---:|---:|
| `parquet_gzip` | 422 | 422 |
| `csv_snappy` | 422 | 422 |
| `primary_key_partition` | 422 | 422 |
| `redis_partition` | 422 | 422 |
| `minio_not_an_extraction_source` | 404 | 404 |
| `missing_table` | 404 | 404 |
| `zero_readers` | 422 | 422 |

MinIO est la destination et un espace explorable ; il n’existe pas de lecteur
source MinIO dans les jobs actuels. Sa demande d’extraction est donc rejetée,
ce qui ne signifie pas une panne du stockage.

## Contre-exemples déjà présents dans l’historique

- `9230be9f-620b-476c-b4e7-ad8adabf044e` : 5000 lignes, 4000 fichiers,
  partitionnement customer_id + status.
- `0a7fa971-ff80-40c7-a8bf-86406570079a` : 5000 lignes, 4770 fichiers,
  partitionnement customer_id + invoice_date.

Ces deux résultats sont des références historiques, pas deux nouvelles
exécutions de cette campagne. Les partitions ne sont pas toujours une
optimisation : leur cardinalité et le nombre de fichiers doivent être évalués.

## Reprise et limites

Le runner et l’overlay sont décrits dans le README. Le rapport local
work/scenario-campaign/report.json enregistre la tentative avant le POST puis
l’UUID reçu ; après une soumission ambiguë, il s’arrête pour éviter un doublon.
La reprise relit les mêmes UUID et ne resoumet pas les cas terminés. Ne pas
effacer le rapport pour contourner cette protection.

La reprise de cette campagne terminée a été testée avec l’overlay versionné :
aucun job supplémentaire n’a été créé.

Les vérifications sont des contrôles agrégés, pas des checksums exhaustifs de
chaque cellule. Elles ne fournissent pas de snapshot transactionnel global ni
de preuve de scalabilité multiworker. La campagne ne modifie pas les sources,
ne régénère pas les fixtures et ne supprime pas les extractions existantes.

[Preuves JSON](../validation/results/extraction-scenarios-2026-11-27.json).
