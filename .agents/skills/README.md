# Skills Atlas — feature/partitioning-csv

26 skills de fonctionnalités et de maintenance. Leur présence ne signifie pas que chaque fonctionnalité est implémentée dans cette branche historique. Lire d’abord [le contexte de cette branche](atlas-maintenance-flow/references/branche.md).

| Skill | Rôle |
|---|---|
| [atlas-api](atlas-api/SKILL.md) | Contrats API Atlas |
| [atlas-auth](atlas-auth/SKILL.md) | Authentification |
| [atlas-benchmarks](atlas-benchmarks/SKILL.md) | Benchmarks reproductibles |
| [atlas-catalog](atlas-catalog/SKILL.md) | Catalogue Hive et enregistrement |
| [atlas-connection](atlas-connection/SKILL.md) | Connexions relationnelles |
| [atlas-connectiondiagnostics](atlas-connectiondiagnostics/SKILL.md) | Diagnostic des connexions |
| [atlas-connectors](atlas-connectors/SKILL.md) | Plugins JDBC |
| [atlas-deployment](atlas-deployment/SKILL.md) | Docker et cible cloud |
| [atlas-exploration](atlas-exploration/SKILL.md) | Exploration relationnelle |
| [atlas-frontend](atlas-frontend/SKILL.md) | Interface Angular |
| [atlas-glossary](atlas-glossary/SKILL.md) | Glossaire métier |
| [atlas-health](atlas-health/SKILL.md) | Santé du service |
| [atlas-iceberg](atlas-iceberg/SKILL.md) | Tables Iceberg — en cours |
| [atlas-job-retry](atlas-job-retry/SKILL.md) | Relance des jobs — en cours |
| [atlas-jobs](atlas-jobs/SKILL.md) | Jobs d’extraction |
| [atlas-live-architecture](atlas-live-architecture/SKILL.md) | Architecture live locale — prototype |
| [atlas-maintenance-flow](atlas-maintenance-flow/SKILL.md) | Workflow de maintenance Atlas |
| [atlas-nativeingestion](atlas-nativeingestion/SKILL.md) | Extraction Redis et ClickHouse |
| [atlas-nativesources](atlas-nativesources/SKILL.md) | Exploration des sources natives |
| [atlas-presentation](atlas-presentation/SKILL.md) | Expliquer et présenter Atlas |
| [atlas-quality](atlas-quality/SKILL.md) | Qualité des données |
| [atlas-spark](atlas-spark/SKILL.md) | Exécution Spark |
| [atlas-spark-resources](atlas-spark-resources/SKILL.md) | Ressources Spark configurables — en cours |
| [atlas-sql](atlas-sql/SKILL.md) | Console SQL Trino |
| [atlas-storage](atlas-storage/SKILL.md) | Stockage MinIO |
| [atlas-timestamp-precision](atlas-timestamp-precision/SKILL.md) | Précision des timestamps — en cours |

## API

[Skill API](atlas-api/SKILL.md) : toutes les entrées, sorties, validations, statuts et erreurs. [Contrats détaillés du snapshot complet](atlas-api/references/contrats-api.md) ; [inventaire propre à cette branche](atlas-api/references/routes-branche.json).

Le guide personnel de révision, credentials, sauvegardes et work/ ne sont pas publiés. La fonctionnalité live et les autres travaux en stash restent locaux. Les skills n’autorisent pas de migration, de suppression ou de charge supplémentaire.
