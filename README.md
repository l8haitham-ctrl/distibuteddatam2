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

Cette branche de socle expose `/health`, `/actuator/health/readiness` et `/docs`.
Les endpoints de connexion et d'exploration sont ajoutes dans les branches suivantes.

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
    connection/      Connexion et sources (branche suivante)
    exploration/     Structure des bases (branche suivante)
  shared/            Erreurs communes et traduction HTTP
```

Le trajet est `controleur -> service -> connecteur -> base source`.
Le SQL reste dans les connecteurs. Les erreurs ne renvoient jamais les mots
de passe ou les details du pilote aux clients.

## Interface et prochaines etapes

L'interface Angular est developpee sur sa propre branche. Sa version finale
sera servie par Nginx dans Docker ; `/api` sera transmis au backend Compose.
La maquette et le mode connecte seront clairement distingues.

Le [backlog](docs/roadmap.md) comprend le glossaire metier lie aux tables et
colonnes, puis l'extraction, MinIO, le catalogue, Trino et les benchmarks.

References techniques : [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html),
[JDBC SQL Server](https://learn.microsoft.com/en-us/sql/connect/jdbc/configuring-the-client-for-ssl-encryption),
[Angular](https://angular.dev/reference/versions).

## Skills du projet

Les consignes par fonctionnalité, le workflow de maintenance et les contrats API
sont accessibles dans [les skills Atlas](.agents/skills/README.md). Le contexte
indique les fonctionnalités et routes présentes dans cette branche.
