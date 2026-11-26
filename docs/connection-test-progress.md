# Suivi du test depuis la liste des sources

Branche `feature/source-test-progress`, base `feature/platform-authentication`.

Le bouton Tester d'une source configuree ouvre un dialogue contenant les
etapes, leurs etats, les messages du serveur et les durees mesurees. Le test
reel est lance une fois par source. Pendant son execution, Suivre permet de
reouvrir la fenetre sans redemarrer le diagnostic. Fermer la fenetre laisse
le test en cours et la liste est mise a jour a son terme.

## Etapes reelles

| Etape | Verification |
| --- | --- |
| Resolution du serveur | Resolution DNS via le systeme Java |
| Acces au port | Ouverture d'une socket TCP vers une adresse resolue, budget total 5 s |
| Connexion a la base | Ouverture JDBC avec le plugin existant ; TLS, authentification et selection de base sont une operation du pilote |
| Validation de la session SQL | Appel JDBC isValid(5) sur cette connexion |

Les etats passent de En attente a En cours, puis Reussi ou Echec. Apres un
echec, les etapes suivantes sont Ignorees. Les erreurs de transport ou un
flux termine sans resultat final laissent les controles inconnus Non confirmes,
sans transformer une attente en reussite. Aucune animation minutee ne simule
la progression. Sur une base locale rapide, plusieurs etapes peuvent etre
terminees avant le prochain rafraichissement visuel ; le compte rendu reste visible.

Le diagnostic ne cree pas de job Spark et n'ecrit aucune donnee metier.
L'acces au port utilise une socket distincte de la connexion JDBC ; un port
accessible ne prouve donc pas a lui seul la validite des identifiants ou du certificat.
La resolution DNS depend du systeme ; le flux et le navigateur ont une limite
globale de 90 secondes et les delais JDBC restent ceux du plugin.

## API et implementation

POST /sources/{id}/test/events retourne un flux text/event-stream (SSE), avec
des evenements progress contenant step, state, message et duration_ms.
step vaut dns, tcp, jdbc, validation ou complete ; complete contient le
resultat final passed/failed. L'endpoint exige une session et un jeton CSRF.
Une source inconnue est refusee avant l'ouverture du flux. Les messages ne
contiennent ni mot de passe, ni URL JDBC complete, ni exception brute.

Le package backend features/connectiondiagnostics separe api, dto, service
et connector. Le service notifie un Consumer sans connaitre HTTP. Le controleur
adapte ces notifications a SseEmitter et execute le travail dans un thread
virtuel Java 21. Le proxy Nginx desactive le buffering pour transmettre les
evenements sans attendre la fin du test.

Angular utilise fetch en lecture de flux avec cookie same-origin et le jeton
CSRF en memoire. Les fragments SSE et UTF-8 sont assembles avant lecture.
Aucune reconnexion automatique ne relance un diagnostic. Les resultats sont
gardes dans SourcesStore pendant la session de l'application, sans stockage
des identifiants. Recharger completement l'application efface cet historique local.

L'ancien POST /sources/{id}/test reste compatible et conserve sa reponse JSON.
Le formulaire Ajouter une source et ses tests classiques ne changent pas :
cette feature concerne le bouton Tester de la liste des sources configurees.
Les connexions temporaires et le mode maquette ne lancent pas ce diagnostic.

## Verification Docker

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml --profile tools run --rm --no-deps backend-tests mvn -B '-Dtest=DiagnosticsTests,DiagnosticsIntegrationTests,ConnectionIntegrationTests,DatabaseIntegrationTests,AuthenticationTests' test
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml build backend frontend
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml up -d --no-deps --wait backend frontend
```

Conserver les overrides actifs. Les integrations verifient SQL Server et,
avec RUN_PG_TESTS=true, PostgreSQL. Les tests unitaires couvrent les echecs
DNS, le refus d'authentification, la fermeture de connexion et un transport
annule. Angular teste les trames fragmentees, CSRF, absence de relance,
etapes en echec/ignorees et resultat non confirme si le flux est interrompu.

Reference : [SseEmitter Spring MVC](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-ann-async.html).

Les tests Angular utilisent au maximum deux workers via vitest-base.config.mts.
Ce reglage evite la saturation memoire des rendus DOM/JIT lorsque Docker execute
aussi SQL Server, Trino et Spark. Des executions initiales paralleles avaient
depasse la limite de 5 secondes de plusieurs tests de rendu existants ; la
suite finale conserve les memes assertions et delais et passe integralement.

Validation du 26 novembre 2026 : 18 tests backend cibles passes avec SQL Server,
PostgreSQL et securite ; construction backend avec 55 tests passes (101
decouverts, 46 integrations ignorees), puis 56 tests Angular passes dans 13
fichiers. Les images backend/frontend sont construites et demarrees.
Le navigateur observe DNS/TCP reussis pendant que JDBC est encore en cours,
puis les quatre etapes reussies en 2426 ms. La [capture](evidence/source-test-sqlserver.jpg)
et la [synthese](../validation/results/source-test-progress-2026-11-26.json)
conservent la preuve. Les jobs existants restent presents ; aucun job Spark
n'est lance par ces diagnostics.
