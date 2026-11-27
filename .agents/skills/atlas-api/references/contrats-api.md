# Référence locale des API Atlas

Révisée le 27 novembre 2026 à partir du commit `eee650a12f1895f44bcc61951babd0423cba95e1`. Ce document décrit la branche de référence, sans appliquer les travaux en stash. Aucun serveur n’a été appelé pour générer ce contrat. Les exemples marqués illustratifs ne sont ni des identifiants réels ni des résultats mesurés.

## Comment utiliser cette référence

Chaque endpoint ci-dessous nomme ses schémas ; leur définition complète se trouve dans « Schémas complets partagés ». Pour une réponse destinée à l’utilisateur, développer ces champs. Les tableaux de variantes font partie du contrat et ne doivent pas être omis.

## Accès, sécurité et erreurs communes

Base backend interne : http://backend:8000 dans Compose. Le proxy frontend/nginx.conf retire le préfixe externe /api/ et transmet vers le backend : /api/sources dans le navigateur correspond à /sources dans les contrôleurs. Vérifier le proxy de développement et les ports Compose selon le mode utilisé.

- Public : /health, /auth/session, /auth/login et sondes Actuator readiness/liveness. Tout le reste exige une session.
- Cookie de session ATLAS_SESSION, HttpOnly, SameSite strict ; secure configurable APP_COOKIE_SECURE ; expiration d’inactivité 30 min.
- Tous POST/PUT/DELETE exigent X-CSRF-TOKEN valide, y compris login, test de connexion et requête SQL en lecture seule. Récupérer le cookie/jeton via GET /auth/session ; se connecter avec un formulaire, puis relire le jeton.
- 401 anonyme : `{"detail":"Connexion requise."}` ; 403 : `{"detail":"Session ou jeton de protection invalide. Actualisez la page."}`.
- Les JSON inconnus sont refusés (fail-on-unknown-properties=true). Body invalide global : 422 `{"detail":"Paramètres de la demande invalides."}` ; jobs ajoutent errors. Aucun mot de passe rejeté ne doit être réaffiché.
- Les 404 de source/table/job sont des objets detail. Paramètres path/query malformés non gérés peuvent rester des erreurs Spring 400 ; exceptions imprévues peuvent rester 500. Les contrats ci-dessous décrivent les handlers présents et ne promettent pas une couverture d’erreur exhaustive.
- Feature désactivée : contrôleur conditionnel absent, donc route potentiellement 404. Une UI visible ne prouve pas qu’un profil optionnel est activé.

## Index des routes métier et sécurité

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/health` | Vérifier que le backend répond |
| GET | `/auth/session` | Lire session et jeton CSRF |
| POST | `/auth/login` | Ouvrir une session plateforme |
| POST | `/auth/logout` | Fermer la session |
| GET | `/connectors` | Lister les plugins JDBC |
| GET | `/sources` | Lister les profils relationnels |
| POST | `/connections/test` | Tester une configuration JDBC saisie |
| POST | `/sources/{id}/test` | Tester un profil relationnel enregistré |
| POST | `/sources/{id}/test/events` | Voir les étapes du test en direct |
| GET | `/sources/{id}/tables` | Lister les tables relationnelles |
| GET | `/sources/{id}/tables/{table}` | Décrire une table relationnelle |
| POST | `/connections/explore` | Explorer une connexion ad hoc |
| GET | `/native/connectors` | Lister les connecteurs natifs |
| POST | `/native/connections/test` | test une configuration native saisie |
| POST | `/native/connections/explore` | explore une configuration native saisie |
| GET | `/native/sources` | Lister les profils natifs |
| POST | `/native/sources/{id}/test` | test un profil natif |
| POST | `/native/sources/{id}/explore` | explore un profil natif |
| POST | `/storage/jobs` | Créer une extraction asynchrone |
| GET | `/storage/jobs` | Lister les jobs récents |
| GET | `/storage/jobs/partition-candidates` | Vérifier les colonnes partitionnables |
| GET | `/storage/jobs/{id}` | Suivre un job |
| GET | `/storage/jobs/{id}/result` | Lire le résultat terminé |
| GET | `/storage/status` | Vérifier accès au bucket |
| GET | `/catalog/tables` | Lister les tables de jobs publiées |
| POST | `/sql/query` | Exécuter une requête Trino en lecture seule |
| POST | `/glossary/terms` | Créer un terme métier |
| GET | `/glossary/terms` | Chercher des termes |
| GET | `/glossary/terms/{id}` | Lire un terme |
| PUT | `/glossary/terms/{id}` | Modifier un terme |
| POST | `/glossary/terms/{id}/links` | Lier un terme à une table ou colonne |
| DELETE | `/glossary/terms/{id}/links/{linkId}` | Supprimer un lien métier |
| GET | `/glossary/links` | Retrouver les liens d’une cible technique |
| POST | `/quality/reports` | Comparer source et trois extractions |


## Schémas complets partagés

Les notations `?` et `| null` décrivent un champ optionnel ou nullable. Les tableaux peuvent être vides. Dates Instant : chaînes ISO-8601 UTC. Les noms JSON sont ceux du code. Les exemples sont illustratifs, pas des mesures nouvellement exécutées.

### SessionInfo
`{authenticated: boolean, username: string | null, csrf_token: string}`.

### ConnectionConfig — entrée seulement
| Champ | Type / présence / validation |
|---|---|
| engine | string obligatoire ; `[a-z][a-z0-9_]{0,31}` ; plugin installé requis |
| host | string obligatoire, max 255 ; lettres/chiffres/`_.:-` |
| port | integer obligatoire, 1..65535 |
| database | string obligatoire, max 128 |
| username | string obligatoire, max 128 |
| password | string obligatoire, max 128 ; WRITE_ONLY, jamais dans la réponse |
| trust_server_certificate | boolean optionnel/null ; true explicite autorise la confiance au certificat selon le plugin |

### Source
`{id:string, engine:string, host:string, port:integer, database:string, username:string, default_schema:string}`. Ne contient ni password, ni statut de dernier test persisté côté serveur.

### ConnectionTest
`{database_name:string, database_user:string, status:"connected"}`. SQL Server utilise DB_NAME/CURRENT_USER ; le plugin générique utilise le nom de base configuré et le nom utilisateur JDBC.

### Connector
`{engine:string, name:string, default_port:integer, default_schema:string, available:boolean, capabilities:string[]}` ; capabilities = connection/exploration/extraction pour les plugins JDBC actuels. Pilotes implémentés : sqlserver et postgresql. available prouve la classe pilote accessible, pas un serveur joignable.

### TableName et TableStructure
TableName = `{schema_name:string, table_name:string}`.
TableStructure = `{schema_name:string, table_name:string, columns:Column[], primary_key:PrimaryKey[], foreign_keys:ForeignKey[]}`.
Column commun SQL Server : `{name:string, data_type:string, position:integer, max_length_bytes:integer, precision:integer, scale:integer, nullable:boolean, is_identity:boolean, default_value:string|null}`.
Le connecteur JDBC générique ajoute `jdbc_type:integer, character_length:integer, temporal:boolean, partitionable:boolean`. Ne pas exiger ces quatre champs dans la variante SQL Server. Les nombres viennent des métadonnées du pilote.
PrimaryKey = `{constraint_name:string, column_name:string, position:integer}`.
ForeignKey = `{constraint_name:string, column_name:string, position:integer, referenced_schema:string, referenced_table:string, referenced_column:string}`.

### DiagnosticEvent — données d’un événement SSE
`{step:"dns"|"tcp"|"jdbc"|"validation"|"complete", state:"running"|"passed"|"failed"|"skipped", message:string, duration_ms:integer}`. Nom d’événement : progress. Durée de l’étape, ou durée totale pour complete. Après un échec, les étapes suivantes sont skipped puis complete/failed.

### NativeConnection — entrée seulement
| Champ | Type / présence / validation |
|---|---|
| engine | string obligatoire : redis / minio / clickhouse |
| host | string obligatoire max 255, caractères lettres/chiffres/`_.:-` |
| port | integer obligatoire 1..65535 |
| namespace | string obligatoire max 128 : index Redis, bucket MinIO ou base ClickHouse |
| username | string obligatoire max 128 : utilisateur Redis/ClickHouse ou access key MinIO |
| password | string obligatoire max 128, WRITE_ONLY : mot de passe ou secret key |
| tls | boolean optionnel ; défaut true |
| prefix | string optionnel max 512 ; défaut chaîne vide |

Pour Redis namespace doit être un index 0..15 écrit avec 1 à 2 chiffres.
NativeProfile = `{id:string, engine:string, host:string, port:integer, namespace:string, username:string, prefix:string, tls:boolean}` ; ClickHouse ajoute `extraction_source_id:"clickhouse"` ; Redis ajoute `extraction_source_id:"redis", extraction_schema:string, extraction_dataset:string`. MinIO n’ajoute pas ces champs. Ces profils ne renvoient pas password.
NativeConnector = `{engine:string, name:string, default_port:integer, capabilities:["connection","exploration"]}`. Ce tableau ne décrit pas toutes les capacités des lecteurs d’extraction distincts.
NativeTest = `{status:"connected", engine:string, namespace:string, transport:"TLS"|"plain"}`.
NativeExplore = `{engine:string, namespace:string, objects:NativeObject[], truncated:boolean, limit:100, transport:"TLS"|"plain", note:string}`.
NativeObject Redis = `{name:string, type:string, fields:string[]}` ; au plus 20 noms de champs hash, aucune valeur. Échantillon de 100 clés, au plus 20 pages et budget de boucle 10 s, SCAN non transactionnel.
NativeObject MinIO = `{name:string, type:"object", size:integer, fields:[]}` ; taille en octets, au plus 100 objets et budget de boucle 10 s.
NativeObject ClickHouse = `{name:string, type:string, fields:string[], columns:NativeColumn[], columns_truncated:boolean}` ; type est le moteur de table, au plus 100 tables et 200 colonnes/table.
NativeColumn = `{name:string, data_type:string, position:integer, sorting_key:boolean, partition_key:boolean}`.

### CreateJobRequest
| Champ | Type / obligatoire / défaut / contraintes |
|---|---|
| source_id | string obligatoire non vide max 64 ; profil relationnel configuré ou lecteur natif redis/clickhouse activé |
| schema | string optionnel ; défaut du moteur ; identifiant `[A-Za-z_][A-Za-z0-9_]{0,127}` |
| table | string obligatoire ; même syntaxe identifiant |
| format | string optionnel défaut parquet ; parquet ou csv |
| partition_by | string[] optionnel défaut [] ; 8 colonnes max, identifiants valides |
| fetch_size | integer optionnel défaut 1000 ; 1..100000 |
| compression | string optionnel ; parquet : snappy par défaut, zstd, uncompressed ; csv : gzip par défaut ou uncompressed |
| partition_granularity | string optionnel défaut month ; year/month/day |
| num_partitions | integer optionnel défaut 4 ; 1..16, demande de lecture, pas garantie de parallélisme |

Validation métier : source/table accessible, noms exacts ; partition_by sans doublon, pas sur PK, type admis, cardinalité de la colonne source au plus 1000, pas de collision avec colonnes dérivées. Pour les lecteurs natifs actuels : partition_by doit rester vide, une partition de lecture effective. Il n’existe pas de champ publié pour sélection arbitraire de colonnes, filtre WHERE ou incrémental dans cette requête.

### StorageJob
`{id:UUID, status:string, request:CreateJobRequest, created_at:Instant, started_at:Instant|null, finished_at:Instant|null, submission_id:string|null, error_code:string|null, result:JobResult|null}`.
États persistés : QUEUED, SUBMITTING, RUNNING, REGISTERING, COMPLETED, FAILED. submission_id identifie le driver Spark, pas l’UUID métier. Erreurs possibles : SUBMISSION_UNCERTAIN, EXECUTION_TIMEOUT, RESULT_MISSING, RESULT_INVALID, SPARK_* et CATALOG_REGISTRATION_FAILED.

### JobResult et CatalogEntry
Tous les champs du manifeste actuel :
| Champ | Type / sens |
|---|---|
| version | integer, actuellement 2 |
| job_id | UUID sous forme string |
| prefix | string, source/schema/table/UUID |
| source_id, schema_name, table_name, source_engine | string |
| format, compression | string |
| row_count, file_count, storage_bytes | integers ; taille des fichiers de données en octets |
| schema_json | string contenant le schéma Spark JSON |
| partition_by | string[] |
| partition_granularity | string |
| partition_columns | array de `{name:string,type:string,source_column:string,derived:boolean}` |
| partition_file_counts | objet dynamique chemin-partition → nombre de fichiers ; __unpartitioned__ sans partition |
| fetch_size, max_records_per_file | integers ; max_records_per_file=100000 actuellement |
| requested_num_partitions, effective_num_partitions | integers |
| jdbc_partition_column, jdbc_lower_bound, jdbc_upper_bound | string ou null |
| parallel_read_note | string, décrit aussi le fallback ou le lecteur natif |
| extract_write_ms, verify_ms, spark_duration_ms | integers en millisecondes |
| spark_application_id | string |
| csv_options | CSV seulement : `{header:false,separator:",",null_value:"\\N",date_format:"yyyy-MM-dd",timestamp_format:"yyyy-MM-dd HH:mm:ss",decimal_format:"plain",quote:"none",escape:"none",encoding:"UTF-8",unsafe_strings:"fail"}` |
| csv_preserve_whitespace | CSV seulement : true |
| manifest_key, bucket | strings ajoutées par le backend |
| catalog_table | string, table physique Hive de cette exécution |
| current_view | string pour Parquet, null pour CSV |
| registration_ms | integer, enregistrement et vérification catalogue |
| example_query | string SQL exemple |

CatalogEntry ajoute `location:string`, URI s3://bucket/prefix/data/. Pas de champs Iceberg ni de lien retry dans le contrat publié. Les manifests d’anciennes versions peuvent avoir moins de champs ; ne pas inventer les champs manquants.

### PartitionCandidate
Relationnel : `{name:string, data_type:string, eligible:boolean, reason:string}`. Natif : `{name:string,data_type:string,eligible:false,reason:string}`. Ce sont des suggestions validées au moment de l’appel, pas une réservation de configuration.

### SqlRequest / SqlResult
Entrée : `{sql:string obligatoire non vide max 20000, max_rows:integer optionnel défaut 1000, intervalle 1..1000}`.
Sortie : `{columns:[{name:string,type:string}], rows:any[][], row_count:integer, truncated:boolean, execution_ms:integer, query_id:string, stats:{elapsed_ms:integer,cpu_ms:integer,processed_rows:integer,processed_bytes:integer,state:string}}`.
rows est un tableau de lignes dans l’ordre de columns, pas un tableau d’objets. Une cellule SQL NULL est null ; dates/heures SQL sont converties en string. Une limite de résultat ne prouve pas une limite des données scannées.

### TermRequest, BusinessTerm et LinkRequest
TermRequest = `{name:string obligatoire non vide max160, definition:string obligatoire non vide max8000, calculation_rule:string|null optionnel max8000, owner:string|null optionnel max160, status:"DRAFT"|"VALIDATED" obligatoire}`. Les textes sont strip ; calculation_rule et owner null deviennent chaîne vide.
BusinessTerm = `{id:UUID,name:string,definition:string,calculation_rule:string,owner:string,status:"DRAFT"|"VALIDATED",created_at:Instant,updated_at:Instant,links:TermLink[]}`.
LinkRequest = `{source_id:string obligatoire non vide max128,schema_name:string obligatoire non vide max128,table_name:string obligatoire non vide max128,column_name:string|null optionnel max128 et au moins un caractère non blanc}`.
TermLink = `{id:UUID,term_id:UUID,source_id:string,schema_name:string,table_name:string,column_name:string|null,created_at:Instant}`.

### QualityRequest / QualityReport
Entrée : `{source_id:string obligatoire non vide,customer_job_id:UUID obligatoire,invoice_job_id:UUID obligatoire,payment_job_id:UUID obligatoire}`.
Sortie : `{report_id:UUID,generated_at:Instant,profile:"customer_invoice_payment_v1",source_id:string,jobs:{Customer:UUID,Invoice:UUID,Payment:UUID},catalog_tables:{Customer:string,Invoice:string,Payment:string},source_metrics:QualityMetrics,lake_metrics:QualityMetrics,checks:QualityCheck[],status:"passed"|"failed"}`.
QualityCheck = `{id:string,observed:string,expected:string,status:"passed"|"failed"}`.
QualityMetrics : objet avec sept clés. Valeurs métriques sérialisées en strings numériques :
- customer : row_count, distinct_ids, id_sum, null_rows, duplicate_emails, invalid_emails, invalid_timestamps.
- invoice : row_count, distinct_ids, id_sum, null_rows, amount_sum, invalid_amounts, invalid_statuses, invalid_timestamps.
- payment : row_count, distinct_ids, id_sum, null_rows, amount_sum, invalid_amounts, invalid_methods, invalid_timestamps.
- orphan_invoices, orphan_payments, payment_dates, invoice_balances : chacune `{violations:string}`.
checks couvre contraintes source/lac, réconciliation des métriques, stabilité source avant/après et relations. Ne signifie pas comparaison ligne par ligne. Modèle requis : colonnes métier du sujet et updated_at ; moteurs SQL Server/PostgreSQL uniquement dans ce profil.

## Détail endpoint par endpoint

### 1. GET /health

**Rôle :** Vérifier que le backend répond.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Aucun paramètre ni body. Public.

**Authentification :** public ; voir le handshake CSRF commun pour login.

**Sorties :** 200 application/json : {status:"ok"}.

**Erreurs et limites :** Ne teste pas les dépendances ; panne HTTP/transport possible.

**Code à vérifier :** [HealthController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/health/HealthController.java).

**Exemple illustratif :**

```json
{"status":"ok"}
```

### 2. GET /auth/session

**Rôle :** Lire session et jeton CSRF.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Aucun body. Cookie existant si disponible ; accessible anonymement.

**Authentification :** public ; voir le handshake CSRF commun pour login.

**Sorties :** 200 SessionInfo ; Cache-Control: no-store. Peut créer une session/Set-Cookie.

**Erreurs et limites :** Pas de mot de passe renvoyé.

**Code à vérifier :** [AuthController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/auth/api/AuthController.java).

**Exemple illustratif :**

```json
{"authenticated":false,"username":null,"csrf_token":"<jeton>"}
```

### 3. POST /auth/login

**Rôle :** Ouvrir une session plateforme.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Content-Type: application/x-www-form-urlencoded ; champs username et password ; cookie obtenu avant login ; X-CSRF-TOKEN. Pas un body JSON.

**Authentification :** public ; voir le handshake CSRF commun pour login.

**Sorties :** 204 sans body ; cookie session fixé/renouvelé par Spring Security. Relire /auth/session après login.

**Erreurs et limites :** 401 {detail:"Identifiants incorrects."} ; 403 si CSRF invalide.

**Code à vérifier :** [AuthConfiguration.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/auth/config/AuthConfiguration.java).

**Exemple illustratif :**

```text
username=<utilisateur>&password=<mot-de-passe>
```

### 4. POST /auth/logout

**Rôle :** Fermer la session.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Cookie ATLAS_SESSION ; X-CSRF-TOKEN ; aucun champ/body requis.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 204 sans body ; session invalidée et cookie supprimé.

**Erreurs et limites :** 403 si CSRF invalide ; ne pas supposer une réponse JSON.

**Code à vérifier :** [AuthConfiguration.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/auth/config/AuthConfiguration.java).

### 5. GET /connectors

**Rôle :** Lister les plugins JDBC.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Aucun paramètre/body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 Connector[].

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive.

**Code à vérifier :** [ConnectorController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectors/api/ConnectorController.java).

### 6. GET /sources

**Rôle :** Lister les profils relationnels.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Aucun paramètre/body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 Source[]. Liste sans connexion automatique ni mot de passe.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée.

**Code à vérifier :** [SourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connection/api/SourceController.java).

### 7. POST /connections/test

**Rôle :** Tester une configuration JDBC saisie.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Body complet ConnectionConfig. Ne crée pas de profil.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 ConnectionTest.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée.

**Code à vérifier :** [ConnectionController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connection/api/ConnectionController.java).

**Exemple illustratif :**

```json
{"engine":"sqlserver","host":"sqlserver","port":1433,"database":"BusinessDB","username":"lake_reader","password":"<secret>","trust_server_certificate":true}
```

### 8. POST /sources/{id}/test

**Rôle :** Tester un profil relationnel enregistré.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Path id:string ; aucun body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 ConnectionTest.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée.

**Code à vérifier :** [SourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connection/api/SourceController.java).

### 9. POST /sources/{id}/test/events

**Rôle :** Voir les étapes du test en direct.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Path id:string ; aucun body ; Accept: text/event-stream ; cookie et CSRF.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 text/event-stream, événements progress contenant DiagnosticEvent ; Cache-Control no-store, X-Accel-Buffering:no ; timeout émetteur 90 s. Échec métier possible dans un flux HTTP 200.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 source inconnue avant ouverture du flux ; interruption possible du flux après ouverture.

**Code à vérifier :** [ConnectionDiagnosticsController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/connectiondiagnostics/api/ConnectionDiagnosticsController.java).

**Exemple illustratif :**

```text
event: progress
data: {"step":"dns","state":"running","message":"Recherche de l’adresse du serveur.","duration_ms":0}
```

### 10. GET /sources/{id}/tables

**Rôle :** Lister les tables relationnelles.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Path id:string ; aucun body/query.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 {tables:TableName[]}.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée.

**Code à vérifier :** [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java).

### 11. GET /sources/{id}/tables/{table}

**Rôle :** Décrire une table relationnelle.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Path id:string, table:string ; query schema:string optionnel, défaut du moteur.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 TableStructure, avec variantes SQL Server/JDBC.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée.

**Code à vérifier :** [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java).

### 12. POST /connections/explore

**Rôle :** Explorer une connexion ad hoc.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Body complet ConnectionConfig.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 {database:string,tables:TableStructure[]}.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée.

**Code à vérifier :** [ExplorationController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/exploration/api/ExplorationController.java).

### 13. GET /native/connectors

**Rôle :** Lister les connecteurs natifs.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Aucun paramètre/body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 NativeConnector[].

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive.

**Code à vérifier :** [NativeSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeSourceController.java).

### 14. POST /native/connections/test

**Rôle :** test une configuration native saisie.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Body complet NativeConnection ; cookie et CSRF.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 NativeTest.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 502 {detail:string} générique pour connexion/exploration refusée.

**Code à vérifier :** [NativeSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeSourceController.java).

**Exemple illustratif :**

```json
{"engine":"redis","host":"redis-source","port":6379,"namespace":"0","username":"atlas_reader","password":"<secret>","tls":false,"prefix":"analytics:customer_segments:"}
```

### 15. POST /native/connections/explore

**Rôle :** explore une configuration native saisie.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Body complet NativeConnection ; cookie et CSRF.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 NativeExplore avec variantes Redis/MinIO/ClickHouse.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 502 {detail:string} générique pour connexion/exploration refusée.

**Code à vérifier :** [NativeSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeSourceController.java).

**Exemple illustratif :**

```json
{"engine":"redis","host":"redis-source","port":6379,"namespace":"0","username":"atlas_reader","password":"<secret>","tls":false,"prefix":"analytics:customer_segments:"}
```

### 16. GET /native/sources

**Rôle :** Lister les profils natifs.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Aucun paramètre/body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 NativeProfile[]. Profils conditionnés par configuration, pas un registre créable via POST.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive.

**Code à vérifier :** [NativeConfiguredSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeConfiguredSourceController.java).

### 17. POST /native/sources/{id}/test

**Rôle :** test un profil natif.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Path id:string : redis-demo/clickhouse-demo/minio-lake si activés ; aucun body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 NativeTest.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 502 si profil inconnu ou service natif refusé (pas 404 garanti).

**Code à vérifier :** [NativeConfiguredSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeConfiguredSourceController.java).

### 18. POST /native/sources/{id}/explore

**Rôle :** explore un profil natif.

**Disponibilité :** Toujours déclaré dans le code ; dépendances configurées requises pour fonctionner.

**Entrées :** Path id:string : redis-demo/clickhouse-demo/minio-lake si activés ; aucun body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 NativeExplore.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 502 si profil inconnu ou service natif refusé (pas 404 garanti).

**Code à vérifier :** [NativeConfiguredSourceController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/nativesources/api/NativeConfiguredSourceController.java).

### 19. POST /storage/jobs

**Rôle :** Créer une extraction asynchrone.

**Disponibilité :** jobs.enabled=true ; stockage, PostgreSQL et Spark requis selon opération.

**Entrées :** Body CreateJobRequest complet ; cookie et CSRF.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 202 StorageJob ; header Location vers /storage/jobs/{id}. La création retourne avant extraction.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée. 422 {detail:string,errors:{champ:message}} pour validation job ; 503 historique indisponible.

**Code à vérifier :** [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java).

**Exemple illustratif :**

```json
{"source_id":"sqlserver","schema":"dbo","table":"Invoice","format":"parquet","compression":"snappy","partition_by":["invoice_date"],"partition_granularity":"month","fetch_size":1000,"num_partitions":4}
```

### 20. GET /storage/jobs

**Rôle :** Lister les jobs récents.

**Disponibilité :** jobs.enabled=true ; stockage, PostgreSQL et Spark requis selon opération.

**Entrées :** Aucun paramètre/body ; pas de pagination HTTP déclarée.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 StorageJob[] ; au plus 100 jobs, triés created_at DESC puis id DESC.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 503 historique indisponible.

**Code à vérifier :** [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java).

### 21. GET /storage/jobs/partition-candidates

**Rôle :** Vérifier les colonnes partitionnables.

**Disponibilité :** jobs.enabled=true ; stockage, PostgreSQL et Spark requis selon opération.

**Entrées :** Query source_id:string obligatoire, table:string obligatoire, schema:string optionnel (défaut source), partition_granularity:year/month/day optionnel défaut month.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 PartitionCandidate[].

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée. 422 choix invalide ; erreurs de paramètres manquants potentiellement 400 Spring.

**Code à vérifier :** [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java).

### 22. GET /storage/jobs/{id}

**Rôle :** Suivre un job.

**Disponibilité :** jobs.enabled=true ; stockage, PostgreSQL et Spark requis selon opération.

**Entrées :** Path id:UUID ; aucun body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 StorageJob.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 job absent ; 503 historique indisponible ; UUID invalide potentiellement 400 Spring.

**Code à vérifier :** [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java).

### 23. GET /storage/jobs/{id}/result

**Rôle :** Lire le résultat terminé.

**Disponibilité :** jobs.enabled=true ; stockage, PostgreSQL et Spark requis selon opération.

**Entrées :** Path id:UUID ; aucun body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 JobResult.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 job absent ; 409 {detail:string} si non COMPLETED ; 503 historique indisponible.

**Code à vérifier :** [JobController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/jobs/api/JobController.java).

### 24. GET /storage/status

**Rôle :** Vérifier accès au bucket.

**Disponibilité :** storage.enabled=true

**Entrées :** Aucun paramètre/body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 {status:"ready",bucket:string}.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 503 stockage indisponible.

**Code à vérifier :** [StorageController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/storage/api/StorageController.java).

### 25. GET /catalog/tables

**Rôle :** Lister les tables de jobs publiées.

**Disponibilité :** catalog.enabled=true

**Entrées :** Aucun paramètre/body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 CatalogEntry[] ; liste issue des jobs COMPLETED de métadonnées, pas liste exhaustive de tous les catalogues Trino.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 503 {detail:"Catalogue indisponible."}.

**Code à vérifier :** [CatalogController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/catalog/api/CatalogController.java).

### 26. POST /sql/query

**Rôle :** Exécuter une requête Trino en lecture seule.

**Disponibilité :** catalog.enabled=true

**Entrées :** Body SqlRequest ; cookie et CSRF. SELECT/WITH/SHOW/DESCRIBE, pas de multi-instructions ni commentaires ; point-virgule final admis. Timeout 60 s.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 SqlResult complet.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 422 requête refusée/invalide ou paramètres SQL invalides ; 503 connexion SQL indisponible.

**Code à vérifier :** [SqlController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/sql/api/SqlController.java).

**Exemple illustratif :**

```json
{"sql":"SELECT count(*) AS total FROM hive.sqlserver_dbo.invoice","max_rows":100}
```

### 27. POST /glossary/terms

**Rôle :** Créer un terme métier.

**Disponibilité :** jobs.enabled=true ; PostgreSQL pour persistance ; source accessible pour lier une cible.

**Entrées :** Body TermRequest.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 201 BusinessTerm ; Location: /glossary/terms/{id}.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 terme/lien absent ; 409 conflit ; 422 cible invalide ou paramètres glossaire invalides ; 503 métadonnées glossaire indisponibles.

**Code à vérifier :** [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java).

**Exemple illustratif :**

```json
{"name":"Montant facturé","definition":"Montant porté sur une facture.","calculation_rule":"SUM(Invoice.amount)","owner":"Finance","status":"DRAFT"}
```

### 28. GET /glossary/terms

**Rôle :** Chercher des termes.

**Disponibilité :** jobs.enabled=true ; PostgreSQL pour persistance ; source accessible pour lier une cible.

**Entrées :** Query q:string défaut vide max160 ; limit:integer défaut50, 1..100 ; offset:integer défaut0, minimum0. Aucun body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 BusinessTerm[].

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 terme/lien absent ; 409 conflit ; 422 cible invalide ou paramètres glossaire invalides ; 503 métadonnées glossaire indisponibles.

**Code à vérifier :** [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java).

### 29. GET /glossary/terms/{id}

**Rôle :** Lire un terme.

**Disponibilité :** jobs.enabled=true ; PostgreSQL pour persistance ; source accessible pour lier une cible.

**Entrées :** Path id:UUID. Aucun body.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 BusinessTerm.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 terme/lien absent ; 409 conflit ; 422 cible invalide ou paramètres glossaire invalides ; 503 métadonnées glossaire indisponibles.

**Code à vérifier :** [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java).

### 30. PUT /glossary/terms/{id}

**Rôle :** Modifier un terme.

**Disponibilité :** jobs.enabled=true ; PostgreSQL pour persistance ; source accessible pour lier une cible.

**Entrées :** Path id:UUID ; body TermRequest complet, pas un PATCH partiel.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 BusinessTerm.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 terme/lien absent ; 409 conflit ; 422 cible invalide ou paramètres glossaire invalides ; 503 métadonnées glossaire indisponibles.

**Code à vérifier :** [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java).

### 31. POST /glossary/terms/{id}/links

**Rôle :** Lier un terme à une table ou colonne.

**Disponibilité :** jobs.enabled=true ; PostgreSQL pour persistance ; source accessible pour lier une cible.

**Entrées :** Path id:UUID ; body LinkRequest. Noms exacts validés dans métadonnées relationnelles.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 201 TermLink.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 terme/lien absent ; 409 conflit ; 422 cible invalide ou paramètres glossaire invalides ; 503 métadonnées glossaire indisponibles. 400 authentification de la base refusée ; 404 source/table absente selon route ; 422 connecteur indisponible/paramètres invalides ; 503 serveur inaccessible ; 502 opération SQL source refusée.

**Code à vérifier :** [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java).

**Exemple illustratif :**

```json
{"source_id":"sqlserver","schema_name":"dbo","table_name":"Invoice","column_name":"amount"}
```

### 32. DELETE /glossary/terms/{id}/links/{linkId}

**Rôle :** Supprimer un lien métier.

**Disponibilité :** jobs.enabled=true ; PostgreSQL pour persistance ; source accessible pour lier une cible.

**Entrées :** Path id:UUID et linkId:UUID ; aucun body ; CSRF.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 204 sans body.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 terme/lien absent ; 409 conflit ; 422 cible invalide ou paramètres glossaire invalides ; 503 métadonnées glossaire indisponibles.

**Code à vérifier :** [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java).

### 33. GET /glossary/links

**Rôle :** Retrouver les liens d’une cible technique.

**Disponibilité :** jobs.enabled=true ; PostgreSQL pour persistance ; source accessible pour lier une cible.

**Entrées :** Query source_id/schema_name/table_name:string obligatoires non vides max128 ; column_name:string optionnel max128 non blanc si fourni ; limit:integer défaut50, 1..100 ; offset:integer défaut0, minimum0.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 TermLink[].

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 404 terme/lien absent ; 409 conflit ; 422 cible invalide ou paramètres glossaire invalides ; 503 métadonnées glossaire indisponibles.

**Code à vérifier :** [GlossaryController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/glossary/api/GlossaryController.java).

### 34. POST /quality/reports

**Rôle :** Comparer source et trois extractions.

**Disponibilité :** jobs.enabled=true ET catalog.enabled=true

**Entrées :** Body QualityRequest ; jobs terminés de la bonne source/schéma/table ; profil Customer/Invoice/Payment enrichi.

**Authentification :** session ATLAS_SESSION ; X-CSRF-TOKEN en plus pour POST/PUT/DELETE.

**Sorties :** 200 QualityReport ; status=failed peut signaler des différences sans erreur HTTP.

**Erreurs et limites :** 401 session absente ; 403 pour un appel mutateur sans CSRF valide ; 422 body JSON invalide selon handler global. Les erreurs non prises en charge explicitement peuvent utiliser le format Spring Boot (notamment 400 pour certains paramètres invalides) ou un 500 générique : ne pas promettre une normalisation exhaustive. 422 profil ou jobs incompatibles ; 503 analyse qualité indisponible ; autres erreurs sources/jobs selon handler global.

**Code à vérifier :** [QualityController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/quality/api/QualityController.java).

**Exemple illustratif :**

```json
{"source_id":"sqlserver","customer_job_id":"<uuid>","invoice_job_id":"<uuid>","payment_job_id":"<uuid>"}
```

## Messages d’erreur du handler global

La forme est `{detail:string}` ; le code ci-dessous est une catégorie interne, pas un champ JSON automatiquement renvoyé.

| Catégorie interne | HTTP | detail |
|---|---:|---|
| CONNECTOR_NOT_FOUND | 422 | Connecteur non installé ou indisponible. |
| AUTHENTICATION | 400 | Authentification refusée : vérifier les identifiants et la base. |
| UNAVAILABLE | 503 | Connexion impossible : vérifier le serveur, le port et le certificat. |
| SOURCE_NOT_FOUND | 404 | Source inconnue. |
| TABLE_NOT_FOUND | 404 | Table introuvable ou non accessible. |
| OBJECT_NOT_FOUND | 404 | Fichier introuvable. |
| STORAGE_UNAVAILABLE | 503 | Stockage indisponible : vérifier le service et sa configuration. |
| STORAGE_INVALID | 422 | Paramètres de stockage invalides. |
| JOB_NOT_FOUND | 404 | Traitement introuvable. |
| JOB_NOT_COMPLETE | 409 | Le traitement ne dispose pas d’un résultat terminé. |
| JOB_ENGINE_UNAVAILABLE | 503 | Moteur de traitement indisponible. |
| JOB_RESULT_INVALID | 502 | Résultat du traitement invalide. |
| QUERY / défaut | 502 | La base a refusé l’opération. Vérifier les droits de lecture. |

Handlers spécialisés :
- Jobs validation : `{detail:string,errors:{nom_de_champ:string}}`, 422. Exemple : `{"detail":"Parametres du job invalides.","errors":{"compression":"Combinaison invalide."}}` (message illustratif abrégé ; lire InvalidJobException pour le texte réel). Historique indisponible : 503 `{detail:"Historique des traitements indisponible."}`.
- SQL invalide : 422 `{detail:"Requete refusee ou invalide ; utiliser SELECT, WITH, SHOW ou DESCRIBE en lecture seule."}`. Connexion SQL : 503 `{detail:"Service SQL indisponible."}`. Validation DTO : 422 avec message sql/max_rows.
- Catalogue : 503 `{detail:"Catalogue indisponible."}`.
- Source native : 502 `{detail:"Connexion ou exploration impossible. Verifiez le serveur, les identifiants, TLS et les droits de lecture."}`.
- Glossaire : 404 `{detail:"Terme ou lien introuvable."}` ; 409 `{detail:"Ce nom de terme ou ce lien existe deja."}` ; 422 cible `{detail:"Colonne introuvable ou cible invalide."}` ; 503 DAO `{detail:"Metadonnees du glossaire indisponibles."}` ; 422 paramètres path/query gérés `{detail:"Parametres du glossaire invalides."}`.
- Qualité : 422 `{detail:"Selectionner trois jobs termines, de la meme source et du schema attendu, pour Customer, Invoice et Payment."}` ; 503 `{detail:"Controle qualite indisponible. Verifier les sources, le catalogue et le schema du profil metier."}` ; échec des règles métier=200 avec QualityReport.status=failed.

## Routes fournies par le framework et documentation

Ces routes ne sont pas comptées dans l’inventaire des contrôleurs métier :

| Route | Entrées | Sorties / sécurité |
|---|---|---|
| GET /docs et redirections Swagger UI | session ; pas de body | HTML/ressources Swagger, redirection possible vers /swagger-ui ; endpoint configuré par springdoc |
| GET /openapi.json | session ; pas de body | document OpenAPI JSON : openapi/info/servers/paths/components/security selon springdoc ; le schéma généré des Map peut être moins précis que ce guide |
| GET /actuator/health/liveness | public ; pas de body | réponse Actuator, habituellement {status:"UP"}, 200 si UP ou 503 selon état/agrégateur |
| GET /actuator/health/readiness | public ; pas de body | réponse Actuator, habituellement {status:"UP"}, 200 ou 503 selon état ; ne prouve pas toute la chaîne métier |
| GET /actuator/health | session ; pas de body | réponse Actuator avec détails masqués |
| GET /actuator/info | session ; pas de body | objet des contributeurs info configurés, pas un contrat métier stable |

## Points de contrat à ne pas inventer

Pas de POST /sources persisté, de DELETE /glossary/terms/{id}, de POST /storage/jobs/{id}/retry ou de route Iceberg dans cette référence. Les paramètres d’une connexion ad hoc ne sont pas automatiquement persistés dans l’environnement Spark. Les profils natifs et source_id de jobs ont des identifiants distincts.

## Vérification et maintenance

Lire contrôleurs + DTO + services/connectors qui construisent Map + handlers. Vérifier AuthConfiguration pour login/logout et application.yml pour les chemins docs/sondes. Comparer cet inventaire aux annotations avant toute mise à jour. L’inventaire JSON contient les mêmes routes et renvoie aux sources ; il complète ce document, ne remplace pas les schémas ci-dessus.
