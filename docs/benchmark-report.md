# Resultats des benchmarks locaux

Campagne `local-2026-11-22-100k`, du 22 novembre 2026 23:32:27 UTC
au 22 novembre 2026 23:52:41 UTC. Branche `feature/reproducible-benchmarks`.

Les 24 extractions mesurees ont reussi. Une extraction supplementaire
d'echauffement est exclue. Les 216 requetes mesurees et les 72 requetes
d'echauffement ont des identifiants uniques et des resultats conformes.
L'audit final a retourne `integrity=passed`.

## Donnees et environnement

- SQL Server : BusinessBenchmark.benchmark.Invoice100k, six colonnes.
- 100 000 factures ; dates du 01/01/2025 au 31/12/2026.
- SUM(amount)=104 634 500,00 ; SUM(invoice_id)=5 000 050 000.
- Janvier 2025 : 4 247 factures, montant 2 442 323,84.
- Statuts : paid 70 000, partial 20 000, unpaid 10 000.
- Docker 29.0.1 : 8 CPU logiques et 7,64 Gio de RAM disponibles.
- Spark : deux lecteurs JDBC demandes, un coeur executeur, heap driver et
  executeur 512 Mio ; maxRecordsPerFile=100000, fixes entre scenarios.

Le [protocole](benchmarks.md) definit les chronometrages et le SQL. Le JSON
de campagne conserve l'environnement initial et les identifiants des images.

## Tableau des medianes

| Scenario | MiB donnees | Fichiers | Lecture + ecriture (s) | Verification (s) | Catalogue (s) | Pipeline serveur (s) | SQL global (ms) | SQL janvier (ms) | SQL statut (ms) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| csv_raw | 5.290 | 2 | 19.121 | 2.436 | 0.623 | 37.698 | 159 | 580 | 575 |
| csv_gzip | 1.251 | 2 | 17.630 | 2.716 | 1.297 | 33.982 | 297 | 476 | 387 |
| parquet_raw | 1.793 | 2 | 18.103 | 3.195 | 0.681 | 35.511 | 165 | 144 | 159 |
| parquet_snappy | 1.230 | 2 | 19.724 | 3.705 | 1.051 | 39.145 | 222 | 229 | 431 |
| parquet_zstd | 0.690 | 2 | 18.607 | 3.794 | 1.056 | 37.465 | 138 | 197 | 230 |
| parquet_month | 1.248 | 48 | 24.673 | 5.104 | 1.430 | 44.771 | 308 | 246 | 529 |
| parquet_fetch100 | 1.230 | 2 | 16.977 | 3.111 | 0.811 | 36.405 | 137 | 153 | 229 |
| parquet_fetch10000 | 1.230 | 2 | 17.138 | 2.589 | 0.679 | 35.745 | 183 | 225 | 181 |

Les durees JDBC, conversion et depot MinIO se chevauchent : extract_write_ms mesure cette phase combinee. Catalogue signifie enregistrement/verification Trino, pas transfert S3 isole.
Le premier passage SQL est exclu ; aucune mesure ne pretend representer un cache froid. La requete janvier ajoute des contraintes redondantes annee/mois uniquement sur la table partitionnee.
Les octets sont les fichiers de donnees, hors manifeste et _SUCCESS. Les mesures brutes contiennent identifiants de jobs/requetes, SQL, repetitions, compteurs et environnement.


Les medianes des phases ne s'additionnent pas necessairement a celle du
pipeline, qui comprend aussi demarrage, surveillance et autres couts fixes.

## Interpretation

### Format et compression

Parquet sans compression utilise 1 880 314 octets contre 5 547 450 pour
CSV brut, soit 66,10 % de moins. Le format et ses encodages apportent ici
un gain avant meme d'appliquer un codec.

Parquet Zstd utilise 723 561 octets : 86,96 % de moins que CSV brut et
43,91 % de moins que Parquet Snappy. C'est le meilleur resultat de stockage
de cette campagne. Les tailles sont identiques sur les trois repetitions.

Les durees n'etablissent pas un classement universel : CSV gzip apparait
plus rapide a ingerer que CSV brut dans la mediane, mais les plages se
recouvrent. Le SQL global est presque identique entre CSV brut (159 ms)
et Parquet brut (165 ms) : pas d'acceleration systematique de Parquet
demontree sur chaque requete.

### Partitionnement

Les partitions mensuelles passent de 2 a 48 fichiers et augmentent la taille
de 1,49 %. La phase lecture/ecriture passe de 19,724 a 24,673 s (+25,09 %).
Le filtre explicite annee/mois reduit les lignes traitees par Trino de
100 000 a 4 247 (-95,753 %), sur toutes les mesures.

Pourtant, la latence mediane de janvier passe de 229 a 246 ms (+7,42 %) ;
le parcours global passe de 222 a 308 ms. L'elagage fonctionne sans gain
de latence sur ce petit jeu. La multiplication des petits fichiers et les
couts fixes sont une explication compatible avec ces observations, pas
une mesure isolee de leur cout CPU.

Les requetes comparent le meme resultat metier. La variante partitionnee
ajoute des contraintes redondantes pour permettre l'elagage ; le SQL exact
est conserve dans les preuves.

### Taille des lots

Les lots JDBC 100, 1000 et 10000 donnent exactement les memes 1 289 934
octets et deux fichiers en Parquet Snappy. Le nombre de lignes par lot
n'est pas le nombre de lignes par fichier.

Les medianes lecture/ecriture sont 16,977 s, 19,724 s et 17,138 s.
Leurs plages min-max respectives sont 16,555-19,240 s, 17,052-27,688 s
et 16,225-18,951 s. Elles ne permettent pas de generaliser que le lot 100
est meilleur pour toutes les bases ou tous les volumes. Les differences
SQL entre ces scenarios ne doivent pas etre attribuees au fetch JDBC :
les fichiers produits sont identiques en volume et structure.

## Limites et decisions

Ce test porte sur 100 000 factures synthetiques. Trois repetitions restent
un faible echantillon ; les caches ne sont pas purges. Les services habituels
restent actifs, et une construction courte du runner/audit a eu lieu pendant
la premiere serie. Il n'y a pas d'isolation CPU experimentale. Chaque job
demarre de nouveaux processus Spark, dont le cout est conserve.
Ce n'est pas un test a froid ni une preuve sur plusieurs teraoctets.

Pour ce jeu, Zstd est interessant pour reduire le stockage. Les donnees
non partitionnees restent raisonnables tant que le volume ne justifie pas
les petits fichiers supplementaires. Remesurer sur un volume plus grand
avant de generaliser. Aucun reglage de production n'a ete modifie
automatiquement sur la base de ces mesures.

## Preuves et reproduction

- [Mesures brutes et SQL](../benchmarks/results/local-2026-11-22-100k.json)
- [Tableau CSV](../benchmarks/results/local-2026-11-22-100k-summary.csv)
- [Resume JSON](../benchmarks/results/local-2026-11-22-100k-summary.json)
- [Commandes de reproduction et audit Docker](benchmarks.md)

Six tests du runner/audit passent dans Docker. La preparation a aussi ete
reexecutee avec Windows PowerShell : la fixture existante est verifiee sans
remplacement de lignes et les JSON sont ecrits sans BOM. Aucun secret n'est
inclus dans les livrables.
