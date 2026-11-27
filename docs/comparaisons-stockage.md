# Comprendre les benchmarks d'Atlas

Ce document explique les **quatre comparaisons demandées par le professeur** et
les résultats de la campagne enregistrée `local-2026-11-22-100k`.
Il décrit des expériences déjà exécutées, pas une nouvelle campagne ni des
performances estimées. Une nouvelle exécution devra posséder ses propres
identifiants, résultats et conditions matérielles.

## 1. Ce que nous cherchons à comparer

Le sujet demande de mesurer l'effet des choix de format, de stockage et de lecture :

| Axe du sujet | Expériences réalisées | Question posée |
|---|---|---|
| CSV contre Parquet | `csv_raw` et `parquet_raw` | Quel est l'effet du format, sans compression ? |
| Partitionné contre non partitionné | `parquet_month` et `parquet_snappy` | Organiser par année/mois réduit-il les lectures et le temps SQL ? |
| Avec et sans compression | CSV brut/gzip ; Parquet brut/Snappy/Zstd | Quel gain de stockage et quel coût de traitement ? |
| Différentes tailles de lots | Parquet Snappy, fetch 100/1 000/10 000 | Le réglage de récupération JDBC modifie-t-il la durée d'ingestion ? |

Les scénarios de chaque comparaison utilisent la même table source. Le format,
le codec, les colonnes de partition et le fetch size sont fixés selon le plan.
Nous ne comparons pas des données métier différentes.

## 2. Données et conditions de l'expérience

La source est `BusinessBenchmark.benchmark.Invoice100k`, dans SQL Server :

- **100 000 factures synthétiques**, six colonnes ;
- dates du 1er janvier 2025 au 31 décembre 2026 ;
- somme des montants : **104 634 500,00** ;
- somme des identifiants : **5 000 050 000** ;
- janvier 2025 : **4 247 factures**, montant **2 442 323,84** ;
- statuts : **70 000 paid**, **20 000 partial**, **10 000 unpaid**.

L'environnement conservé dans le rapport expose huit CPU logiques et environ
7,64 Gio de RAM à Docker. Chaque application Spark est cependant limitée à
**un cœur d'exécuteur**, avec des heaps driver et exécuteur de 512 Mio.
Deux partitions JDBC sont demandées et `maxRecordsPerFile=100000` reste fixe.

**Deux partitions logiques ne prouvent pas deux lectures simultanées.** Cette
campagne compare des stratégies de stockage sur la configuration locale ; elle
ne démontre pas un gain multicœur ou multiworker.

## 3. Comment les mesures ont été obtenues

Le runner soumet de vraies extractions à l'API, attend la publication du résultat,
puis interroge les tables enregistrées dans Trino. Il conserve les identifiants
des jobs, les paramètres, les requêtes, les temps et les réponses.

Le protocole comprend :

1. huit scénarios ;
2. trois répétitions d'extraction par scénario, avec rotation déterministe de l'ordre ;
3. trois types de requêtes : agrégat global, filtre janvier et regroupement par statut ;
4. pour chaque requête de chaque extraction : un passage de warmup puis trois mesures ;
5. une comparaison des résultats avec les agrégats de référence.

Cela représente :

```text
8 scénarios × 3 répétitions = 24 extractions mesurées
24 extractions × 3 requêtes × 3 mesures = 216 requêtes SQL mesurées
24 extractions × 3 requêtes × 1 warmup = 72 requêtes SQL d'échauffement
```

Une extraction supplémentaire d'échauffement est exclue des 24 extractions
mesurées. Les warmups SQL sont aussi exclus des médianes.

### Pourquoi afficher une médiane ?

Une exécution peut subir un démarrage plus lent, une charge concurrente ou un
effet de cache. La médiane limite l'influence d'une valeur extrême, sans faire
disparaître toutes les variations.

- Les durées d'extraction du tableau sont les **médianes de trois extractions**.
- Chaque durée SQL est la **médiane de neuf mesures pour cette requête et ce
  scénario** : trois mesures dans chacune des trois extractions.

Ce n'est donc ni une mesure unique, ni une moyenne de toutes les requêtes mélangées.

## 4. Résultats réellement enregistrés

Les tailles ci-dessous sont exprimées en **Mio** : `octets / 1 048 576`.
Elles concernent les fichiers de données, sans le manifeste ni `_SUCCESS`.

| Scénario | Taille en Mio | Fichiers | Lecture + conversion + écriture, s | Vérification, s | Catalogue, s | SQL global, ms | SQL janvier, ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| CSV brut | 5,290 | 2 | 19,121 | 2,436 | 0,623 | 159 | 580 |
| CSV gzip | 1,251 | 2 | 17,630 | 2,716 | 1,297 | 297 | 476 |
| Parquet brut | 1,793 | 2 | 18,103 | 3,195 | 0,681 | 165 | 144 |
| Parquet Snappy | 1,230 | 2 | 19,724 | 3,705 | 1,051 | 222 | 229 |
| Parquet Zstd | 0,690 | 2 | 18,607 | 3,794 | 1,056 | 138 | 197 |
| Parquet mensuel, Snappy | 1,248 | 48 | 24,673 | 5,104 | 1,430 | 308 | 246 |
| Parquet Snappy, fetch 100 | 1,230 | 2 | 16,977 | 3,111 | 0,811 | 137 | 153 |
| Parquet Snappy, fetch 10 000 | 1,230 | 2 | 17,138 | 2,589 | 0,679 | 183 | 225 |

Les valeurs exactes en octets et les autres colonnes figurent dans le
[tableau CSV de la campagne](../benchmarks/results/local-2026-11-22-100k-summary.csv).

### Ce que chaque durée signifie

| Mesure | Ce qui est mesuré | Limite |
|---|---|---|
| `extract_write_ms` | Phase combinée lecture JDBC, conversion et écriture MinIO | Ces activités se chevauchent ; elles ne sont pas isolées |
| `verify_ms` | Relecture et contrôle des fichiers produits | Ce n'est pas le temps d'upload |
| `registration_ms` | Enregistrement et vérification de la table dans le catalogue via Trino | Ce n'est pas le temps de transfert S3 |
| Temps SQL | Exécution de requêtes, avec identifiants et statistiques Trino | Après warmup ; aucun cache froid garanti |
| Pipeline serveur | Parcours du job, incluant les coûts de démarrage et de suivi | Les médianes de chaque phase ne s'additionnent pas forcément à cette médiane |

Le sujet demande des temps d'extraction et de chargement. Atlas mesure bien les
phases ci-dessus, mais **ne sépare pas encore lecture, transformation et upload**.
Il faut annoncer cette réserve au professeur plutôt que renommer le temps de
catalogue en « temps d'upload ».

## 5. Comparaison 1 — CSV contre Parquet

Nous commençons avec les variantes **sans compression**, afin d'observer
l'effet du format avant celui du codec :

| Mesure | CSV brut | Parquet brut |
|---|---:|---:|
| Octets | 5 547 450 | 1 880 314 |
| Taille en Mio | 5,290 | 1,793 |
| Lecture/conversion/écriture | 19,121 s | 18,103 s |
| SQL global | 159 ms | 165 ms |
| SQL janvier | 580 ms | 144 ms |

Parquet brut occupe ici **66,10 % d'espace en moins**. Son organisation colonnaire
et ses encodages apportent un gain avant même l'ajout d'un codec de compression.

La requête globale n'est pourtant pas plus rapide en Parquet dans ces médianes :
165 ms contre 159 ms. Le filtre janvier est, lui, plus rapide. Le résultat dépend
de la requête et de l'environnement ; aucune accélération universelle n'est prouvée.

**À dire à l'oral :** « Nous avons isolé l'effet du format en comparant deux
versions non compressées. Parquet réduit fortement le stockage, mais le gain
de temps SQL n'est pas systématique sur toutes les requêtes. »

## 6. Comparaison 2 — Avec et sans partitionnement

Nous conservons Parquet, Snappy et le fetch size de 1 000. Seule l'organisation
physique varie : aucun partitionnement, ou partitionnement par année/mois.

| Mesure | Non partitionné | Partitionné par mois |
|---|---:|---:|
| Taille en Mio | 1,230 | 1,248 |
| Fichiers | 2 | 48 |
| Lecture/conversion/écriture | 19,724 s | 24,673 s |
| Lignes traitées pour janvier | 100 000 | 4 247 |
| SQL janvier | 229 ms | 246 ms |

Le filtre sur les partitions réduit les lignes traitées de **95,753 %**.
C'est la preuve du **partition pruning**, c'est-à-dire l'évitement des partitions
non nécessaires au calcul.

Cependant, la latence médiane passe de 229 à 246 ms. Sur ce petit jeu, créer et
parcourir davantage de fichiers ajoute des coûts fixes. Les petits fichiers sont
une explication compatible avec l'observation ; leur coût n'a pas été isolé par
une mesure CPU spécifique.

### Condition importante pour le pruning mesuré

Les requêtes ciblent les tables de run. La variante partitionnée ajoute :

```sql
AND invoice_date_year = 2025
AND invoice_date_month = 1
```

Ces contraintes sont redondantes avec le filtre métier sur janvier, mais elles
permettent de cibler explicitement les partitions. Le test ne prouve pas que
Trino déduit toujours ces contraintes à partir du seul champ `invoice_date`,
notamment à travers la vue courante qui masque les colonnes dérivées.

**À dire à l'oral :** « Le partitionnement réduit réellement la lecture, mais
il n'accélère pas notre requête sur 100 000 lignes. Il faut dimensionner les
partitions et les fichiers selon le volume, pas partitionner systématiquement. »

## 7. Comparaison 3 — Avec et sans compression

Nous comparons le même format avec différents codecs :

| Format | Sans compression | Codec(s) comparé(s) |
|---|---:|---|
| CSV | 5,290 Mio | gzip : 1,251 Mio |
| Parquet | 1,793 Mio | Snappy : 1,230 Mio ; Zstd : 0,690 Mio |

Parquet Zstd utilise **723 561 octets** : c'est le plus petit stockage de cette
campagne, soit **86,96 % de moins que CSV brut** et **43,91 % de moins que
Parquet Snappy**.

Attention : la réduction par rapport au CSV brut combine l'effet du **format**
et celui du **codec**. Pour comparer seulement les codecs, il faut rester dans
une même famille, par exemple Parquet brut contre Parquet Zstd.

La compression peut modifier le travail CPU et le volume d'entrées/sorties,
mais cette campagne ne sépare pas ces coûts. CSV gzip apparaît même plus rapide
à ingérer dans sa médiane que CSV brut ; cela ne prouve pas que compresser est
toujours plus rapide. Les durées et leurs variations doivent être interprétées
avec les conditions locales et les caches.

**À dire à l'oral :** « Zstd donne ici le meilleur stockage. Le choix d'un codec
reste un compromis entre espace, ressources de calcul et lectures ; nos mesures
ne permettent pas d'établir un classement universel. »

## 8. Comparaison 4 — Tailles de lots JDBC

Nous gardons Parquet Snappy, sans partitionnement physique, et comparons
`fetch_size=100`, `1000` et `10000`.

| Fetch size | Lecture/conversion/écriture | Octets | Fichiers |
|---:|---:|---:|---:|
| 100 | 16,977 s | 1 289 934 | 2 |
| 1 000 | 19,724 s | 1 289 934 | 2 |
| 10 000 | 17,138 s | 1 289 934 | 2 |

Le fetch size est un réglage de lecture transmis au pilote JDBC. Il ne correspond
ni au nombre de fichiers ni au nombre de tâches exécutées simultanément.

Les trois variantes ont ici le même volume de données produit et le même nombre
de fichiers. Le lot 100 a la médiane la plus faible, mais les plages min/max des
durées se recouvrent. Trois répétitions ne suffisent pas pour affirmer que 100
est le meilleur réglage pour toute base ou toute volumétrie.

Les différences de temps SQL entre ces variantes ne doivent pas être attribuées
directement au fetch JDBC : ce réglage intervient pendant l'ingestion, avant
les requêtes analytiques sur les fichiers.

**À dire à l'oral :** « Nous avons varié le réglage JDBC en gardant le stockage
constant. Le fetch size influence potentiellement la lecture, mais ne détermine
pas la taille des fichiers, et le meilleur réglage doit être remesuré. »

## 9. Vérifier la justesse avant de comparer la vitesse

Une extraction qui perd des lignes pourrait sembler plus rapide. Le runner
vérifie donc les comptes et les agrégats avant d'accepter une mesure :

- nombre total de factures ;
- somme exacte des montants, comparée sous forme décimale textuelle ;
- somme des identifiants ;
- compte et montant de janvier ;
- comptes et montants par statut ;
- absence de résultats tronqués et identité des paramètres attendus.

Le rapport brut contient **24 extractions terminées**, **216 échantillons SQL
mesurés**, **72 warmups SQL** et un audit `integrity=passed`.
Ces contrôles vérifient les agrégats choisis, pas un checksum de chaque ligne.

## 10. Limites à présenter clairement

1. Dataset synthétique de 100 000 lignes et six colonnes, pas un test massif.
2. Un cœur d'exécuteur Spark : aucune preuve de scalabilité multicœur/multiworker.
3. Trois extractions par scénario : échantillon statistique limité.
4. Caches non purgés : ne pas annoncer des mesures à froid.
5. Pas d'isolation CPU stricte ; services habituels présents pendant la campagne.
6. Démarrage de Spark compris dans certains coûts du pipeline.
7. Lecture, transformation et upload combinés dans `extract_write_ms`.
8. Pas de mesure séparée du pic mémoire, de l'usage CPU ou du trafic réseau.
9. Pruning mesuré avec des prédicats dérivés explicites.
10. Tailles limitées aux fichiers de données, hors métadonnées et historique.

Ces limites ne rendent pas la campagne inutile : elles définissent exactement
ce que les résultats permettent de conclure.

## 11. Conclusion courte pour le professeur

> « Nous avons exécuté les quatre familles de comparaison demandées, sur le
> même dataset et avec répétitions. Le format et la compression réduisent
> fortement le stockage. Le partitionnement réduit les lignes lues, mais ses
> petits fichiers n'améliorent pas la latence sur notre volume. Les tailles de
> lots ne modifient pas le volume des fichiers. Tous les résultats sont contrôlés
> par des agrégats métier, et nous annonçons les limites des chronométrages et
> de notre configuration locale. »

## Sources et reproduction

- [Campagne brute : jobs, SQL, résultats et environnement](../benchmarks/results/local-2026-11-22-100k.json)
- [Médianes et octets exacts en CSV](../benchmarks/results/local-2026-11-22-100k-summary.csv)
- [Rapport technique de la campagne](benchmark-report.md)
- [Protocole et commandes de reproduction](benchmarks.md)
- [Définition des scénarios et contrôles](../benchmarks/plan.mjs)
- [Calcul des médianes](../benchmarks/report.mjs)

Une relance de campagne nécessite des services sains, l'absence d'autre ingestion
active et des ressources suffisantes. Elle doit utiliser un nouvel identifiant
de campagne et conserver les résultats précédents.
