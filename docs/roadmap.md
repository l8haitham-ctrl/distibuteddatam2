# Suite du projet

## Ordre de realisation

Le socle Docker, les connexions SQL Server, l'exploration et l'interface Angular
sont deja implementes. Chaque nouvelle fonctionnalite conserve sa branche,
ses tests et sa documentation avant d'etre poussee ; aucune fusion automatique.

Le [generateur de volumes](large-dataset.md), branche `feature/large-dataset`,
ajoute les profils small/large, updated_at et un reseed explicite. Le profil
large a ete genere et verifie dans une base separee ; BusinessDB small reste
la source des tests. Les mesures figurent dans README.md. La prochaine branche
du parcours demande est `feature/partitioning-csv`.

| Etape | Fonctionnalite | Resultat attendu | Etat |
| --- | --- | --- | --- |
| 1 | Stockage MinIO | Bucket prive persistant, compte limite, service Java par flux, tests reels | Implemente sur `feature/minio-storage` |
| 2 | Jobs persistants | Creer et suivre un traitement avec statuts, dates, erreurs et resultat ; conserver l'etat apres redemarrage | Implemente sur `feature/storage-jobs` |
| 3 | Extraction par lots | Lecture JDBC Spark bornee ; ajouter selection, lecture repartie et progression detaillee | Premiere version complete de table ; optimisations a faire |
| 4 | Parquet et depot | Ecrire dans MinIO, borner les fichiers et publier un manifeste | Premiere version Spark implementee ; CSV a faire |
| 5 | Partitionnement et compression | Organiser les extractions par source/table/job puis partitions adaptees ; comparer sans surpartitionner | A faire |
| 6 | Catalogue technique | Enregistrer schemas, emplacements et partitions dans un catalogue compatible Trino | A faire |
| 7 | Acces SQL Trino | Lire les donnees deposees, verifier les jointures Customer/Invoice/Payment et les droits de lecture | A faire |
| 8 | Parcours Angular complet | Selectionner, lancer, suivre un job, consulter le resultat et executer une requete analytique via l'API | A faire |
| 9 | Qualite et glossaire | Verifier completude, types, cles et montants ; relier les definitions metier aux tables et colonnes | A faire plus tard |
| 10 | Benchmarks | Comparer CSV/Parquet, partitions, compression et lots avec les memes donnees et requetes ; presenter des mesures reelles | A faire |
| 11 | Preparation de livraison | Parcours complet reproductible, documentation, secrets et adaptation Azure/AWS | A faire |

La branche `feature/storage-jobs` utilise PostgreSQL pour l'historique et
Spark pour l'ingestion, sans Spring Batch. La suite porte sur la selection de
donnees, la lecture JDBC repartie et le partitionnement de stockage. Une API
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

## Glossaire metier (demande utilisateur, a implementer plus tard)

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
implementes. CSV, selection avancee, partitions, catalogue, Trino et mesures
comparables restent a implementer.

## Bonus apres le parcours principal

Apache Iceberg, chargement incremental, evolution de schema, statistiques,
relance des traitements en echec et comparaison de tables Iceberg.
