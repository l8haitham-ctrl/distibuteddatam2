# Extraction Redis : un hash par ligne

Branche `feature/redis-hash-extraction`, base `feature/clickhouse-extraction`.

Depuis la liste commune, explorer `redis-demo`, puis choisir
**Extraire customer_segments en Parquet**. L'interface cree un job et ouvre son
suivi habituel. Les 100 cles de l'explorateur ne sont qu'un echantillon : le
traitement parcourt l'ensemble du prefixe configure `analytics:customer_segments:`.

```json
{
  "source_id": "redis", "schema": "redis_0", "table": "customer_segments",
  "format": "parquet", "compression": "snappy", "fetch_size": 1000,
  "partition_by": [], "num_partitions": 1
}
```

## Mapping et controles

| Colonne du lac | Origine | Type |
|---|---|---|
| _key | Cle Redis complete | string non NULL |
| customer_id | Champ du hash | string, NULL si absent |
| segment | Champ du hash | string, NULL si absent |
| loyalty_points | Champ du hash | string, NULL si absent |

Les valeurs Redis restent du texte : pas d'inference risquant de perdre des
zeros initiaux ou de modifier leur sens. Pour des agregats numeriques dans
Trino, utiliser par exemple `SUM(CAST(loyalty_points AS BIGINT))`.

Le mapping est explicite cote serveur, jamais deduit du seul echantillon de
l'explorateur. Les options d'environnement sont `REDIS_DATABASE` (0..15),
`REDIS_EXTRACTION_DATASET`, `REDIS_EXTRACTION_PREFIX`, `REDIS_EXTRACTION_FIELDS`
(noms separes par des virgules), et les parametres de transport Redis. Si vous
les modifiez dans un deploiement, les fournir a l'API **et** au worker. Le
compte lecteur doit aussi avoir les droits sur ce prefixe. La demonstration
Compose conserve ses valeurs precedentes et ne change aucune ACL ni aucune cle.

Seuls les hashes sont pris en charge par cette extraction. Une cle d'un autre
type dans le prefixe, un champ non mappe, un nom incompatible avec le catalogue,
un encodage non UTF-8 ou une ligne de plus de 4 Mio provoquent une erreur, sans
publication de table analytique. Les champs manquants donnent NULL. Les noms
dupliques sans distinction de casse et le nom reserve `_key` sont refuses.

SCAN est utilise, jamais KEYS. Les commandes binaires permettent de verifier
strictement UTF-8. Les hashes sont lus en flux dans les lots bornes du lecteur
natif. Les doublons de cles possibles avec SCAN sont supprimes par un shuffle
Spark sur `_key`, et non par un ensemble de toutes les cles en memoire du driver.

Redis ne fournit pas ici de snapshot : les cles expirees pendant la lecture
peuvent disparaitre, et des modifications concurrentes peuvent modifier le
resultat. La deduplication ne garantit pas de retenir la version la plus recente.
Cette version standalone n'implemente ni Redis Cluster ni chargement incremental.
Elle utilise un lecteur de source et ne partitionne pas les fichiers.

## Architecture et lancement

`RedisHashExtraction` complete `features/nativeingestion/connector`. Les classes
de lecture et le mapping sont partages entre l'API et le jar Spark. Le compte
lecteur provient de l'environnement, sans secret dans les arguments Spark ou
dans les requetes du navigateur. Le worker rejoint le reseau Docker interne
Redis ; aucun port Redis public n'est ajoute.

Conserver les six fichiers Compose du [guide ClickHouse](redis-clickhouse-exploration.md),
reconstruire backend/frontend/Spark, puis mettre a jour ces services lorsqu'aucun
job n'est actif. Les volumes et les seeds existants sont conserves.

Le parcours reste : Redis -> Spark -> Parquet ou CSV -> MinIO -> Hive -> Trino.
Les resultats donnent la table SQL et, pour Parquet, la vue courante
`hive.redis_redis_0.customer_segments`.

```powershell
docker compose @compose --profile tools run --rm --no-deps native-ingestion-check --source=redis --table=customer_segments
docker compose @compose --profile tools run --rm --no-deps native-ingestion-check --source=redis --table=customer_segments --format=csv
```

Le controle compare les comptes, les sommes et la distribution des segments
avec le catalogue Redis direct de Trino, puis avec le lac. Il attend pour la
fixture 1 000 cles uniques, une somme des IDs de 500 500, des points de fidelite
de 5 005 000 et 200 clients VIP / 800 standard. Les rapports dans
`work/native-ingestion` conservent les IDs des jobs pour eviter les doublons
de soumission lors d'une relance du controle.

## Verification locale du 26 novembre 2026

Les deux formats ont ete relus dans Trino avec les memes agregats que Redis :

| Format | Lignes / cles uniques | Octets stockes | Extraction + ecriture |
|---|---:|---:|---:|
| Parquet Snappy | 1 000 / 1 000 | 13 629 | 26 246 ms |
| CSV gzip | 1 000 / 1 000 | 7 335 | 18 422 ms |

Ces deux petits essais ne constituent pas un benchmark general des formats.
Les sommes sont de 500 500 pour les IDs et 5 005 000 pour les points, avec
200 VIP et 800 standard. La reconciliation porte sur ces agregats, pas sur
un checksum integral ou un snapshot transactionnel.

Le job Parquet a ete declenche depuis l'interface, puis interroge depuis la
console SQL : [resultat du job](evidence/redis-extraction-completed.jpg) et
[resultat SQL](evidence/redis-extraction-sql.jpg).
Les [preuves des deux traitements](../validation/results/redis-hash-extraction-2026-11-26.json)
conservent les IDs et les mesures. Les 24 tests backend cibles Docker passent ;
le build backend passe 66 tests (56 integrations desactivees lors du build).
Les 67 tests Angular, le build de production et les controles Spark passent.
