# Extraction ClickHouse vers le lac

Branche `feature/clickhouse-extraction`, base `feature/unified-source-list`.

Depuis la liste commune, **Explorer** la source `clickhouse-demo`, puis choisir
**Extraire invoice en Parquet** (ou une autre table compatible). L'interface
cree un job persistant et ouvre son suivi. Seuls les profils configures cote
serveur sont extractibles ; une connexion temporaire du formulaire ne transmet
pas ses identifiants aux workers et ne propose pas ce bouton.

```json
{
  "source_id": "clickhouse", "schema": "analytics", "table": "invoice",
  "format": "parquet", "compression": "snappy", "partition_by": [],
  "fetch_size": 1000, "num_partitions": 1
}
```

L'API reste `POST /storage/jobs`, puis les endpoints habituels de suivi et de
resultat. Le serveur valide la structure et les types avant de creer le job.
Le profil natif expose `extraction_source_id` pour annoncer cette capacite.

## Lecture et qualite

Les classes Java partagees `features/nativeingestion/{connector,dto}` sont
utilisees par l'API et Spark. Leur configuration Spring reste dans `config`.
Elles ne dependent pas de la couche web. Les identifiants viennent de
l'environnement du worker, jamais des arguments Spark ou du navigateur.

Un lecteur Spark execute un SELECT ClickHouse via son interface HTTP, puis
consomme JSONEachRow en flux. Les lignes sont converties par lots bornes par
`fetch_size` et un budget de 8 Mio environ, avec une limite de 4 Mio par ligne
JSON. La table complete n'est pas chargee dans la memoire du driver.
Le resultat suit le meme parcours : Parquet/CSV -> MinIO -> manifeste verifie
-> Hive -> Trino. Le job n'est termine qu'apres verification du compte Trino.

Les entiers non signes sont elargis sans perte : `UInt64` devient
`decimal(20,0)`. Les decimaux gardent precision et echelle (maximum 38 chiffres),
les NULL sont conserves, les dates restent des dates et DateTime est normalise
en UTC. Nullable et LowCardinality sont deplies. Les types Array, Map, Tuple,
Int128/256, Decimal de precision superieure a 38 et DateTime64 sont refuses
avant la soumission plutot que convertis silencieusement en texte.

Cette premiere version utilise un seul lecteur de source. `num_partitions`
n'ajoute pas de lectures paralleles, et le manifeste indique ce comportement.
Le partitionnement natif des fichiers est refuse explicitement. Le support
JDBC SQL Server/PostgreSQL conserve ses options precedentes. Il ne s'agit pas
d'un snapshot transactionnel coordonne avec les autres sources.

L'API accepte Parquet (snappy/zstd/uncompressed) et CSV (gzip/uncompressed),
avec les restrictions CSV existantes pour les separateurs et sauts de ligne.
Le bouton de l'interface choisit Parquet Snappy sans partitionnement.

## Docker et verification

Utiliser les six fichiers Compose du [guide ClickHouse](redis-clickhouse-exploration.md).
La nouvelle image Spark ajoute les bibliotheques natives dans un POM distinct.
Reconstruire `backend`, `frontend`, `spark-master` et `spark-worker`, puis
redemarrer les services mis a jour lorsqu'aucun job n'est actif. Conserver
les volumes et ne pas relancer les seeds SQL Server ou Redis.

Le service optionnel `native-ingestion-check` execute un vrai job et compare
les comptes, les sommes exactes, les NULL et les dates entre la source et Trino :

```powershell
docker compose @compose --profile tools run --rm --no-deps native-ingestion-check --source=clickhouse --table=invoice
docker compose @compose --profile tools run --rm --no-deps native-ingestion-check --source=clickhouse --table=customer
```

Les rapports locaux dans `work/native-ingestion` conservent l'identifiant du
job pour ne jamais en soumettre un second apres une reponse incertaine. Les
erreurs de soumission de l'interface demandent aussi de verifier Storage Jobs
avant toute relance. Les secrets ne sont pas enregistres dans ces rapports.

## Resultats locaux du 26 novembre 2026

29 tests backend cibles passent sur les services Docker ; le build backend
execute aussi 62 tests sans infrastructure (54 integrations desactivees pendant
ce build). Les controles de conversion Spark passent, notamment le maximum
UInt64 et les depassements de precision decimale.

| Scenario | Lignes relues dans Trino | Reconciliation |
|---|---:|---|
| invoice Parquet Snappy | 5 000 | Somme 129 972,16 ; somme des IDs 12 502 500 ; dates min/max identiques |
| customer Parquet Snappy | 1 000 | 100 emails NULL ; somme des IDs 500 500 ; horodatage minimum identique |
| invoice CSV gzip | 5 000 | Memes comptes, montants, IDs et dates que la source |

Les [preuves de controle](../validation/results/clickhouse-extraction-2026-11-26.json)
conservent les identifiants des trois jobs et leurs resultats. Les 45 anciens
jobs ne sont pas modifies. Ces controles comparent des agregats, pas un checksum
integral ni un snapshot transactionnel commun a plusieurs systemes.
