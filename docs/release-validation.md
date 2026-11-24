# Validation de la livraison locale

Validation realisee les 23 et 24 novembre 2026 sur Windows et Docker Desktop,
application issue de `9e58927`, puis correctifs de demarrage et outils de
livraison sur `feature/release-readiness`. Aucun merge vers master.

## Resultats et preuves

| Verification | Resultat | Preuve |
| --- | --- | --- |
| Qualite source / extractions immuables | 92 controles passes ; 1000 clients, 5000 factures, 8000 paiements | [Rapport qualite](data-quality.md) et [JSON](../validation/results/data-quality-2026-11-23.json) |
| Backend avec integrations reelles | 84 cas distincts verifies, dont un apres relance ciblee | [Synthese des tests](../validation/results/test-summary-2026-11-23.json) |
| Angular | 36 tests passes dans 10 fichiers, execution Docker | [Synthese des tests](../validation/results/test-summary-2026-11-23.json) |
| Premier demarrage sur volumes neufs | 17 controles passes, extraction et lecture SQL de 5000 factures | [Controle fonctionnel](../validation/results/fresh-install-2026-11-23.json) |
| Conservation de l'installation principale | 43 jobs, huit volumes et configuration conserves ; qualite encore valide | [Controle d'installation](../validation/results/installation-check-2026-11-23.json) |
| Job lance depuis Angular | Termine ; 48 fichiers Parquet, 201538 octets, inscription au catalogue | [Controle API/SQL, 15 verifications](../validation/results/main-ui-flow-2026-11-24.json) |
| Requete metier depuis Angular | Jointure clients/factures/paiements : trois lignes de resultat | [Capture SQL](evidence/final-sql.jpg) |
| Services apres restauration et parcours UI | 11 services actifs et healthy | [Etat Docker](../validation/results/release-health-2026-11-24.json) |

La suite backend initiale compte 84 tests, zero erreur et zero test ignore,
avec un echec : `JobIntegrationTests.realSparkWritesReadableParquetAndManifest`.
Le journal du driver `driver-20261123233431-0030` montre un timeout de lecture
pendant la negociation TLS SQL Server, avant l'extraction, alors que les tests
frontend tournaient aussi. Le meme cas, rejoue seul, passe. Les 83 autres cas
avaient passe initialement : il ne s'agit donc pas d'une execution unique de
84 tests tous verts. Aucun mecanisme de relance automatique n'a ete ajoute.

## Premier demarrage et correctifs

Le premier essai sur volumes vierges a revele que `pg_isready` via le socket
local pouvait valider le serveur temporaire utilise par l'image PostgreSQL
pendant son initialisation, avant la disponibilite TCP attendue par les autres
services. L'initialisation du metastore echouait alors par connexion refusee.
[L'echec initial est conserve](../validation/results/installation-check-initial-2026-11-23.json).

Les sondes PostgreSQL metadata et source utilisent maintenant TCP sur
127.0.0.1. La sonde Spark attend explicitement `status=ALIVE`, y compris apres
la recuperation de son historique ; une simple reponse HTTP ne suffit pas.
La seconde installation et l'extraction ont passe avec ces sondes.

L'essai utilise le projet Docker separe `atlas-release-check-20261123`, des
secrets nouvellement generes et quatre volumes neufs. Les images construites
localement sont reutilisees : ce n'est pas un test sur une machine sans cache
d'images. L'installation principale a ete arretee temporairement sans supprimer
ses volumes, puis restauree avec ses overrides PostgreSQL, Redis et benchmarks.
Seuls les quatre volumes du projet de test, verifies par nom et label, ont
ete supprimes. Les fichiers et le job de cet environnement jetable ne sont
donc plus disponibles ; leurs mesures restent dans le rapport JSON.

Le script [setup-env.ps1](../scripts/setup-env.ps1) a ete execute avec Windows
PowerShell : il produit 16 secrets distincts sans les afficher et preserve
un fichier existant. Le hash de la configuration principale est reste identique.

## Parcours navigateur

Depuis Angular : test de connexion SQL Server, ouverture de dbo.Invoice,
verification des colonnes et de FK_Invoice_Customer, puis lancement d'une
extraction Parquet/Snappy par mois, fetch_size=1000 et deux lecteurs JDBC.
Le job `6c145cbe-5ac9-441f-8d76-c003e8496266` atteint COMPLETED en 124 secondes.
Sa table est `hive.runs.invoice_6c145cbe` ; sa [capture](evidence/final-job.jpg)
montre les fichiers, l'emplacement MinIO et l'inscription au catalogue.

La console SQL a ensuite execute la requete du [guide de demonstration](demo-guide.md),
en agregeant les paiements par facture avant la jointure, pour eviter de
multiplier le montant d'une facture ayant plusieurs paiements.

| Statut | Factures | Montant facture | Montant paye |
| --- | ---: | ---: | ---: |
| paid | 3500 | 3421705.00 | 3421705.00 |
| partial | 1000 | 983795.00 | 491900.00 |
| unpaid | 500 | 486925.00 | 0.00 |
| Total | 5000 | 4892425.00 | 3913605.00 |

Le temps affiche pour cette execution est 2231 ms. Cette mesure unique sert
au controle fonctionnel ; les performances sont comparees dans la campagne
dediee de [benchmarks](benchmark-report.md).

## Reproduire les controles

Suivre [le guide de livraison](delivery.md) pour construire et demarrer les
services. Lancer les suites sequentiellement pour limiter la contention locale.
Avec les sources optionnelles et les jobs qualite prepares :

```powershell
$customerJob = 'UUID-du-job-Customer-termine'
$invoiceJob = 'UUID-du-job-Invoice-termine'
$paymentJob = 'UUID-du-job-Payment-termine'
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml --profile tools run --rm --no-deps -e RUN_QUALITY_TESTS=true -e "QUALITY_CUSTOMER_JOB=$customerJob" -e "QUALITY_INVOICE_JOB=$invoiceJob" -e "QUALITY_PAYMENT_JOB=$paymentJob" backend-tests mvn -B test
docker build --target build -t distributed-data-frontend-tests:local frontend
docker run --rm --entrypoint npm distributed-data-frontend-tests:local test
```

Les UUID doivent provenir de l'installation testee et de la meme source.
Les identifiants historiques des rapports ne doivent pas etre copies dans une
base neuve. Conserver les overrides deja actifs, dont benchmarks si utilise.
Pour verifier un job existant sans lancer une autre extraction :

```powershell
docker compose -f docker-compose.yml -f docker-compose.validation.yml --profile tools run --rm --no-deps validation --run-id=verification-job --job-id=UUID-du-job-Invoice
```

Le controle suppose le jeu small sans modification : 5000 factures et
4892425.00 au total. Le drapeau `--require-empty` est reserve aux volumes
neufs. Il n'efface pas l'installation et echoue si des jobs existent deja.

## Portee

Livraison pedagogique locale validee, avec donnees fictives. Les controles
qualite comparent des agregats ; ils ne prouvent ni un checksum cellule par
cellule, ni un snapshot transactionnel commun entre source et lake.
L'authentification multiutilisateur, la creation permanente des sources depuis
l'UI et un deploiement Azure/AWS ne sont pas valides dans cette livraison.
Les autres moteurs relationnels exigent un plugin, un pilote et leurs tests.
Redis ne fournit pas d'extraction Parquet. Le glossaire metier reste differe ;
les bonus restent exclus. Voir [le plan d'integration](integration-plan.md)
pour la revue des branches ; aucune fusion n'est executee.
