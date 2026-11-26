# Livraison locale reproductible

Version complete : `feature/release-readiness`, base `feature/data-quality-report`.
La branche master n'est pas fusionnee automatiquement. Seuls Git, Docker
Desktop avec conteneurs Linux et PowerShell sont necessaires sur Windows.
Java, Maven, Node et les tests de l'application s'executent dans Docker.

## Premiere installation

```powershell
git clone --branch feature/release-readiness https://github.com/l8haitham-ctrl/distibuteddatam2.git atlas
cd atlas
powershell -NoProfile -File scripts/setup-env.ps1
docker compose up -d --build --wait --wait-timeout 300
docker compose ps -a
```

Le script cree .env uniquement s'il n'existe pas, genere 19 secrets distincts
et ne les affiche pas. Conserver ce fichier avec les volumes ; modifier un
mot de passe dans .env ne change pas celui d'une base deja initialisee.
Ne pas committer ce fichier. Le script ne remplace jamais une configuration
existante, meme si elle contient encore des valeurs a completer.

Le Compose de base fournit SQL Server, l'API, Angular, Spark, MinIO,
PostgreSQL de metadonnees, Hive Metastore et Trino. Les initialisations sont
automatiques et conservent les donnees existantes. SEED_SCALE=small reste
le jeu de demonstration par defaut : 1000 clients, 5000 factures, 8000 paiements.

Interfaces : [Angular](http://localhost:3000), [Swagger](http://localhost:8000/docs),
[MinIO](http://localhost:9001), [Spark](http://localhost:8088).
Les ports publies sont lies a 127.0.0.1. Trino utilise un certificat local ;
la console SQL Angular passe par l'API et ne necessite pas de visite de son UI.

## Redemarrer une installation existante

```powershell
git fetch origin
git switch feature/release-readiness
powershell -NoProfile -File scripts/setup-env.ps1
powershell -NoProfile -File scripts/setup-login.ps1
docker compose up -d --build --wait --wait-timeout 300
```

Ne pas recreer les workers pendant une extraction active. docker compose stop
arrete les services sans supprimer les volumes. Ne pas utiliser down -v sur
l'environnement principal pour une mise a jour. Aucun reseed n'est requis.

## Sources optionnelles

Pour activer PostgreSQL metier, Redis et leurs profils de demonstration :

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml up -d --build --wait --wait-timeout 300
```

Les secrets optionnels sont deja generes par setup-env.ps1 pour une nouvelle
installation. Sur une ancienne configuration, les completer selon .env.example
sans remplacer les secrets des volumes existants. Utiliser ensuite les memes
fichiers Compose pour conserver ces sources lors d'un redemarrage.
Voir [PostgreSQL](postgresql-connector.md) et [Redis/MinIO](native-sources.md).

L'override benchmarks ajoute une source isolee BusinessBenchmark ; preparer
son jeu de donnees selon [le protocole](benchmarks.md) avant de l'utiliser.

## Controle fonctionnel Docker

```powershell
docker compose -f docker-compose.yml -f docker-compose.validation.yml build validation
docker compose -f docker-compose.yml -f docker-compose.validation.yml --profile tools run --rm --no-deps validation --run-id=controle-local
```

Le controle verifie Angular, le proxy, les connexions, le stockage, la
structure, Spark, une extraction Invoice et son resultat SQL. Il conserve
le job, les fichiers et le rapport dans validation/results. Il ne relance pas
automatiquement un traitement en echec ou une soumission incertaine.
Pour reprendre un controle interrompu dont le job est connu, ajouter --resume
avec le meme run-id. Ne pas utiliser --require-empty sur une installation
existante : ce drapeau sert exclusivement au test de volumes neufs.

## Tests complets

La suite de base utilise atlas_test et des schemas/objets de test isoles :

```powershell
docker compose --profile tools run --rm --no-deps backend-tests mvn -B test
docker build --target build -t distributed-data-frontend-tests:local frontend
docker run --rm --entrypoint npm distributed-data-frontend-tests:local test
```

Les tests optionnels PostgreSQL, Redis et qualite demandent la stack et les
parametres documentes dans [le rapport de validation](release-validation.md).
Ne pas lancer les tests d'ingestion et une autre extraction au meme moment.

## Etat de livraison

[Rapport de validation](release-validation.md), [qualite](data-quality.md),
[benchmarks](benchmark-report.md), [demonstration](demo-guide.md) et
[integration](integration-plan.md) constituent les preuves de cette version.
Il s'agit d'une livraison pedagogique locale. L'authentification locale est ajoutee
sur `feature/platform-authentication` ; voir [le guide](authentication.md).
La sauvegarde permanente des sources creees dans l'UI et le deploiement cloud
ne sont pas presentes comme des fonctionnalites validees. Le glossaire reste
differe et les bonus sont exclus.
