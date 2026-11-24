# Finaliser le parcours analytique local et ses preuves de validation

La branche par defaut contient le premier socle d'ingestion. Cette livraison
ajoute les formats et partitions, le catalogue Hive/Trino, le parcours Angular,
les plugins SQL Server/PostgreSQL, l'exploration native Redis/MinIO, les
benchmarks et la reconciliation qualite des donnees.

Le parcours attendu est : choisir une source et une table, lancer une
extraction, suivre son statut, puis interroger la table enregistree dans Trino.
Un traitement n'est COMPLETED qu'apres verification des fichiers et du catalogue.

## Validation a examiner

- [Validation de livraison](release-validation.md) : tests Docker, premier
  demarrage sur volumes neufs, conservation de l'environnement principal et
  controle navigateur du parcours Angular.
- [Qualite](data-quality.md) : 92 controles passes, 1000 clients, 5000 factures
  et 8000 paiements reconcilies sur des extractions immuables.
- [Benchmarks](benchmark-report.md) : 24 extractions, 216 requetes mesurees,
  trois repetitions et preuves brutes conserves.
- [Guide de demonstration](demo-guide.md) et [guide de livraison](delivery.md).

## Limites connues

Livraison pedagogique locale : pas d'authentification utilisateur/API complete,
pas de creation permanente des nouvelles sources depuis l'interface et pas
de deploiement cloud valide. Le glossaire reste differe ; Iceberg, incremental,
evolution de schema et relance des jobs en echec restent exclus.
Redis propose connexion/exploration et SQL via son catalogue ; son extraction
vers Parquet n'est pas annoncee. Les autres moteurs relationnels necessitent
leur plugin, pilote et tests.

Les branches sont empilees selon [le plan d'integration](integration-plan.md).
Cette description est prete pour la revue ; aucune fusion n'est realisee.
