# Suite du projet

## Ordre de realisation

Le socle Docker, les connexions SQL Server, l'exploration et l'interface Angular
sont deja implementes. Chaque nouvelle fonctionnalite conserve sa branche,
ses tests et sa documentation avant d'etre poussee ; aucune fusion automatique.

Le [generateur de volumes](large-dataset.md), branche `feature/large-dataset`,
ajoute les profils small/large, updated_at et un reseed explicite. Le profil
large a ete genere et verifie dans une base separee ; BusinessDB small reste
la source des tests. Les mesures figurent dans README.md. La branche
du parcours demande `feature/angular-jobs-sql` ajoute le parcours complet, apres le catalogue et Trino
de `feature/hive-catalog-trino`, basee sur `feature/partitioning-csv`.

| Etape | Fonctionnalite | Resultat attendu | Etat |
| --- | --- | --- | --- |
| 1 | Stockage MinIO | Bucket prive persistant, compte limite, service Java par flux, tests reels | Implemente sur `feature/minio-storage` |
| 2 | Jobs persistants | Creer et suivre un traitement avec statuts, dates, erreurs et resultat ; conserver l'etat apres redemarrage | Implemente sur `feature/storage-jobs` |
| 3 | Extraction par lots | Lecture JDBC Spark par lots avec 1 a 16 partitions de lecture ; selection avancee et progression detaillee futures | Lecture parallele implementee sur feature/partitioning-csv |
| 4 | Parquet et depot | Ecrire dans MinIO, borner les fichiers et publier un manifeste | CSV/Parquet implementes sur feature/partitioning-csv |
| 5 | Partitionnement et compression | Parquet snappy/zstd/sans compression ; CSV gzip/sans compression ; dates annee/mois/jour et colonnes de faible cardinalite | Implemente sur feature/partitioning-csv |
| 6 | Catalogue technique | Enregistrer schemas, emplacements et partitions dans Hive Metastore/PostgreSQL | Implemente sur feature/hive-catalog-trino |
| 7 | Acces SQL Trino | Lire les donnees deposees, verifier les jointures Customer/Invoice/Payment et les droits de lecture | Implemente sur feature/hive-catalog-trino |
| 8 | Parcours Angular complet | Selectionner, lancer, suivre un job, consulter le resultat et executer une requete analytique via l'API | Implemente sur feature/angular-jobs-sql |
| 9 | Qualite et glossaire | Verifier completude, types, cles et montants ; relier les definitions metier aux tables et colonnes | Qualite implementee ; backend du glossaire sur `feature/business-glossary`, interface Angular a suivre |
| 10 | Benchmarks | Comparer CSV/Parquet, partitions, compression et lots avec les memes donnees et requetes ; presenter des mesures reelles | Campagne locale 100k terminee sur `feature/reproducible-benchmarks` : 24 extractions, 216 requetes mesurees, rapport et preuves |
| 11 | Preparation de livraison | Parcours complet reproductible, documentation, secrets et adaptation Azure/AWS | Livraison locale validee sur `feature/release-readiness` : Docker neuf, UI et SQL ; cible cloud documentee, non deployee |

La branche `feature/storage-jobs` utilise PostgreSQL pour l'historique et
Spark pour l'ingestion, sans Spring Batch. Le catalogue Hive/Trino, les ecrans
Angular et la campagne de benchmarks sont maintenant implementes. Une API
ne doit pas annoncer un job termine avant que les fichiers et son manifeste
soient valides. La relance apres erreur et les chargements incrementaux seront
ajoutes ensuite. Le [guide des jobs](storage-jobs.md) precise les limites actuelles.

Le flux cible reste :

```text
Angular -> API Spring Boot -> PostgreSQL (jobs) -> Spark -> SQL Server (lots)
                                                     -> Parquet -> MinIO
                                -> catalogue -> Trino -> resultat SQL -> Angular
```

Le catalogue decrit les donnees ; Trino lit les fichiers depuis MinIO en
consultant ce catalogue. Le depot de fichiers seul ne cree pas une table SQL.

## Glossaire metier (implementation demandee)

Le backend est implemente sur `feature/business-glossary`, base
`feature/release-readiness` : termes persistants PostgreSQL, recherche,
statuts, regles de calcul textuelles et liens verifies vers tables/colonnes
des sources configurees. Voir [le guide](business-glossary.md).
L'interface Angular et la navigation depuis l'explorateur restent a developper.
La liste suivante conserve les objectifs restants, dont synonymes et validation
metier, au-dela de cette premiere API.

- Definir des termes : chiffre d'affaires, facture impayee, paiement partiel, client actif.
- Relier chaque terme a une ou plusieurs tables et colonnes, avec le schema et
  l'identifiant de source pour eviter les ambiguites.
- Conserver definition, synonymes, responsable, regle de calcul et date de validation.
- Afficher les termes associes dans l'explorateur technique et retrouver les
  tables depuis le glossaire.
- Distinguer une definition metier validee d'une relation technique declaree.
- Prevoir une persistance dediee pour ces metadonnees ; ne pas modifier les bases sources.

## Pipeline analytique

MinIO, l'historique PostgreSQL et le premier parcours Spark/Parquet sont
implementes. La branche feature/partitioning-csv ajoute CSV, compression, partitions et lecteurs
JDBC paralleles. La branche feature/hive-catalog-trino ajoute le metastore,
Trino, les tables par execution, les vues et l'API SQL. La selection avancee
reste a implementer ; les mesures comparables figurent dans [le rapport](benchmark-report.md).
Le parcours Angular est implemente ; voir [le guide](angular-pipeline.md).

## Bonus apres le parcours principal

Apache Iceberg, chargement incremental, evolution de schema, statistiques,
relance des traitements en echec et comparaison de tables Iceberg.

## Catalogue Hive et Trino

`feature/hive-catalog-trino`, base `feature/partitioning-csv`, ajoute les API de
catalogue/SQL et le statut REGISTERING, avec tests de lecture et de droits. Voir [catalogue et SQL](catalog-trino.md).
Le wireframe fourni sert de reference au parcours Angular ; l'interface du
glossaire metier et la navigation depuis les tables/colonnes restent a developper.

## Prochaine etape

La branche feature/angular-jobs-sql depend explicitement de
feature/partition-candidates, qui expose les colonnes partitionnables validees
sur SQL Server. La comparaison des performances est maintenant realisee
sur 100 000 factures : voir [le rapport](benchmark-report.md).
La livraison locale et la validation finale sont terminees : voir
[le rapport de validation](release-validation.md) et [le guide de livraison](delivery.md).
La prochaine action est la revue des branches selon [le plan d'integration](integration-plan.md) ;
la fusion attend une demande explicite.
Les bonus restent exclus ; l'interface du glossaire suit sa premiere feature backend.

L'adaptation visuelle du PDF est implementee sur `feature/wireframe-frontend`,
base `feature/angular-jobs-sql` : Data Sources, formulaire pleine page,
navigation, stockage et habillage commun. Voir [le guide du wireframe](wireframe-frontend.md).
La creation permanente de sources et les etapes JDBC en direct restent des
evolutions backend distinctes ; l'exploration temporaire reste disponible.

## Extensibilite relationnelle

Le socle `feature/jdbc-connector-plugins` definit une SPI partagee entre l'API
et Spark et decouverte par ServiceLoader. `feature/postgresql-connector`
ajoute PostgreSQL comme source metier reelle, avec les memes API, des tests
croises et des extractions Parquet/CSV relues dans Trino. L'interface
`feature/connector-aware-frontend` charge les moteurs disponibles depuis
GET /connectors. SQL Server demeure le premier cas d'utilisation, pas une
restriction de l'architecture. Les autres moteurs requierent leur fournisseur,
leur pilote JDBC et une validation sur une base reelle.

Un test analytique optionnel sur `feature/redis-analytics` ajoute un catalogue
Redis natif a Trino et une jointure avec les factures Parquet de MinIO. Il
ne remplace pas les sources relationnelles et ne fournit pas d'ingestion Spark
Redis. Voir [le guide de test](redis-analytics.md). La campagne locale de benchmarks
est terminee ; le backend du glossaire est ajoute separement, son interface reste a suivre.

`feature/native-source-connections` puis `feature/native-source-interface`
ajoutent la connexion/exploration native Redis et MinIO et leur formulaire
Angular. Les metadonnees de session ne contiennent aucun mot de passe ; cette
extension ne fournit pas d'extraction Redis vers Parquet ni d'enregistrement
automatique de fichiers MinIO arbitraires dans Trino.
