# Atlas — plateforme de donnees

Projet pedagogique : connecter une base relationnelle, explorer sa structure,
puis construire un pipeline analytique Parquet / MinIO / catalogue / Trino.
La stack retenue est **Angular + Java 21 / Spring Boot**, entierement conteneurisee.

## Branches de fonctionnalites

Les branches sont empilees pour conserver leurs dependances sans fusionner
automatiquement la branche principale :

| Branche | Contenu | Base de revue |
| --- | --- | --- |
| `feature/springboot-foundation` | Socle Java, configuration, sante, Docker, documentation API | Version precedente |
| `feature/database-connections` | Connexions, sources, compte de lecture | Socle Spring Boot |
| `feature/schema-exploration` | Tables, colonnes, types, cles et relations | Connexions |
| `feature/angular-database-explorer` | Maquette et interface Angular connectee | Exploration |
| `feature/cloud-ready-containers` | Developpement conteneurise et architecture Azure/AWS | Interface Angular |
| `feature/minio-storage` | MinIO, bucket prive, service de stockage et tests reels | Conteneurs cloud |
| `feature/storage-jobs` | Jobs PostgreSQL, API asynchrone et premiere ingestion Spark/Parquet | Stockage MinIO |

Le socle expose `/health`, `/actuator/health/readiness` et `/docs`.
La fonctionnalite de connexion ajoute `GET /sources`,
`POST /sources/{id}/test` et `POST /connections/test`.

## Demarrer sans SDK sur Windows

Seul Docker Desktop (conteneurs Linux) est necessaire. Aucun Java ou Maven
local n'est requis : la compilation et les tests unitaires sont dans le Dockerfile.

Pour une premiere installation :

```powershell
if (-not (Test-Path -LiteralPath .env)) { Copy-Item .env.example .env }
```

Definir des mots de passe forts dans `.env` **avant la premiere initialisation**.
Si la base existe deja, conserver `MSSQL_SA_PASSWORD` : changer le fichier ne
change pas le mot de passe enregistre dans le volume SQL Server.

```powershell
docker compose up -d --build
docker compose ps -a
```

- API : [http://localhost:8000/health](http://localhost:8000/health).
- Documentation : [http://localhost:8000/docs](http://localhost:8000/docs).
- Stockage : [http://localhost:8000/storage/status](http://localhost:8000/storage/status).
- Console MinIO : [http://localhost:9001](http://localhost:9001).
- Spark : [http://localhost:8088](http://localhost:8088).
- SQL Server : `localhost,1433`, base `BusinessDB`, utilisateur administratif `sa`.

Les ports sont accessibles uniquement en local. Le conteneur Java s'execute
sans utilisateur root, ferme ses connexions et supporte l'arret gracieux.
La configuration est externalisee dans les variables d'environnement.

## Base de test persistante

| Table | Lignes initiales | Relation |
| --- | ---: | --- |
| Customer | 1 000 | Un client a plusieurs factures |
| Invoice | 5 000 | customer_id reference Customer |
| Payment | 8 000 | invoice_id reference Invoice |

Les factures couvrent 2025 et 2026 : 3 500 payees en deux versements, 1 000
partiellement payees et 500 impayees. Les montants sont en `DECIMAL(12,2)`.
Ce petit jeu deterministe sert au developpement ; les benchmarks utiliseront
plus tard des volumes plus importants.

`sqlserver-init` attend SQL Server, puis reessaie l'initialisation si une base
persistante termine sa recuperation. `Exited (0)` est son etat final normal.
Les relances ne dupliquent pas les donnees. `docker compose down` conserve
le volume : ne pas utiliser `down -v` pour un simple redemarrage.

Verifier les relations, dates et paiements :

```powershell
docker compose run --rm --no-deps sqlserver-init '/opt/mssql-tools18/bin/sqlcmd -S sqlserver -U sa -C -b -i /scripts/02-verify.sql'
```

Si les logs indiquent `Password did not match`, remettre dans `.env` le mot
de passe qui a initialise la base. S'il est perdu, une recuperation hors ligne
avec `mssql-conf --noprompt set-sa-password` peut conserver les donnees.
Arreter le service et sauvegarder le volume avant cette operation. `backups/`
est ignore par Git et les sauvegardes anterieures conservent les anciens identifiants.

## Tests dans Docker

```powershell
docker compose --profile tools run --rm backend-tests
```

Le socle utilise JUnit et Spring MVC Test. Les branches fonctionnelles ajoutent
leurs tests de connexion et d'integration SQL Server au meme parcours.

## Architecture du code

```text
backend/src/main/java/com/atlas/explorer/
  ExplorerApplication.java
  features/
    health/          Sante du service
    connection/      Connexion, sources, DTO, services et connecteurs JDBC
    exploration/     Exploration des schemas, tables, colonnes et relations
    storage/         Stockage objet, flux, metadonnees et bootstrap local MinIO
    jobs/            API, historique PostgreSQL, dispatcher et connecteur Spark
  shared/            Erreurs communes et traduction HTTP
```

Le trajet est `controleur -> service -> connecteur -> base source`.
Le SQL reste dans les connecteurs. Les erreurs ne renvoient jamais les mots
de passe ou les details du pilote aux clients.

## Etape 1 : stockage MinIO

MinIO fournit le stockage objet S3 local. Un bucket est un conteneur d'objets ;
chaque fichier y est identifie par une cle, par exemple
`sqlserver/dbo/Invoice/job-123/part-0001.parquet`. Ces prefixes organisent les
objets ; ce ne sont pas des dossiers physiques. Ce module de stockage prepare
le parcours d'extraction Spark/Parquet decrit a l'etape 2.
Compose lance une instance locale unique, sans haute disponibilite.

Le demarrage suit cet ordre :

```text
MinIO sain -> storage-init -> bucket lake + compte applicatif -> API
SQL Server sain -> sqlserver-init -> backend-init ----------------^
```

Le service ponctuel `storage-init` termine normalement en `Exited (0)`.
Il peut etre relance : il conserve le bucket et ses objets, met a jour le
compte applicatif et reapplique sa politique d'acces. Le compte de l'API peut
lire, ecrire et supprimer des objets dans le seul bucket configure ; il ne
peut ni administrer MinIO ni creer d'autres buckets. Aucun acces public n'est
accorde aux objets. Le volume `minio-data` conserve les fichiers aux redemarrages.

Pour une installation existante, ajouter les variables MinIO de `.env.example`
a `.env`, choisir deux secrets differents, puis relancer `docker compose up -d --build`.
Conserver les identifiants SQL Server existants. Ne pas partager `.env`.

- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` : connexion administrateur a la
  console ; transmis uniquement a MinIO et au bootstrap.
- `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` : compte applicatif limite.
- `STORAGE_BUCKET` : `lake` par defaut ; nom de 3 a 63 caracteres minuscules,
  chiffres ou tirets, commencant et terminant par une lettre ou un chiffre.
- `STORAGE_REGION` : `us-east-1` par defaut.
- `MINIO_API_PORT` / `MINIO_CONSOLE_PORT` : ports locaux 9000 / 9001.

Dans Docker, l'API utilise `http://minio:9000`, pas `localhost:9000`.
Hors Compose, activer le module avec `STORAGE_ENABLED=true` et renseigner
`STORAGE_ENDPOINT`, la region, le bucket et les identifiants applicatifs.
Le module est desactive par defaut, notamment dans le bootstrap SQL et les
modeles Kubernetes existants. Les adaptateurs Azure/identites IAM ne sont
pas encore implementes.

`GET /storage/status` retourne `{"status":"ready","bucket":"lake"}` si
le compte peut acceder au bucket, ou HTTP 503 avec un message generique.
Ce controle verifie l'acces au bucket ; les tests verifient aussi l'ecriture.
Le service Java `StorageService` permet l'envoi par flux de taille connue,
la lecture par flux, les metadonnees et la suppression. L'appelant ferme les
flux. Le SDK utilise des parties de 5 Mio pour les envois multiparties,
sans charger le fichier entier. Les extractions passent par l'API de jobs ;
il n'y a pas de route d'envoi libre de fichiers.

Le test Docker existant inclut maintenant une vraie integration MinIO :
aller-retour CSV, fichier vide, envoi multipartie de plus de 6 Mio avec
comparaison SHA-256, suppression, fichier absent, refus d'acces anonyme et
refus de creation d'un bucket par le compte applicatif. Les fichiers de test
sont supprimes apres verification. `RUN_STORAGE_TESTS=true` active ces tests.

L'image MinIO est construite a partir de la version officielle epinglee
`RELEASE.2025-10-15T17-29-55Z` ; le premier build telecharge et compile Go
dans Docker et prend plusieurs minutes. Aucun Go local n'est necessaire.
Cette version est distribuee sous AGPL-3.0 ; la licence est conservee dans
l'image. Le depot communautaire est archive : ce choix repond au sujet
pedagogique et devra etre reevalue avant une exploitation durable.
Voir la [publication officielle](https://github.com/minio/minio/releases/tag/RELEASE.2025-10-15T17-29-55Z)
et le [depot MinIO](https://github.com/minio/minio).

## Etape 2 : jobs persistants et Spark

`POST /storage/jobs` accepte une table SQL Server et renvoie HTTP 202 avec un
UUID. PostgreSQL conserve la demande et son etat ; Spark execute l'extraction
en arriere-plan, ecrit du Parquet dans MinIO et publie un manifeste de resultat.
`GET /storage/jobs/{id}` suit le traitement ; `GET /storage/jobs/{id}/result`
retourne les compteurs et l'emplacement une fois termine. `GET /storage/jobs`
liste les 100 derniers traitements. Spring Batch n'est pas utilise.

Ajouter `POSTGRES_ADMIN_PASSWORD` et `JOBS_PASSWORD` depuis `.env.example`
avant de reconstruire une installation existante. Conserver les identifiants
deja enregistres dans les volumes. Les nouvelles bases et leurs volumes sont
distincts de BusinessDB et du stockage MinIO. PostgreSQL n'est pas expose sur
un port Windows. L'API et le worker utilisent des comptes applicatifs limites.

Le [guide des jobs](docs/storage-jobs.md) explique le flux, les requetes
PowerShell, les etats, la recuperation apres redemarrage et les limites. La
premiere version prend en charge Parquet, compression snappy ou aucune, une
partition JDBC et un job actif. Le partitionnement des fichiers et l'interface
Angular de suivi viennent ensuite ; une demande de partition non vide est
refusee explicitement. Les fichiers seuls ne sont pas encore accessibles via SQL.

## Connexions

Le service ponctuel `backend-init` cree `lake_reader`, lui attribue les droits
de lecture et de consultation des definitions, puis termine en `Exited (0)`.
L'API ne recoit que `SOURCE_PASSWORD`, pas le mot de passe `sa`.
`POST /connections/test` accepte `engine`, `host`, `port`, `database`,
`username`, `password`, `trust_server_certificate`. Seul `sqlserver` est
implemente. Le certificat est verifie par defaut ; l'exception explicite est
la source Docker locale avec certificat auto-signe.

Le serveur est vu depuis le conteneur API : `sqlserver` pour le projet,
`host.docker.internal` pour une instance sur Windows. Les identifiants soumis
ne sont pas persistes. Le pilote JDBC recoit chaque propriete separement.

Les tests d'integration sont actives par `RUN_DB_TESTS=true` dans le service
`backend-tests` et verifient une vraie connexion ainsi que les privileges limites.

## Interface et prochaines etapes

L'exploration ajoute :

- `GET /sources/{id}/tables` : tables par schema.
- `GET /sources/{id}/tables/{table}?schema=dbo` : colonnes, types, precision,
  echelle, nullabilite, cle primaire et cles etrangeres.
- `POST /connections/explore` : meme configuration que le test de connexion,
  avec retour de la structure complete accessible au compte soumis.

Les noms de schemas et tables sont lies en parametres SQL. La structure est
lue dans les catalogues SQL Server ; les relations sont celles declarees dans
la base, pas des liens inférés a partir des noms de colonnes.

L'interface Angular est disponible sur [http://localhost:3000](http://localhost:3000).
Nginx la sert depuis un conteneur non root et transmet `/api` au backend.
Compose relance le frontend lors d'une mise a jour du backend pour renouveler
la resolution de son adresse dans le proxy.
La [maquette interactive](http://localhost:3000/?maquette=1) utilise des
metadonnees illustratives et n'effectue aucune connexion reelle.

Le mode connecte charge BusinessDB, permet de rechercher une table, de
consulter ses colonnes et de suivre ses relations. **Nouvelle connexion**
ouvre le formulaire SQL Server avec test et exploration. Les identifiants
ne sont pas conserves dans le navigateur ; une connexion personnalisee
charge les metadonnees puis abandonne le mot de passe. Pour actualiser cette
source, il faut le saisir a nouveau.

La compilation TypeScript et les tests Angular sont executes pendant
`docker compose build frontend`. Les sources sont organisees dans
`frontend/src/app/core` (API et etat), `features` (composants fonctionnels)
et `shared` (composants communs). Les polices sont embarquees dans l'image.

## Developper le projet dans des conteneurs

Docker Compose **2.24.4 ou plus recent** est requis pour l'override des ports.
Pour travailler avec les sources montees et le rechargement Angular :

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

Le serveur Angular et Maven tournent dans leurs propres conteneurs. Aucun
SDK Windows n'est necessaire. Les dependances Node et Maven sont dans des
volumes dedies. Apres modification Java, redemarrer `backend` pour relancer
la compilation ; Angular recharge automatiquement les changements frontend.

Un conteneur `workspace` fournit Java, Maven et Node pour travailler sur le
projet lui-meme. Dans VS Code avec Dev Containers, utiliser **Reopen in
Container** ; la configuration `.devcontainer/devcontainer.json` ouvre ce
workspace avec les services de developpement. Il est aussi utilisable en CLI :

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml --profile tools run --rm --no-deps workspace bash
```

Pour revenir aux images de production locales :

```powershell
docker compose up -d --build
```

Les images `*-dev:local` sont distinctes des images de production : le
bootstrap SQL continue d'utiliser le JAR de production. Les identifiants
administratifs ne sont jamais injectes dans le frontend ou dans l'API.

## Preparation Azure / AWS

L'[architecture cloud](docs/architecture-cloud.md) explique les correspondances
AKS/EKS, ACR/ECR, bases privees, stockage objet et gestion des secrets.
`deploy/k8s/base` contient des manifests prives pour les deux images, avec
ressources, sondes, secrets externes et execution non root.

Ces fichiers sont des modeles de preparation : aucun cloud n'est deploye.
Renseigner les images, la source et le secret externe, et terminer TLS/OIDC
avant d'ajouter une entree publique. Le catalogue, Trino, les optimisations
d'extraction et le glossaire metier restent au backlog.

La [feuille de route](docs/roadmap.md) donne l'ordre des prochaines etapes,
dont les jobs, l'extraction, le catalogue, Trino et le glossaire metier.

References techniques : [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html),
[JDBC SQL Server](https://learn.microsoft.com/en-us/sql/connect/jdbc/configuring-the-client-for-ssl-encryption),
[Angular](https://angular.dev/reference/versions).

## Skills du projet

Les consignes par fonctionnalité, le workflow de maintenance et les contrats API
sont accessibles dans [les skills Atlas](.agents/skills/README.md). Le contexte
indique les fonctionnalités et routes présentes dans cette branche.
