# Integration preparee, sans fusion

La livraison est preparee sur `feature/release-readiness`, base
`feature/data-quality-report`. Les branches restent publiees separement.
La branche par defaut n'est pas modifiee par cette preparation.

## Ordre de dependance

| Branche | Base de revue |
| --- | --- |
| feature/large-dataset | master |
| feature/partitioning-csv | feature/large-dataset |
| feature/hive-catalog-trino | feature/partitioning-csv |
| feature/partition-candidates | feature/hive-catalog-trino |
| feature/angular-jobs-sql | feature/partition-candidates |
| feature/wireframe-frontend | feature/angular-jobs-sql |
| feature/jdbc-connector-plugins | feature/wireframe-frontend |
| feature/postgresql-connector | feature/jdbc-connector-plugins |
| feature/connector-aware-frontend | feature/postgresql-connector |
| feature/redis-analytics | feature/connector-aware-frontend |
| feature/native-source-connections | feature/redis-analytics |
| feature/native-source-interface | feature/native-source-connections |
| feature/reproducible-benchmarks | feature/native-source-interface |
| feature/data-quality-report | feature/reproducible-benchmarks |
| feature/release-readiness | feature/data-quality-report |
| feature/business-glossary | feature/release-readiness |
| feature/business-glossary-ui | feature/business-glossary |
| feature/explicit-source-selection | feature/business-glossary-ui |
| feature/platform-authentication | feature/explicit-source-selection |
| feature/source-test-progress | feature/platform-authentication |

L'extension business-glossary est posterieure a la validation de livraison :
son backend peut etre revu separement, voir [le guide du glossaire](business-glossary.md).
La branche suivante business-glossary-ui ajoute l'ecran Angular et la navigation
depuis les tables/colonnes ; voir [son guide](business-glossary-ui.md).

Chaque branche contient ses dependances. Il ne faut pas cherry-pick des
fonctionnalites dans un ordre arbitraire ni rejouer leurs commits deja inclus.
Une revue globale de release-readiness vers master permet aussi de conserver
ces commits et les branches de fonctionnalites existantes.

## Verification avant integration

Les preuves de livraison sont dans [le rapport de validation](release-validation.md),
[le rapport qualite](data-quality.md) et [les benchmarks](benchmark-report.md).
Verifier le diff, les secrets exclus de Git et la chaine d'ancetres :

```powershell
git fetch origin
git log --oneline origin/master..origin/feature/release-readiness
git diff --stat origin/master...origin/feature/release-readiness
git merge-base --is-ancestor origin/master origin/feature/release-readiness
```

Un retour zero de la derniere commande confirme la possibilite d'une avance
rapide a cet instant. Si master avance entre-temps, reevaluer les conflits
et les tests ; ne pas forcer la publication.

## Action reservee a une demande de fusion

Les commandes suivantes sont documentees pour la revue, pas executees :

```powershell
git switch master
git merge --ff-only origin/feature/release-readiness
git push origin master
```

Une integration ne requiert ni la suppression des branches, ni un reseed,
ni la suppression des volumes. Le prototype local d'architecture reste
hors de cette livraison ; le stash qui le conserve n'est pas applique.
