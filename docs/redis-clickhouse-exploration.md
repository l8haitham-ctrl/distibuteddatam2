# Explorer Redis et ClickHouse

Branche `feature/clickhouse-native-exploration`, base `feature/kaggle-soccer-source`.
Le connecteur ClickHouse complete l'exploration native Redis/MinIO existante.
Les sources relationnelles SQL Server, PostgreSQL et Football sont conservees.

## Parcours utilisateur

Depuis **Sources**, choisir **Explorer Redis, ClickHouse ou MinIO**, ou ouvrir
[/sources/native](http://localhost:3000/sources/native). Se connecter a la
plateforme si necessaire.

1. **Tester Redis** verifie l'authentification et le PING de `redis-demo`.
2. **Explorer Redis** affiche un echantillon de cles, leurs types et les noms
   de champs des hashes. Le profil cible `analytics:customer_segments:` dans
   la base Redis 0. Les 1 000 hashes existants ne sont pas modifies.
3. **Tester ClickHouse (OLAP)** verifie l'authentification et l'existence de
   la base `analytics` du profil `clickhouse-demo`.
4. **Explorer ClickHouse (OLAP)** affiche les tables `customer`, `invoice` et
   `payment`, leurs moteurs `MergeTree`. Deplier une table affiche les colonnes,
   leurs types exacts (`UInt64`, `Nullable(String)`, `Decimal(12, 2)`, etc.),
   les colonnes de tri et les colonnes de partitionnement.

Les cles de tri ClickHouse ne garantissent pas l'unicite et ne sont pas des
cles primaires relationnelles. Redis ne fournit pas de schema tabulaire fixe.
Cette exploration consulte la structure, sans charger les valeurs metier.
Elle ne lance ni Spark ni un nouveau catalogue Trino.

Le formulaire personnalise accepte aussi ClickHouse : hote depuis le backend,
port de l'interface HTTP, base de donnees, compte et prefixe de noms de tables.
TLS est verifie par defaut ; seuls les profils Docker locaux le desactivent.
Les identifiants des profils restent cote serveur. Les nouvelles connexions
personnalisees sont temporaires et ne conservent pas leur mot de passe.

## Lancement Docker

Les cinq fichiers existants restent actifs ; ajouter le sixieme en dernier :

```powershell
./scripts/setup-clickhouse.ps1
$compose = @('-f','docker-compose.yml','-f','docker-compose.connectors.yml',
  '-f','docker-compose.analytics.yml','-f','docker-compose.benchmarks.yml',
  '-f','docker-compose.soccer.yml','-f','docker-compose.clickhouse.yml')
docker compose @compose up -d --no-deps --wait clickhouse-source
docker compose @compose run --rm --no-deps clickhouse-init
docker compose @compose build backend frontend
docker compose @compose up -d --no-deps --wait backend frontend
```

`setup-clickhouse.ps1` ajoute seulement les secrets manquants dans `.env`.
L'image officielle deja disponible est epinglee par digest (version locale
25.11.1.558). Un volume distinct conserve ClickHouse. Aucun volume existant
n'est supprime et aucun reseed SQL Server/Redis n'est execute.

`clickhouse-init` cree une fixture synthetique independante : 1 000 clients,
5 000 factures et 8 000 paiements. Ce n'est pas une copie des bases SQL Server
ou du dataset Football. Les tables existantes ne sont jamais tronquees ; les
insertions de demonstration ont lieu seulement quand la table est vide.

Connexion depuis le backend : `clickhouse-source:8123`, base `analytics`,
utilisateur `atlas_reader`, secret `CLICKHOUSE_READER_PASSWORD`. Depuis le PC :
`localhost:8123`. Le compte d'administration est reserve a l'initialisation,
son mot de passe n'est pas transmis au backend. Le compte lecteur a des droits
SELECT limites et le profil `readonly=1`.

## API et architecture

Le fournisseur `ClickhouseNativeConnector` implemente `NativeConnector` dans
`features/nativesources/connector`. Le routage, les controles HTTP et les erreurs
restent dans les couches existantes. Aucun pilote JDBC supplementaire ni
changement de la SPI d'ingestion relationnelle n'est necessaire.

- `GET /native/connectors` : ajoute `clickhouse`, capacites connexion/exploration.
- `GET /native/sources` : ajoute `clickhouse-demo`, sans mot de passe.
- `POST /native/sources/clickhouse-demo/test` : test du profil configure.
- `POST /native/sources/clickhouse-demo/explore` : tables et colonnes.
- `POST /native/connections/test` et `/explore` : configuration personnalisee.

Exemple de configuration personnalisee (session et jeton CSRF requis) :

```json
{
  "engine": "clickhouse", "host": "clickhouse-source", "port": 8123,
  "namespace": "analytics", "username": "atlas_reader",
  "password": "<secret lecteur local>", "tls": false, "prefix": "inv"
}
```

Le protocole ClickHouse HTTP utilise les en-tetes d'authentification et des
parametres SQL types pour la base et le prefixe. Aucune concatenation de saisie
dans SQL, aucune redirection suivie, aucun mot de passe dans l'URL.
Les lectures sont bornees a 100 tables, 200 colonnes par table, 2 Mo par reponse
et cinq secondes par requete (trois requetes maximum pour l'exploration).
Les limites sont annoncees avec `truncated` et `columns_truncated`.
Une reponse trop volumineuse ou invalide produit une erreur generique.

Redis conserve SCAN, ses limites de temps et de taille, et les champs des hashes
sans valeurs. Voir [les sources natives](native-sources.md).

## Tests

```powershell
docker compose @compose --profile tools run --rm --no-deps backend-tests mvn -B '-Dtest=NativeSourceTests,NativeSourceIntegrationTests,ClickhouseConnectorTests,ClickhouseIntegrationTests,ConnectionIntegrationTests,DatabaseIntegrationTests' test
```

Les tests Angular et le build de production sont executes par l'image frontend.
Les tests ClickHouse couvrent l'exploration API authentifiee, les types OLAP,
le prefixe litteral, les identifiants invalides, le compte sans ecriture,
les limites, la protection des secrets et le refus des redirections.

References officielles : [interface HTTP](https://clickhouse.com/docs/interfaces/http),
[colonnes systeme](https://clickhouse.com/docs/operations/system-tables/columns),
[droits des utilisateurs](https://clickhouse.com/docs/operations/settings/settings-users).

## Verification locale du 26 novembre 2026

Les 23 tests backend cibles passent sur les conteneurs reels. Le build backend
execute aussi 60 tests sans infrastructure (52 integrations desactivees pendant
ce build). Les 58 tests Angular et le build de production passent dans Docker.
Le controle navigateur confirme les tests de connexion des deux profils,
l'echantillon Redis de 100 hashes, les trois tables ClickHouse et les colonnes
de `invoice`, dont `Date`, `Decimal(12, 2)` et `LowCardinality(String)`.
Les 45 jobs existants sont conserves et aucune extraction n'est creee.

Preuves : [rapport](../validation/results/redis-clickhouse-exploration-2026-11-26.json),
[Redis](evidence/redis-native-exploration.jpg),
[ClickHouse](evidence/clickhouse-native-exploration.jpg).
