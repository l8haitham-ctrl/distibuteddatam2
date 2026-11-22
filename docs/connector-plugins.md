# Plugins de bases relationnelles

La plateforme utilise une SPI JDBC (`RelationalPlugin`) pour separer les
particularites des moteurs de la logique metier. L'objectif est l'extensibilite
a des bases relationnelles disposant d'un pilote JDBC, et non une promesse que
tous les moteurs et tous leurs types proprietaires sont deja testes.

Branche `feature/jdbc-connector-plugins`, base `feature/wireframe-frontend`.

## Contrat

Le code partage se trouve dans
`backend/src/main/java/com/atlas/explorer/features/connectors/{connector,dto}`.
Il n'a pas de dependance Spring/HTTP et est compile dans l'API et dans le JAR
d'ingestion Spark. Le build Spark utilise maintenant le contexte racine du depot.

Chaque fournisseur definit :

- identifiant moteur, nom, classe du pilote, port et schema par defaut ;
- URL et proprietes JDBC construites depuis des champs separes ;
- schema par defaut eventuellement derive de la configuration de connexion,
  pour les moteurs dont le namespace depend de la base ou de l'utilisateur ;
- connexion avec indication de lecture seule, delimitation des identifiants ;
- calcul de cardinalite, choix catalogue/schema JDBC si necessaire ;
- options de lecture transmises au reader JDBC de Spark.

L'exploration generique utilise DatabaseMetaData pour les tables, colonnes,
types JDBC, cles primaires et etrangeres. L'adaptateur SQL Server conserve son
exploration sys.* existante et ses informations de longueur Unicode.
Les plugins peuvent adapter la selection catalogue/schema et les schemas
systeme. Les types JDBC non partitionnables sont exclus par la validation.
Les types proprietaires complexes exigent une adaptation et des tests ; une
conversion non prise en charge doit echouer plutot que produire un faux succes.

## Ajouter un fournisseur

1. Implementer `RelationalPlugin` dans une classe publique sans constructeur
   obligatoire et sans dependance au backend web. SQL Server fournit un exemple.
2. Declarer le nom complet de la classe dans
   `META-INF/services/com.atlas.explorer.features.connectors.connector.RelationalPlugin`.
   Un JAR de fournisseur peut apporter son propre descripteur ServiceLoader.
3. Ajouter le fournisseur et le pilote JDBC aux dependances du backend et de
   Spark, ou ajouter la classe au module de connecteurs du depot. Les images
   doivent recevoir les memes fournisseurs et versions compatibles du contrat.
4. Reconstruire les images API et Spark dans Docker. L'installation est une
   operation administrateur avec redemarrage, pas un telechargement depuis l'UI.
5. Verifier connexion, schema, precision, droits de lecture, extraction par lots,
   partitions, CSV/Parquet et relecture Trino sur le moteur reel.

Il n'y a pas de switch a etendre dans les controleurs, le dispatcher ou le
programme Spark pour chaque nouveau moteur. Le registre rejette les moteurs
inconnus et les doublons. `GET /connectors` annonce les fournisseurs presents,
le port/schema par defaut, la presence du pilote et les capacites communes.

## Sources configurees

Sans changement de configuration, seule la source historique `sqlserver` est
active ; SOURCE_HOST/PORT/DATABASE/USERNAME/PASSWORD/TRUST_SERVER_CERTIFICATE
restent compatibles. Pour plusieurs sources :

```text
SOURCE_IDS=sqlserver,warehouse
SOURCE_WAREHOUSE_ENGINE=<identifiant du plugin installe>
SOURCE_WAREHOUSE_HOST=<hote>
SOURCE_WAREHOUSE_PORT=<port>
SOURCE_WAREHOUSE_DATABASE=<base>
SOURCE_WAREHOUSE_USERNAME=<compte lecteur>
SOURCE_WAREHOUSE_PASSWORD=<secret>
SOURCE_WAREHOUSE_TRUST_SERVER_CERTIFICATE=false
```

Les identifiants de source sont uniques, en minuscules/chiffres/underscore.
Configurer les memes valeurs dans le backend et le worker. Ne pas inscrire les
secrets dans le JSON d'un job, les arguments du driver, le manifeste ou Git.
Le lecteur ne recoit jamais une URL JDBC arbitraire envoyee par le navigateur.

Les schemas omis dans l'API d'exploration et dans POST /storage/jobs sont resolus
depuis le plugin du moteur selectionne. Le schema est persiste explicitement
dans la demande normalisee ; les anciens jobs avec dbo restent lisibles.

## Validation

Les tests verifient la decouverte d'un fournisseur via un descripteur JAR,
les doublons, les identifiants inconnus, les configurations multi-sources et
la separation des proprietes SQL Server de son URL. Les tests existants de
connexion/exploration sont executes contre SQL Server Compose.

```powershell
docker compose --profile tools run --rm --no-deps backend-tests mvn -B test '-Dtest=ConnectorPluginTests,ApiTests,SchemaServiceTests,JobTests,ConnectionIntegrationTests,DatabaseIntegrationTests'
docker compose build backend spark-master
```

Ne pas remplacer les conteneurs Spark pendant un job actif. L'API, le master
et le worker doivent utiliser ensemble les images du meme protocole d'ingestion.
L'ajout de PostgreSQL comme moteur source constitue la fonctionnalite suivante.

Validation locale : 32 tests cibles passes (dont connexion/exploration SQL
Server Compose), images API/Spark construites, extraction
`6151da4d-10c5-4cd1-91c2-b1320dbf8c94` terminee avec 5 000 factures et COUNT(*)
Trino = 5 000. Apres un redemarrage, attendre l'etat ALIVE du master Spark,
qui peut traverser une minute de recuperation meme si son interface HTTP repond.
