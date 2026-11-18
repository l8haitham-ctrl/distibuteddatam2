# Suite du projet

## Ordre de realisation

Le socle Docker, les connexions SQL Server, l'exploration et l'interface Angular
sont deja implementes. Chaque nouvelle fonctionnalite conserve sa branche,
ses tests et sa documentation avant d'etre poussee ; aucune fusion automatique.

| Etape | Fonctionnalite | Resultat attendu | Etat |
| --- | --- | --- | --- |
| 1 | Stockage MinIO | Bucket prive persistant, compte limite, service Java par flux, tests reels | Implemente sur `feature/minio-storage` |
| 2 | Jobs persistants | Creer et suivre un traitement avec statuts, dates, erreurs et resultat ; conserver l'etat apres redemarrage | A faire |
| 3 | Extraction par lots | Lire une selection SQL Server avec ordre stable et memoire bornee ; suivre les lignes traitees | A faire |
| 4 | Parquet et depot | Convertir les types correctement, ecrire des fichiers de taille controlee, envoyer vers MinIO et produire un manifeste | A faire |
| 5 | Partitionnement et compression | Organiser les extractions par source/table/job puis partitions adaptees ; comparer sans surpartitionner | A faire |
| 6 | Catalogue technique | Enregistrer schemas, emplacements et partitions dans un catalogue compatible Trino | A faire |
| 7 | Acces SQL Trino | Lire les donnees deposees, verifier les jointures Customer/Invoice/Payment et les droits de lecture | A faire |
| 8 | Parcours Angular complet | Selectionner, lancer, suivre un job, consulter le resultat et executer une requete analytique via l'API | A faire |
| 9 | Qualite et glossaire | Verifier completude, types, cles et montants ; relier les definitions metier aux tables et colonnes | A faire plus tard |
| 10 | Benchmarks | Comparer CSV/Parquet, partitions, compression et lots avec les memes donnees et requetes ; presenter des mesures reelles | A faire |
| 11 | Preparation de livraison | Parcours complet reproductible, documentation, secrets et adaptation Azure/AWS | A faire |

La prochaine branche prevue est `feature/storage-jobs`. La persistance des
jobs appartient aux metadonnees de l'application, distinctes de la base source
et des fichiers MinIO. Une API ne doit pas annoncer un job termine avant que
les fichiers et son manifeste soient valides. La relance apres erreur et les
chargements incrementaux seront ajoutes ensuite.

Le flux cible reste :

```text
Angular -> API Spring Boot -> job -> SQL Server (lots)
                                -> conversion Parquet -> MinIO
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

Les jobs d'extraction par lots, Parquet/CSV, partitions, catalogue, Trino et
mesures comparables restent a implementer. MinIO et le service Java de
stockage constituent maintenant le premier composant de ce pipeline.

## Bonus apres le parcours principal

Apache Iceberg, chargement incremental, evolution de schema, statistiques,
relance des traitements en echec et comparaison de tables Iceberg.
