# Connexion et exploration Redis / MinIO

Branche `feature/native-source-connections`, base `feature/redis-analytics`.
Les sources natives utilisent leur protocole et une interface NativeConnector
distincte de la SPI JDBC. Les implementations sont dans
`features/nativesources/{api,service,dto,connector}`. Les controleurs deleguent
aux services ; les connecteurs ne dependent pas de HTTP.

## API

- GET /native/connectors : fournisseurs et capacites connexion/exploration.
- POST /native/connections/test : tester une configuration saisie.
- POST /native/connections/explore : explorer une configuration saisie.
- GET /native/sources : profils de demonstration sans secrets.
- POST /native/sources/{id}/test : tester un profil configure cote serveur.
- POST /native/sources/{id}/explore : explorer un profil configure cote serveur.

```json
{
  "engine": "redis",
  "host": "redis-source",
  "port": 6379,
  "namespace": "0",
  "username": "atlas_reader",
  "password": "<mot de passe lecteur>",
  "tls": false,
  "prefix": "analytics:customer_segments:"
}
```

Pour MinIO : engine=minio, host=minio, port=9000, namespace=lake,
username=cle d'acces, password=cle secrete, prefix=prefixe de fichiers.
Les hotes sont resolus depuis le conteneur backend. TLS est actif et verifie
par defaut, sans option de contournement du certificat. Les profils Docker
locaux existants utilisent explicitement le transport non chiffre sur les
reseaux internes. Ne pas utiliser ce choix pour une connexion distante.

Le profil redis-demo existe seulement si REDIS_READER_PASSWORD est configure.
minio-lake reprend la configuration storage du backend. Les secrets ne sont
ni renvoyes dans GET /native/sources, ni persistants dans une nouvelle table.
Les erreurs ne renvoient pas les messages des pilotes ou les identifiants.

## Structure renvoyee

Redis expose un echantillon de 100 cles maximum, leur type, et au plus
20 noms de champs par hash (Redis >=6.2 pour HRANDFIELD). Aucune valeur n'est
chargee. Le parcours utilise SCAN, jamais KEYS, avec au plus 20 pages et
un budget de 10 secondes verifie entre les commandes. COUNT reste une
indication Redis, pas une limite stricte de reponse serveur. Une base 0..15
est acceptee pour ce premier connecteur standalone. L'ACL serveur reste
responsable des droits et SCAN ne fournit pas de snapshot.

MinIO liste un bucket choisi, avec prefixe facultatif, noms et tailles des
objets. Il ne telecharge pas leur contenu. Les listes sont limitees a
100 objets et annoncent truncated lorsque le resultat est incomplet.
Le compte doit pouvoir verifier l'existence du bucket et lister ses objets.
Les delais reseau sont bornes ; une commande deja commencee peut depasser
legerement le budget d'exploration global.

Ces deux explorations ne creent pas de job Spark ni de table Trino.
L'analyse SQL Redis et des fichiers Parquet deja enregistres est decrite
dans [le guide analytique](redis-analytics.md). L'import d'un fichier MinIO
arbitraire et l'extraction Redis vers Parquet restent des fonctionnalites
distinctes, pas des capacites annoncees par ces connecteurs.

## Lancement et tests

Apres avoir lance l'environnement [Redis analytique](redis-analytics.md) :

```powershell
docker compose build backend
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml up -d --no-deps --wait backend
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml --profile tools run --rm --no-deps backend-tests mvn -B test '-Dtest=NativeSourceTests,NativeSourceIntegrationTests,RedisAnalyticsIntegrationTests,ConnectionIntegrationTests,DatabaseIntegrationTests'
```

Les tests couvrent validation, routage, secrets masques, TLS par defaut,
exploration bornee, echec des identifiants, compatibilite SQL Server et
jointure analytique Redis/MinIO. Aucun test ne modifie les donnees metier.

Reference : [client Java Jedis officiel](https://redis.io/docs/latest/develop/clients/jedis/connect/).

Validation locale : 20 tests cibles passes dans Docker, dont 4 integrations
Redis/MinIO natives, 5 integrations analytiques et 7 integrations SQL Server.
