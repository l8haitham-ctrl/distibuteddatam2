# Plateforme de donnees — SQL Server et API d'exploration

Cette premiere etape fournit une base SQL Server locale, persistante et prete
pour developper la connexion et l'exploration depuis une future interface.

## Prerequis

- Docker Desktop demarre, avec le moteur de conteneurs Linux.
- Une machine x86-64 et suffisamment de memoire pour SQL Server et Docker.
- Le port local 1433 disponible, ou un autre port configure dans `.env`.

SQL Server Developer est utilise pour ce projet de developpement et de test.
SQL Server est proprietaire ; il constitue la source imposee par le sujet.
L'image est celle de Microsoft : `mcr.microsoft.com/mssql/server:2022-latest`.
Ce tag suit les mises a jour de SQL Server 2022. Pour les benchmarks futurs,
il faudra figer le digest de l'image et consigner la version du serveur.

Documentation : [SQL Server dans Docker](https://learn.microsoft.com/sql/linux/quickstart-install-connect-docker).

## Demarrage (PowerShell)

Depuis le dossier du projet :

```powershell
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item .env.example .env
}
```

Uniquement avant la premiere initialisation : choisir un mot de passe fort
dans `.env`. Si la base existe deja, conserver son mot de passe actuel ; ne
pas remplacer `.env` par le fichier d'exemple. Puis executer :

```powershell
docker compose up -d
docker compose ps -a
docker compose logs sqlserver-init
```

Le premier demarrage telecharge l'image. Le service `sqlserver-init` attend que
SQL Server soit sain, puis cree la base et les donnees. Il reessaie jusqu'a
12 fois si une base persistante est encore en cours de recuperation.
Un etat `Exited (0)`
pour ce service est normal : c'est une initialisation ponctuelle. SQL Server
doit rester `healthy`.

Le fichier `.env` est ignore par Git. Le compte `sa` sert ici a l'administration
locale. Le backend utilise `lake_reader`, qui a seulement les droits de lecture
et de consultation de la structure dans BusinessDB. Son mot de passe distinct
est `LAKE_READER_PASSWORD` dans `.env` ; il faut definir cette variable pour
un ancien `.env`, sans modifier `MSSQL_SA_PASSWORD`.

## Base disponible

| Parametre | Valeur |
| --- | --- |
| Serveur depuis Windows | `localhost` |
| Port | `1433` (ou `SQLSERVER_PORT` dans `.env`) |
| Serveur depuis un futur conteneur backend | `sqlserver:1433` |
| Base | `BusinessDB` |
| Schema | `dbo` |
| Authentification | SQL Server |
| Utilisateur d'administration | `sa` |
| Mot de passe | `MSSQL_SA_PASSWORD` dans `.env` |
| Certificat local | Accepter le certificat autosigne pour ce developpement local |

| Table | Lignes initiales | Relation |
| --- | ---: | --- |
| Customer | 1 000 | Un client a plusieurs factures |
| Invoice | 5 000 | `customer_id` reference Customer |
| Payment | 8 000 | `invoice_id` reference Invoice |

Les factures couvrent 2025 et 2026 : 3 500 sont payees en deux versements,
1 000 sont partiellement payees et 500 sont impayees. Les montants utilisent
`DECIMAL(12,2)`. Le jeu est deterministe et volontairement petit pour cette
premiere etape ; un generateur de plus gros volumes sera necessaire pour les
mesures de performance.

## Verification

Ce controle verifie les relations, les dates et la coherence des paiements,
puis affiche les volumes, colonnes et cles etrangeres :

```powershell
docker compose run --rm --no-deps sqlserver-init '/opt/mssql-tools18/bin/sqlcmd -S sqlserver -U sa -C -b -i /scripts/02-verify.sql'
```

Pour ouvrir une session SQL interactive (le mot de passe sera demande) :

```powershell
docker compose exec sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -d BusinessDB
```

Exemple de requete, suivi de `GO` pour l'executer :

```sql
SELECT TOP (10) * FROM dbo.Invoice ORDER BY invoice_id;
GO
```

## Arret et relance

```powershell
docker compose down
docker compose up -d
```

Le volume nomme conserve la base. Le script ne reinjecte pas les donnees si
une des trois tables contient deja des lignes. Ne pas ajouter `-v` a `down`
si vous souhaitez conserver les donnees. Changer le mot de passe dans `.env`
ne change pas celui d'une base deja initialisee.

En cas d'erreur : `docker compose logs sqlserver` et
`docker compose logs sqlserver-init`. Si le port est deja occupe, modifier
`SQLSERVER_PORT` dans `.env`, puis relancer `docker compose up -d`.

Si SQL Server est `unhealthy` et que les journaux indiquent `Login failed`
avec `Password did not match`, le mot de passe de `.env` ne correspond pas
a celui enregistre dans le volume. Retablir le mot de passe d'origine dans
`.env`, puis lancer `docker compose up -d`. Le service d'initialisation peut
rester `Created` tant que le serveur n'est pas sain. Ne pas supprimer le
volume pour corriger ce probleme sans avoir decide de perdre son contenu.

Si le mot de passe d'origine est perdu, une recuperation hors ligne avec
`mssql-conf --noprompt set-sa-password` permet de conserver les donnees.
Arreter SQL Server et sauvegarder le volume avant cette operation. Une
sauvegarde effectuee avant la recuperation locale se trouve dans `backups/`
(ignore par Git). Elle contient encore les anciens identifiants : la restaurer
ne retablirait pas le nouveau mot de passe.

## Etape 2 : API de connexion et d'exploration

```powershell
docker compose up -d --build
```

Le service ponctuel `backend-init` cree le compte `lake_reader` apres
l'initialisation SQL. Il met son mot de passe en coherence avec `.env` a
chaque execution. Lui seul recoit le mot de passe administrateur ; le service
API ne le recoit pas. `backend-init` doit finir en `Exited (0)` et `backend`
rester `healthy`.

Ouvrir [la documentation interactive](http://localhost:8000/docs). Choisir
une operation, cliquer **Try it out**, puis **Execute**. Parcours sans saisir
de mot de passe, avec la source locale deja configuree :

1. `GET /sources` : voir la connexion configuree, sans son mot de passe.
2. `POST /sources/sqlserver/test` : verifier la connexion.
3. `GET /sources/sqlserver/tables` : lister les trois tables.
4. `GET /sources/sqlserver/tables/Invoice?schema=dbo` : afficher colonnes,
   types, precision, echelle, nullabilite, cle primaire et cles etrangeres.

Exemples directs depuis PowerShell :

```powershell
Invoke-RestMethod http://localhost:8000/sources
Invoke-RestMethod -Method Post http://localhost:8000/sources/sqlserver/test
Invoke-RestMethod http://localhost:8000/sources/sqlserver/tables
Invoke-RestMethod 'http://localhost:8000/sources/sqlserver/tables/Invoice?schema=dbo' | ConvertTo-Json -Depth 6
```

Pour tester une connexion saisie par l'utilisateur, utiliser
`POST /connections/test`. `POST /connections/explore` accepte le meme corps
et renvoie la structure complete des tables visibles. Exemple (remplacer le
mot de passe par `LAKE_READER_PASSWORD` de `.env`) :

```json
{
  "engine": "sqlserver",
  "host": "sqlserver",
  "port": 1433,
  "database": "BusinessDB",
  "username": "lake_reader",
  "password": "votre-mot-de-passe-lecteur",
  "trust_server_certificate": true
}
```

Le serveur de base est vu **depuis le conteneur backend** : `sqlserver` pour
ce projet ; `host.docker.internal` pour une autre instance sur Windows.
`localhost` dans ce formulaire designerait le conteneur backend lui-meme.
Le trafic SQL est chiffre. L'acceptation d'un certificat autosigne est
activee pour la source Docker locale ; pour une source externe avec un
certificat valide, garder `trust_server_certificate: false`.

Les connexions soumises ne sont ni sauvegardees ni renvoyees. Les erreurs
d'authentification et de validation ne renvoient pas les mots de passe.
Les noms de tables/schemas sont passes en parametres SQL. L'API n'accepte
pas de SQL libre. Les sources ne voient que les metadonnees accessibles au
compte utilise. Pour l'instant, seul SQL Server est implemente ; Oracle et
les autres moteurs seront des connecteurs supplementaires.

L'API est destinee au developpement local et liee a `127.0.0.1:8000`. Elle
ne fournit pas encore d'authentification utilisateur ou de gestion multiutilisateur.
`GET /health` teste le processus API ; `POST /sources/sqlserver/test` teste
reellement l'acces a la base.

### Verification du backend

```powershell
docker compose exec -T backend python -m unittest discover -s tests -v
```

Les tests utilisent la vraie base de test et verifient les relations,
les types, les droits de lecture, les erreurs et l'absence de mots de passe
dans les reponses. Ils ne modifient pas les donnees.

Documentation technique : [FastAPI](https://fastapi.tiangolo.com/tutorial/first-steps/),
[pilote ODBC Microsoft](https://learn.microsoft.com/en-us/sql/connect/odbc/linux/installing-the-microsoft-odbc-driver-for-sql-server-on-linux).

## Prochaine etape

Construire le formulaire de connexion et l'explorateur visuel des tables
au-dessus de cette API. Puis ajouter les jobs d'extraction par lots vers MinIO.
