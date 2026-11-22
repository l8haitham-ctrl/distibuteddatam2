# Test analytique Redis et MinIO

Branche `feature/redis-analytics`, base `feature/connector-aware-frontend`.
Cette extension optionnelle utilise le connecteur Redis natif de Trino 477.
Elle ne change pas le contrat JDBC relationnel ni les jobs Spark.

## Deux parcours

```text
SQL Server / PostgreSQL -> Spark -> Parquet -> MinIO -> Hive Metastore -> Trino
Redis (hashes de segments clients) ----------------------------------> Trino
                                                            |
                                                  API POST /sql/query
```

Redis est un stockage cle/valeur, MinIO un stockage objet. Le moteur SQL
analytique est Trino. MinIO est deja connecte via le catalogue Hive ; deposer
un fichier seul ne cree pas de table. Les extractions existantes enregistrent
leurs tables et vues dans le catalogue.

La demo Redis contient 1 000 hashes `analytics:customer_segments:<id>`.
Chaque hash comporte customer_id, segment et loyalty_points. Le fichier
de description Trino les expose comme `redis.analytics.customer_segments`.
Il y a 200 clients vip et 800 standard ; leurs identifiants correspondent
aux clients du jeu PostgreSQL. Redis n'apparait pas dans GET /connectors,
qui decrit les sources d'extraction JDBC, ni dans le formulaire relationnel.
L'acces Redis se fait via les requetes SQL, sans extraction Parquet Redis.

## Lancer localement

Conserver les secrets existants. Ajouter a `.env` deux valeurs aleatoires
distinctes d'au moins 16 caracteres pour REDIS_ADMIN_PASSWORD et
REDIS_READER_PASSWORD, en suivant `.env.example`. Ne jamais committer `.env`.

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml build trino
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml up -d --no-deps --wait redis-source
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml up --no-deps --abort-on-container-exit --exit-code-from redis-seed redis-seed
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml up -d --no-deps --wait trino
```

Ces commandes supposent la plateforme deja lancee et une extraction Parquet
PostgreSQL invoice enregistree sous `hive.postgresql_public.invoice`, avec
les 5 000 factures du jeu de demonstration. Voir [le guide PostgreSQL](postgresql-connector.md).
Attendre la fin des jobs actifs avant de remplacer Trino. L'override ne
redemarre pas Spark et ne modifie ni les volumes SQL Server/MinIO ni le metastore.
Utiliser les trois fichiers Compose pour conserver les deux sources et Redis.

## Requetes

Dans Swagger POST /sql/query ou dans le client SQL Trino :

```sql
SELECT segment, COUNT(*) AS customers, SUM(loyalty_points) AS points
FROM redis.analytics.customer_segments
GROUP BY segment;

SELECT COUNT(*) AS invoices, SUM(amount) AS total
FROM hive.postgresql_public.invoice;

SELECT r.segment, COUNT(*) AS invoices, SUM(i.amount) AS revenue
FROM hive.postgresql_public.invoice i
JOIN redis.analytics.customer_segments r ON i.customer_id = r.customer_id
GROUP BY r.segment
ORDER BY r.segment;
```

L'API prend un objet `{"sql":"SELECT ...","max_rows":100}`. Les noms
de catalogue doivent etre qualifies : redis.analytics et hive.postgresql_public.
GET /catalog/tables liste les extractions gerees par la plateforme, pas les
tables Redis ; utiliser SHOW TABLES FROM redis.analytics pour les decouvrir.

## Tests Docker

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml exec -T redis-source sh /fixtures/check-acl.sh
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml --profile tools run --rm --no-deps backend-tests mvn -B test '-Dtest=RedisAnalyticsIntegrationTests,CatalogTests'
```

Les tests exigent 1 000 segments Redis, 5 000 factures MinIO,
SUM(amount)=1 750 250, et une jointure sans perte ni multiplication de lignes.
Ils verifient aussi le refus des ecritures et l'absence d'acces Redis pour
le compte de service d'enregistrement Hive. Les commandes ACL testent le
refus des ecritures Redis, des cles hors perimetre et de l'acces anonyme.

## Limites de cette demo

Le compte Trino Redis est lecteur avec un prefixe de cles autorise. Le
catalogue n'est pas accessible au compte d'enregistrement Hive. Aucun port
Redis n'est publie ; un reseau Docker interne relie uniquement Redis,
son initialiseur et Trino. La version 477 installee n'expose pas d'option
TLS Redis : cette demo est strictement locale. Un deploiement distant
necessite un connecteur avec TLS verifie, pas la publication de ce port.

Le jeu Redis est ephemere, sans volume persistant et sans expiration des cles ;
relancer redis-seed apres une recreation de redis-source. Le script de seed
ecrit uniquement ses propres 1 000 hashes et n'utilise jamais FLUSHDB.
Le schema des hashes est declare explicitement et ne constitue pas une
decouverte automatique de tout Redis. Les requetes lisent des donnees vivantes,
sans snapshot transactionnel commun avec les fichiers Parquet ; ce test
fonctionnel ne constitue pas un benchmark ni un support de Redis Cluster.

Reference : [connecteur Redis officiel Trino](https://trino.io/docs/current/connector/redis.html).
Les options de cette demo sont validees sur le plugin 477 installe ; les
options TLS de versions plus recentes ne sont pas utilisees.

Validation locale : 8 tests Docker passes (5 integrations Redis/MinIO et
3 tests catalogue). La requete API de jointure retourne standard : 4 000
factures / 1 400 000, et vip : 1 000 factures / 350 250. Le controle ACL passe.
