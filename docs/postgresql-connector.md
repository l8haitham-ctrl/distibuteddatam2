# PostgreSQL comme source metier

Branche `feature/postgresql-connector`, base `feature/jdbc-connector-plugins`.
Ce connecteur est une implementation de la SPI existante ; aucun nouveau
controleur et aucun chemin PostgreSQL specifique dans le driver d'ingestion
ne sont necessaires. Le PostgreSQL metier est distinct du PostgreSQL qui
conserve les jobs et le metastore.

## Installation locale optionnelle

Definir PG_SOURCE_ADMIN_PASSWORD et PG_SOURCE_READER_PASSWORD dans `.env`, avec
deux secrets distincts. Les nouveaux volumes conservent ces valeurs lors de
l'initialisation ; changer `.env` ne change pas les mots de passe deja crees.

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml up -d --build
```

L'override ajoute une base `business` avec 1 000 clients, 5 000 factures et
8 000 paiements, un compte atlas_reader avec SELECT uniquement et un certificat
TLS local genere au demarrage dans un volume. Aucun secret ni cle privee n'est
inclus dans l'image ou dans Git. Le serveur n'est pas expose sur un port hote ;
seuls les services du reseau Compose l'utilisent.

`SOURCE_IDS=sqlserver,postgresql` est applique au backend, au worker et aux tests.
Le plugin PostgreSQL impose TLS ; la verification du certificat est active
par defaut. L'override accepte explicitement le certificat auto-signe de cette
demonstration. Un PostgreSQL externe doit avoir un certificat reconnu ou une
configuration de confiance administree adaptee.

## API identique

- GET /connectors : sqlserver et postgresql, ports 1433/5432 et schemas dbo/public.
- GET /sources : les deux sources configurees sans mots de passe.
- POST /sources/postgresql/test : test de connexion.
- GET /sources/postgresql/tables : tables via les metadonnees JDBC.
- GET /sources/postgresql/tables/invoice : schema public resolu par le plugin.
- GET /storage/jobs/partition-candidates?source_id=postgresql&table=invoice.

```json
{
  "source_id": "postgresql",
  "table": "invoice",
  "format": "parquet",
  "compression": "snappy",
  "partition_by": ["invoice_date"],
  "fetch_size": 257,
  "num_partitions": 2
}
```

Le backend normalise le schema public avant de persister la demande.
Spark charge le meme plugin, lit par JDBC et ecrit le manifeste avec
source_engine=postgresql. Le catalogue cree une table par execution et une
vue Parquet `hive.postgresql_public.invoice`, sans remplacer la vue SQL Server.
Le CSV utilise le contrat TEXTFILE deja documente et ne remplace pas la vue
Parquet. Les autres moteurs exigeront leur fournisseur et leur pilote.

## Tests

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml --profile tools run --rm --no-deps backend-tests mvn -B test '-Dtest=PostgresqlConnectorTests,PostgresqlIntegrationTests,ConnectionIntegrationTests,DatabaseIntegrationTests'
```

Les tests couvrent la coexistence des moteurs, les schemas par defaut, types et
precisions, longueurs de caracteres, cles et relations, caracteres joker JDBC,
cardinalite, TLS, privileges SELECT sans ecriture et erreurs sans secrets.

References primaires : [proprietes pgJDBC](https://jdbc.postgresql.org/documentation/use/),
[TLS pgJDBC](https://jdbc.postgresql.org/documentation/ssl/) et
[lecteur JDBC Spark](https://spark.apache.org/docs/latest/sql-data-sources-jdbc.html).

Validation locale : 37 tests cibles passes avec SQL Server et PostgreSQL reels ;
build Docker avec 34 tests unitaires passes (28 tests d'integration exclus de
ce build, executes separement pour les scenarios cibles).
Les extractions PostgreSQL `d6b15a93-6e54-4fd7-bd10-c6e0c3aa2a21`
(Parquet Snappy par mois) et `dfc4c206-0f8f-4ee7-93bf-a682e8461787`
(CSV gzip par statut) sont COMPLETED. Trino relit dans chaque table
5 000 factures et SUM(amount)=1 750 250. Ces controles ne sont pas des benchmarks.
