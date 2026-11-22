# Benchmarks reproductibles

Branche `feature/reproducible-benchmarks`, base `feature/native-source-interface`.
Ce runner Docker mesure les API et images existantes ; il ne remplace pas le
moteur d'ingestion et n'ajoute pas de nouveaux endpoints backend.

## Protocole

Le jeu SQL Server `BusinessBenchmark.benchmark.Invoice100k` contient les
100 000 premieres factures du generateur large. La source et le schema
`benchmark` evitent de remplacer les vues courantes SQL Server/PostgreSQL du
projet. Le script refuse une table existante differente et ne fait aucun
TRUNCATE, DROP ou reseed. Le compte lake_reader recoit SELECT sur cette seule
table supplementaire. Les tables dbo.Customer/Invoice/Payment sont conservees.

La reference est calculee dans SQL Server : compte, somme des montants,
somme des identifiants, dates extremes, total de janvier 2025 et groupes de
statut. Chaque requete Trino verifie les comptes et sommes attendus.
Ces invariants ne sont pas un checksum cryptographique de toutes les cellules.

Huit scenarios, trois extractions chacun :

| Scenario | Format | Compression | Partition | Fetch JDBC |
| --- | --- | --- | --- | ---: |
| csv_raw | CSV | aucune | aucune | 1000 |
| csv_gzip | CSV | gzip | aucune | 1000 |
| parquet_raw | Parquet | aucune | aucune | 1000 |
| parquet_snappy | Parquet | snappy | aucune | 1000 |
| parquet_zstd | Parquet | zstd | aucune | 1000 |
| parquet_month | Parquet | snappy | annee/mois invoice_date | 1000 |
| parquet_fetch100 | Parquet | snappy | aucune | 100 |
| parquet_fetch10000 | Parquet | snappy | aucune | 10000 |

Deux lecteurs JDBC demandes, un coeur executeur, heap driver/executeur 512 Mio,
et maxRecordsPerFile=100000 restent fixes. Modifier fetch_size change la
lecture JDBC, pas directement la taille des fichiers. Les lecteurs peuvent
attendre le coeur disponible. Les scenarios sont decales entre repetitions
pour repartir partiellement les effets d'ordre ; ce n'est pas une randomisation.

Une extraction Parquet Snappy d'echauffement est exclue des statistiques.
Chaque extraction demarre cependant un nouveau driver/executeur Spark :
l'echauffement ne supprime pas le cout de demarrage ni le JIT de ces nouveaux
processus. Ce cout reste compris dans le pipeline serveur mesure.
Chaque extraction mesuree execute trois requetes : agregation globale,
agregation sur janvier 2025 et agregation par statut. Chaque requete fait
un passage d'echauffement exclu, puis trois passages mesures. La mediane SQL
utilise donc neuf mesures par scenario ; l'ingestion trois mesures.

Pour janvier, toutes les tables utilisent le meme filtre invoice_date.
La variante partitionnee ajoute invoice_date_year=2025 et
invoice_date_month=1, contraintes redondantes equivalentes sur ces donnees.
Elles rendent l'elagage des partitions explicite ; un filtre sur la date
d'origine seul ne garantit pas l'elagage de colonnes derivees. Le SQL exact
est conserve dans les resultats. Les donnees et resultats metier compares
sont identiques, mais les predicats physiques ne le sont pas.

## Durees et tailles

- extract_write_ms : lecture JDBC, conversion et ecriture MinIO en flux.
  Ces phases se chevauchent ; elles ne sont pas chronometrees separement.
- verify_ms : relecture de controle Spark, compte et inventaire des fichiers.
- registration_ms / catalog_load_ms : enregistrement dans Hive/Trino,
  synchronisation des partitions et verification SQL. C'est le chargement
  dans le catalogue SQL, pas un temps isole de transfert vers MinIO.
- server_pipeline_ms : du debut serveur a COMPLETED, incluant demarrage
  Spark, surveillance et catalogue. api_wall_ms va de l'horodatage de
  soumission client a finished_at serveur, incluant la file/soumission mais
  excluant le delai de detection du polling. Les horloges Docker sont communes.
- execution_ms : duree SQL mesuree par l'API, incluant connexion/planification
  et lecture du resultat. client_ms inclut l'aller-retour HTTP du runner.
- storage_bytes : fichiers de donnees uniquement ; manifeste et _SUCCESS
  exclus. processed_bytes est le compteur de traitement expose par Trino,
  pas une mesure des E/S physiques du disque MinIO.

Aucun cache n'est purge : le premier passage n'est pas appele cache froid.
Les caches du systeme, JIT, metastore et ressources partagees influencent
les mesures. Les services habituels restent actifs ; la campagne refuse
un autre job actif au depart, sans verrouiller l'API contre d'autres clients.
Ne pas utiliser l'interface pour lancer une autre extraction pendant la campagne.

## Reproduire dans Docker

Prerequis : stack SQL Server/MinIO/Hive/Trino/Spark saine, plugins et overrides
PostgreSQL/Redis deja lances, et base large BusinessBenchmark deja presente.
Pour une installation neuve seulement, la creer sans toucher a BusinessDB :

```powershell
docker compose run --rm --no-deps sqlserver-init 'bash /scripts/measure-large.sh'
```

Cette commande refuse une base BusinessBenchmark existante. Ensuite :

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml build benchmark
powershell -NoProfile -File benchmarks/prepare.ps1
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml up -d --no-deps --wait spark-worker backend
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml --profile tools run --rm --no-deps benchmark --run-id=ma-campagne
```

Ne pas recreer les workers pendant un job actif. Attendre le master ALIVE
et le worker sain ; le runner verifie ce point. Ne pas reutiliser un run-id
existant sauf pour reprendre sa campagne :

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml --profile tools run --rm --no-deps benchmark --run-id=ma-campagne --resume
```

Le runner persiste l'identifiant de chaque job avant de le suivre. Une
soumission HTTP incertaine n'est jamais retentee automatiquement, meme lors
d'une reprise. Inspecter Spark et l'historique pour reconcilier l'identifiant
avant de reprendre. Un job FAILED arrete la campagne ; il n'est pas relance.
Les fichiers, jobs et tables de chaque execution restent conserves pour audit.

## Livrables et tests

`benchmarks/results/<run-id>.json` contient les demandes, manifestes, identifiants
de jobs/requetes, SQL exact, resultats d'integrite et repetitions. Les fichiers
`-summary.csv`, `-summary.json` et `-table.md` sont derives de ces preuves.
Les fixtures contiennent la reference SQL et l'environnement enregistre.
Les secrets restent dans .env et les conteneurs applicatifs ; le runner n'en
recoit aucun. Aucun Java, Maven, Node ou Python hote n'est requis.

Apres la campagne, auditer toutes les preuves avec l'image finale :

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml --profile tools run --rm --no-deps --entrypoint node benchmark audit.mjs /results/ma-campagne.json
```

Cet audit exige 24 jobs distincts, trois repetitions completes par scenario,
les bons parametres dans les manifestes, 288 identifiants de requetes uniques
(dont 216 mesures et 72 echauffements), des resultats conformes aux references,
et l'etat FINISHED des requetes Trino.

Les tableaux peuvent etre regeneres sans aucune nouvelle extraction, apres
le meme audit : remplacer `audit.mjs` par `render.mjs` dans cette commande.

Le build de l'image execute les tests de matrice controlee, ordre des repetitions,
validation d'identifiants SQL, comptes/sommes/groupes, detection de troncature,
medianes et refus des scenarios incomplets.
