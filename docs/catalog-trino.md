# Catalogue Hive et acces SQL Trino

Branche `feature/hive-catalog-trino`, base `feature/partitioning-csv`.
Enregistrement des nouvelles extractions, vues courantes et API SQL en lecture seule.

## Architecture et disponibilite

```text
SQL Server -> Spark -> Parquet/CSV -> MinIO
                                    |
                           manifeste verifie
                                    |
API Spring Boot -> Trino JDBC -> Hive Metastore -> PostgreSQL/metastore
                         |              |
                         +-- lecture SQL des fichiers MinIO
```

PostgreSQL conserve deux types de metadonnees dans des bases et roles distincts :
`atlas` pour les jobs, `metastore` pour Hive. Le metastore ne contient pas les
fichiers. Trino lit MinIO avec son implementation S3 native ; Spark utilise son
propre connecteur S3A. Le metastore dispose des connecteurs correspondant a son
Hadoop, independamment du Hadoop du writer Spark.

Versions choisies pour la validation : Apache Hive 4.0.1, Hadoop AWS 3.3.6,
PostgreSQL JDBC 42.7.8 ; Trino serveur/JDBC 477. Le bootstrap PostgreSQL
utilise le digest immuable de l'image 17.12 deja presente. Spark reste 4.3.0 avec Hadoop
3.5.0. Le couplage entre Spark et Trino porte sur Parquet/TEXTFILE et le schema,
pas sur leurs versions Java/Hadoop. Hadoop 3.3.6 a ete confirme dans le conteneur
Hive ; les tests ont relu les fichiers Parquet et CSV avec Trino.

## Installation

Ajouter a `.env` les variables de `.env.example` : METASTORE_PASSWORD,
TRINO_S3_ACCESS_KEY/SECRET_KEY, TRINO_REGISTRAR_PASSWORD, TRINO_READER_PASSWORD,
TRINO_KEYSTORE_PASSWORD et TRINO_SHARED_SECRET. Choisir des secrets distincts, d'au moins 12 caracteres.
Ne pas modifier les mots de passe deja enregistres dans les volumes existants.

```powershell
docker compose up -d --build
docker compose --profile tools run --rm backend-tests
```

Le service `metastore-db-init` execute le script idempotent
`infra/postgres/02-metastore.sh` meme sur un volume PostgreSQL existant. Sur un
volume neuf, le meme script est aussi execute par l'entrypoint PostgreSQL.
Il cree le role non administrateur et sa base sans supprimer les anciennes
bases. Le metastore verifie son schema avant son initialisation. Aucun reseed
SQL Server ni remplacement de volume n'est necessaire.

Le compte MinIO dedie au catalogue possede seulement GetObject, ListBucket et
GetBucketLocation. Il est distinct du compte d'ingestion. Le bootstrap reste
seul a recevoir les identifiants administrateurs MinIO. Les proprietes contenant
des secrets sont generees au demarrage, jamais ajoutees a l'image ou au depot.

## Tables par execution et vue courante

Chaque nouveau job est enregistre comme table externe :
`hive.runs.invoice_<8 premiers caracteres UUID>`, emplacement
`s3://lake/sqlserver/dbo/Invoice/<UUID>/data/`. Aucun fichier n'est deplace ni
ecrase. Les identifiants SQL sont en minuscules ; les types sont explicitement
mappes, dont DECIMAL(p,s), DATE, TIMESTAMP(3), VARCHAR, INTEGER et BIGINT.

Parquet utilise PARQUET. CSV utilise TEXTFILE, virgule et NULL `\N`, selon le
contrat documente dans storage-jobs.md. Les colonnes de partition sont placees
a la fin de la definition. Pour une table partitionnee, le backend appelle
`hive.system.sync_partition_metadata(...,'FULL')`, puis verifie sa lecture et
son nombre de lignes. Hive associe le schema URI s3 au S3AFileSystem Hadoop.
La verification du chemin accepte la suppression du slash final par Trino,
sans accepter un autre prefixe.

Pour Parquet, `hive.sqlserver_dbo.invoice` est une vue remplacee atomiquement
vers la nouvelle table. Elle expose les colonnes sources, en conservant les
dates originales et les colonnes sources partitionnees directement ; les
colonnes date derivees sont exclues. Les tables historiques restent consultables.
Une nouvelle extraction CSV ne remplace pas la vue Parquet. Un reenregistrement
d'une ancienne execution ne doit pas faire reculer la vue courante.

```text
QUEUED -> SUBMITTING -> RUNNING -> REGISTERING -> COMPLETED
               |           |           |
               +-----------+-----------+-----> FAILED
```

Le manifeste valide est persiste avant REGISTERING. COMPLETED suit seulement
la verification SQL et la publication de la vue. Une panne PostgreSQL apres
publication laisse REGISTERING pour une reprise idempotente. Une erreur de
catalogue retourne `CATALOG_REGISTRATION_FAILED` ; les fichiers restent dans
MinIO. Le code d'erreur est persiste avant le nettoyage des metadonnees. Si
Trino est indisponible pendant ce nettoyage, le job reste REGISTERING avec ce
code jusqu'au retrait confirme de sa table. Il ne devient FAILED qu'ensuite.
Une vue precedente est restauree si une publication interrompue la remplacait.
Les jobs COMPLETED anterieurs a cette
migration restent historiques, sans table catalogue : creer une nouvelle
extraction pour disposer d'une table SQL.

## API SQL

Creer une extraction pour chacune des tables Customer, Invoice et Payment et
attendre COMPLETED pour disposer des trois vues utilisees dans une jointure.
Le depot de fichiers preexistant ne cree pas automatiquement ces vues.

- `GET /catalog/tables` : executions enregistrees et terminees, format,
  emplacement, partitions, compteurs et lien vers la vue courante.
- `POST /sql/query` : `{ "sql": "SELECT * FROM hive.sqlserver_dbo.invoice", "max_rows": 1000 }`.
- `GET /storage/jobs/{id}/result` : ajoute catalog_table, current_view,
  registration_ms et example_query.

La reponse SQL contient columns (nom/type), rows, row_count, truncated,
execution_ms, query_id et stats Trino (elapsed_ms, CPU, lignes et octets lus).
Les resultats sont limites a 1000 lignes, le delai SQL a 60 secondes.
L'API accepte une seule instruction SELECT/WITH/SHOW/DESCRIBE ; les commentaires
SQL et les points-virgules internes sont refuses par sa validation conservative.
Les droits de lecture seule sont aussi appliques directement par Trino, avec
un utilisateur authentifie distinct du compte d'enregistrement. Aucune erreur
brute JDBC, donnee de connexion ou reponse contenant un secret n'est exposee.
Les statistiques d'une requete tronquee peuvent refleter une execution annulee
avant la fin ; execution_ms correspond au temps mesure par l'API.

Trino utilise HTTPS sur https://localhost:8089 ; le certificat local auto-signe
est genere au demarrage. Le JDBC local utilise SSLVerification=NONE : en cloud,
installer un certificat reconnu et activer la verification complete. L'acces
HTTP interne de decouverte n'est pas publie sur Windows. Les identites utilisent
des mots de passe PBKDF2 ; le compte lecteur ne peut pas devenir le registrar.

CLI apres demarrage (saisie du mot de passe lecteur) :

```powershell
docker compose exec trino trino --server https://localhost:8443 --insecure --user atlas_reader --password
```

## Tests et ressources

Les tests de catalogue utilisent les schemas `runs_test` et
`sqlserver_dbo_test`, ainsi que la base de jobs `atlas_test`. Ils nettoient
uniquement leurs tables/vues et prefixes UUID. Ils comparent count,
SUM(amount) et une jointure Customer/Invoice/Payment avec SQL Server ; verifier
l'idempotence, la vue la plus recente, les echecs et les droits directement
via JDBC en contournant le validateur API.

Le heap Trino est borne a 2 Go et celui du metastore a 512 Mo. Il faut aussi
compter SQL Server, les JVM Spark, l'API, MinIO, PostgreSQL et les compilations.
Le poste de validation dispose actuellement d'environ 8 Go alloues a Docker ;
la stack complete a ete executee avec cette allocation pour le profil small.
Prevoir 8 Go alloues a Docker, en plus de la memoire utilisee par Windows.
Les heaps cumules, les fichiers caches et les builds concurrents demandent
davantage de marge pour le profil large ; 12 Go est un budget de depart
estime, pas une mesure de consommation du benchmark. Le disque doit contenir les images decompressees,
les caches de compilation et les volumes ; l'espace temporaire requis varie
selon les images deja presentes.

References : [Hive Docker](https://hive.apache.org/docs/latest/admin/setting-up-hive-with-docker/),
[controle d'acces Trino](https://trinodb.github.io/docs.trino.io/477/security/file-system-access-control.html),
[stockage objet Trino 477](https://trinodb.github.io/docs.trino.io/477/object-storage.html).

La procedure Trino sync_partition_metadata en mode FULL controle les privileges
INSERT et DELETE sur la table. Le registrar les possede pour cet usage de
metadonnees, en plus de SELECT/OWNERSHIP ; le lecteur conserve seulement SELECT.
Les ecritures physiques sur les tables externes restent desactivees dans le
connecteur et la cle MinIO reste en lecture seule. Le delai d'attente des tests
Spark est de 600 s par job pour couvrir un demarrage froid sur la stack complete ;
il ne modifie pas le delai SQL de 60 s ni le delai applicatif des jobs.

## Validation du 21 novembre 2026

54 tests backend distincts ont ete valides par Docker, sans integration ignoree :

| Controle | Resultat |
| --- | --- |
| Suite complete avant corrections finales | 53 executes, 48 reussis, 5 echecs identifies |
| Relance des 5 cas en echec apres corrections | 5 reussis, aucun echec |
| Verification finale des droits et du nettoyage | 3 reussis, dont le nouveau test MinIO ; les 2 autres reconfirment des cas precedents |

Les echecs initiaux portaient sur une connexion Spark interrompue, le delai de
son test et les permissions de synchronisation FULL. Les premiers essais de
mise en route avaient egalement revele les permissions du repertoire de
configuration Hive et l'association du schema URI s3 au S3AFileSystem.
Ces controles ont ete repris apres correction ; aucun resultat n'est invente.

Les tests ont verifie : 1000 Customer, 5000 Invoice, 8000 Payment ; count et
SUM(amount) identiques a SQL Server ; jointure par ville identique ; vue courante
la plus recente, reenregistrement idempotent, Parquet/zstd, CSV gzip et brut,
24 partitions mensuelles et suppression des metadonnees d'un job en echec.
Les refus de CREATE SCHEMA et de procedure pour atlas_reader sont verifies
par JDBC direct. La cle MinIO distincte lit les objets et recoit AccessDenied
pour PutObject et DeleteObject.

Un lancement par le proxy /api a termine en COMPLETED et cree
`hive.runs.invoice_0e1ac517` avec la vue `hive.sqlserver_dbo.invoice` :
5000 lignes, SUM(amount)=4892425.00, registration_ms=5541.
Il s'agit d'une verification fonctionnelle unique, pas d'un benchmark.
Le bootstrap PostgreSQL a ete relance avec succes sur le volume existant.
