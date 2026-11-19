# Volumes de donnees deterministes

Branche `feature/large-dataset`, creee depuis master apres le prompt 0.

## Configuration

| Parametre | Defaut | Role |
| --- | --- | --- |
| SEED_SCALE | small | small ou large |
| SEED_CUSTOMERS | vide | Surcharge du nombre de clients |
| SEED_INVOICES | vide | Surcharge du nombre de factures |
| SEED_DATABASE | BusinessDB | Cible d'une commande d'initialisation explicite |

`small` produit 1 000 clients, 5 000 factures, 8 000 paiements.
`large` produit 100 000 clients, 1 000 000 factures, 1 600 000 paiements.
Le nombre de paiements est derive des factures : 70 % payees en deux versements,
20 % partielles en un versement et 10 % impayees. Le total est donc 1,6 million
plutot que l'approximation 1,5 million du besoin initial. Les surcharges doivent
etre des entiers entre 1 et 10 000 000 ; les paiements restent derives des statuts.

La configuration est validee avant toute substitution SQLCMD. Un nom de base
contient uniquement lettres/chiffres/underscore et commence par une lettre ou
un underscore. Les identifiants ne peuvent donc pas injecter du SQL.

## Conservation des volumes existants

`init.sh` cree la base et les tables si necessaire, installe le generateur et
ajoute `updated_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()` aux trois
tables si la colonne manque. Une valeur deterministe est alors affectee aux
lignes existantes : created_at + 1 jour, invoice_date + 2 jours ou payment_date
+ 1 jour. Les futurs INSERT qui omettent la colonne recoivent l'heure UTC du
serveur. Aucune mise a jour automatique de cette colonne lors d'un UPDATE n'est
encore implementee : le chargement incremental fait partie du bonus exclu.

Si une des tables contient des donnees, la generation est ignoree. Changer
SEED_SCALE ne remplace donc jamais les donnees existantes. Les autres valeurs
des lignes existantes sont conservees ; un nouveau small utilise maintenant
20 villes et une activite client inegale.

## Generation SQL

Une table temporaire de 100 000 nombres est construite par produits cartesiens.
Elle sert d'offset pour des INSERT set-based : 100 000 clients ou factures par
transaction ; les paiements sont produits par plages de 50 000 factures, soit
au plus 100 000 paiements par transaction. Les boucles parcourent des lots,
pas les lignes. Le profil large n'est plus limite a 10 000 lignes.

Les dates parcourent les 730 jours de 2025 et 2026, donc 24 mois complets pour
les profils standard. Une surcharge inferieure a 730 factures couvre seulement
le debut de cette periode. 60 % des factures ciblent approximativement le premier
cinquieme des clients ; le reste utilise une permutation deterministe.
Les montants sont generes en decimal, sans erreur d'arrondi entre versements.

Les commandes de generation et de reseed sont serialisees par un verrou
applicatif SQL Server. Un marqueur de base signale une generation interrompue :
les redemarrages ne considerent pas un chargement partiel comme reussi.
Une relance explicite par reseed est alors necessaire. Une generation n'est pas
atomique sur l'ensemble du jeu : les transactions deja validees restent presentes.

## Grand volume sans remplacer BusinessDB

```powershell
docker compose run --rm --no-deps sqlserver-init 'bash /scripts/measure-large.sh'
```

Cette commande cree `BusinessBenchmark`, genere le profil large, verifie les
invariants et mesure duree et taille. Elle refuse si cette base existe deja.
Pour une autre mesure, choisir un nouveau nom :

```powershell
docker compose run --rm --no-deps -e SEED_DATABASE=BusinessBenchmark2 sqlserver-init 'bash /scripts/measure-large.sh'
```

La base de benchmark reste dans le volume SQL Server et n'est pas supprimee
automatiquement. L'API, le worker Spark et les tests continuent d'utiliser
BusinessDB et son petit jeu. Le raccordement des benchmarks au pipeline se
fera dans les fonctionnalites suivantes.

## Reseed destructif, uniquement sur commande explicite

Sauvegarder la base cible et terminer les extractions qui l'utilisent avant
un reseed. Exemple pour remplacer volontairement BusinessDB par small :

```powershell
docker compose run --rm --no-deps -e SEED_SCALE=small -e CONFIRM_RESEED=BusinessDB sqlserver-init 'bash /scripts/reseed.sh'
```

Le script refuse sans CONFIRM_RESEED egal au nom exact de la base cible.
Il n'est jamais execute au demarrage. Les deux FK connues sont retirees dans
une transaction, les tables tronquees dans l'ordre Payment, Invoice, Customer,
puis les FK recreees avec WITH CHECK avant la generation. SQL Server interdit
le TRUNCATE d'une table referencee, meme si la table enfant est vide : l'ordre
seul ne suffit pas. Une erreur dans cette transaction restaure les tables/FK.

Les extractions deja stockees dans MinIO ne sont pas supprimees par le reseed.
Elles representent des instantanes anterieurs de la source.

## Tests reproductibles

```powershell
docker compose run --rm --no-deps sqlserver-init 'bash /scripts/check-seed.sh'
docker compose --profile tools run --rm backend-tests
```

Le premier test exige une nouvelle base dont le nom commence par AtlasSeedChecks.
Il utilise small par defaut, verifie les invariants et les defaults des trois
updated_at, compare les empreintes avant/apres une nouvelle initialisation et
apres un reseed explicite. La base est conservee pour inspection ; utiliser
`-e SEED_DATABASE=AtlasSeedChecks2` pour repeter le test dans une nouvelle base.
CHECKSUM_AGG est un controle de regression, pas une preuve cryptographique.
Les tests backend restent sur BusinessDB small et verifient les nouvelles colonnes.

La verification d'une base existante se fait avec :

```powershell
docker compose run --rm --no-deps -e SEED_DATABASE=BusinessBenchmark -e SEED_SCALE=large sqlserver-init 'bash /scripts/test-seed.sh'
```

Les configurations invalides sont refusees avant connexion SQL. Un reseed sans
confirmation est refuse sans effacer de lignes. Les deux FK restent actives
et fiables apres les tests de reseed.
