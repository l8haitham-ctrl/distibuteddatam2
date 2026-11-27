---
name: atlas-presentation
description: "Préparer une explication simple d’Atlas, une visite de sa codebase ou une présentation au professeur en distinguant réalisations, mesures et travaux en cours."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Expliquer et présenter Atlas

Lire [support de présentation versionné](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/docs/presentation-professeur.md) ; pour les détails techniques, suivre ses liens vers les fichiers du projet. Utiliser le vocabulaire français de l’utilisateur, expliquer d’abord le besoin métier, puis le trajet d’une facture. Ne pas commencer par une liste de conteneurs.

Relier chaque technologie à son rôle : Angular interaction, Spring Boot contrôle, PostgreSQL métadonnées, source données métier, Spark extraction/transformation, MinIO objets, Parquet format, Hive Metastore catalogue, Trino SQL, Docker environnement. Hive Metastore n’exécute pas les requêtes et Spark ne remplace pas Trino.

Pour une visite de code, partir du clic et suivre routes Angular → client HTTP → controller → service → connector → dépendance. Montrer JobDispatcher, SparkJobEngine, IngestionJob et CatalogService dans cet ordre pour expliquer une extraction. Lire le code actuel avant d’annoncer un nom de classe ou une capacité.

Utiliser seulement les résultats historiques vérifiés pour les benchmarks ; préciser jeu de 100000 factures et un cœur executor. Partitionnement efficace pour les lignes scannées ne garantit pas temps inférieur sur petit volume. Ne pas dire que les bonus Iceberg/retry ou le multiworker sont finis parce qu’une branche/stash existe. Les preuves historiques ne garantissent pas que Docker est sain aujourd’hui.

Pour « donne-moi les API », appliquer [atlas-api](../atlas-api/SKILL.md) et développer toutes les entrées/sorties. Les skills sont publiés par demande explicite ; le guide personnel dans work/ reste local et ne doit pas être publié.
