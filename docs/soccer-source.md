# Source Football Kaggle

Branche `feature/kaggle-soccer-source`, base `feature/source-test-progress`.
Cette extension utilise le connecteur SQL Server existant, sans changer l'API.

## Origine

[European Soccer Database, Hugo Mathien](https://www.kaggle.com/datasets/hugomathien/soccer),
version 10 : fichier SQLite converti en SQL Server, aucune donnee synthetique.
La fiche Kaggle indique « Database: Open Database, Contents: © Original Authors » ;
consulter cette fiche avant toute redistribution. Les fichiers de donnees et les
sauvegardes ne sont pas publies dans ce depot. Le rapport contient l'empreinte
SHA-256 de l'archive telechargee.

## Import Docker

Conserver les quatre configurations precedentes et ajouter la cinquieme :

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml -f docker-compose.soccer.yml --profile tools build soccer-import
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml -f docker-compose.soccer.yml --profile tools run --rm --no-deps soccer-import
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml -f docker-compose.soccer.yml up -d --no-deps backend spark-worker
```

SQL Server doit deja fonctionner et le login `lake_reader` doit exister.
L'import refuse d'ecraser une base `EuropeanSoccer` existante. Les tables,
donnees, contraintes et droits sont importes dans une transaction. En cas
d'erreur, cette transaction est annulee ; la base vide creee auparavant peut
subsister et doit etre inspectee avant une nouvelle tentative.

L'archive et SQLite sont temporaires dans le conteneur, supprimes a sa sortie.
Les donnees SQL persistent dans le volume SQL Server habituel. Les secrets
proviennent de `.env`, jamais du code ni du rapport.

| Table | Lignes |
|---|---:|
| Country | 11 |
| League | 11 |
| Team | 299 |
| Player | 11 060 |
| Match | 25 979 |
| Team_Attributes | 1 458 |
| Player_Attributes | 183 978 |

Le transfert utilise des lots de 100 lignes et compare ensuite toutes les
valeurs SQLite/SQL Server, pas seulement les comptes. `date` et `birthday`
deviennent `datetime2(0)` ; les hauteurs fractionnaires et les cotes restent
en `float`, les autres entiers en `bigint`, les textes en `nvarchar(max)`.
Les valeurs NULL sont conservees. Les sept cles primaires sont preservees ;
sept relations principales sont ajoutees avec validation SQL Server et index.
Les 22 colonnes de composition de joueurs dans Match restent des identifiants
bruts sans contraintes ajoutees : leur integrite n'est pas affirmee.

## Utilisation

Dans **Sources**, choisir `soccer`, puis **Tester** et **Explorer**.
Connexion interne : `sqlserver:1433`, base `EuropeanSoccer`, utilisateur
`lake_reader`, mot de passe `LAKE_READER_PASSWORD` du fichier `.env`.
Depuis le PC, utiliser `localhost,1433`.

Choisir par exemple `Match` pour une extraction Parquet avec un partitionnement
par `date`, ou `Player_Attributes` pour les lots. L'ajout d'une source ne cree
aucune extraction automatiquement et n'enregistre aucune table dans Trino :
declencher ensuite un job pour obtenir les tables analytiques.

```json
{
  "source_id": "soccer",
  "table": "Match",
  "format": "parquet",
  "partition_by": ["date"]
}
```

Tests de connexion et d'exploration sur les bases reelles :

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml -f docker-compose.soccer.yml --profile tools run --rm --no-deps backend-tests mvn -B '-Dtest=SoccerIntegrationTests,ConnectionIntegrationTests,DatabaseIntegrationTests' test
```

Les details de l'import se trouvent dans `datasets/soccer/results/import.json`.

Test de bout en bout (cree une extraction de 299 equipes et sa table Trino) :

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml -f docker-compose.soccer.yml --profile tools run --rm --no-deps soccer-check
```

Le rapport `datasets/soccer/results/pipeline.json` conserve l'identifiant du job.
Une relance de cette verification reutilise cet identifiant ; aucune nouvelle
extraction n'est soumise automatiquement apres un resultat incertain.
Ces rapports locaux sont ignores par Git ; les preuves de verification sont
copiees dans `validation/results/soccer-source-2026-11-26.json` sans identifiants
de connexion secrets. Une nouvelle installation cree son propre job.

## Verification locale du 26 novembre 2026

- Les 222 796 lignes des sept tables ont ete comparees valeur par valeur.
- Les sept relations principales ont ete ajoutees avec `WITH CHECK`.
- Sept tests BusinessDB et trois tests EuropeanSoccer passent dans Docker.
- Le refus d'ecraser une base existante a ete verifie sans modifier les donnees.
- Les 299 equipes ont ete extraites dans un fichier Parquet Snappy de 11 288 octets
  et recomptees dans Trino. Vue disponible : `hive.soccer_dbo.team`.
- Les 44 anciens jobs sont conserves ; la verification ajoute un seul job.
