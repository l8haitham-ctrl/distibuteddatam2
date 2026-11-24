# Connexion a la plateforme

Branche `feature/platform-authentication`, base `feature/explicit-source-selection`.
Cette fonctionnalite ajoute la connexion Angular et protege les API Spring Boot.
Les bases sources conservent leurs comptes techniques de lecture ; le compte
de la plateforme est distinct de sa, lake_reader et des comptes de stockage.

## Compte local

Pour une installation existante :

```powershell
powershell -NoProfile -File scripts/setup-login.ps1
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml up -d --build --wait
```

Le script ajoute uniquement APP_LOGIN_USERNAME=admin et un mot de passe aleatoire
APP_LOGIN_PASSWORD dans .env s'ils sont absents. Il ne remplace pas un compte
existant et n'affiche pas son mot de passe. Lire ce dernier dans le fichier
local .env pour se connecter a [Angular](http://localhost:3000). Ne pas le
committer ou le copier dans un rapport. Les nouvelles installations utilisant
setup-env.ps1 generent directement ce secret avec les autres identifiants.

Conserver les overrides actifs de l'installation. Un changement du compte dans
.env necessite de recreer le backend ; il n'affecte pas les comptes des bases.
Le backend refuse de demarrer sans mot de passe de 16 caracteres minimum et
72 octets UTF-8 maximum. Aucun identifiant de test n'est accepte par defaut en
production : la valeur de secours des tests existe uniquement dans src/test.

## Parcours et protection

- Une page ou un lien direct non authentifie redirige vers /login.
- Apres connexion, retour vers la page interne demandee, ou vers /sources.
- Le choix d'une base reste explicite ; le login ne connecte aucune source.
- Le menu utilisateur propose Se deconnecter. La session serveur est invalidee
  et la page est rechargee pour vider les donnees conservees en memoire par Angular.
- La session utilise un cookie ATLAS_SESSION HttpOnly, SameSite=Strict et une
  expiration apres 30 minutes d'inactivite. Le mot de passe n'est pas memorise
  par l'application ; aucun jeton n'est ecrit dans localStorage/sessionStorage.
- Les API et la documentation refusent les appels anonymes avec 401. Les
  ecritures demandent aussi un jeton CSRF lie a la session ; absence ou jeton
  invalide retourne 403. Les sondes de sante restent publiques pour Docker.

Spring Security gere la verification du mot de passe (BCrypt), la rotation de
l'identifiant de session et les protections CSRF. Angular recharge le jeton
apres connexion et l'envoie seulement aux API locales. Une ecriture en echec
n'est jamais rejouee automatiquement. Apres expiration, se reconnecter ; si une
ecriture renvoie 403, actualiser pour renouveler la session et le jeton.

Le cookie Secure est desactive uniquement pour HTTP localhost. Pour HTTPS,
definir APP_COOKIE_SECURE=true et configurer le proxy de confiance. Cette
version fournit un seul compte local, sans inscription, recuperation de mot
de passe, roles metier ou circuit d'approbation du glossaire. Une exposition
publique demande notamment HTTPS, limitation des tentatives et un fournisseur
d'identite adapte. Les sessions sont en memoire et perdues au redemarrage du
backend ; une execution multi-replicas demandera un stockage partage des sessions.

## API et Swagger

| Methode | Route | Usage |
| --- | --- | --- |
| GET | /auth/session | Etat de connexion et csrf_token, sans cache |
| POST | /auth/login | Formulaire URL-encode username/password avec X-CSRF-TOKEN |
| POST | /auth/logout | Deconnexion avec cookie de session et X-CSRF-TOKEN |

Pour Swagger, se connecter d'abord via Angular, ouvrir Documentation API dans
le menu, executer GET /auth/session et copier csrf_token dans Authorize (csrf).
Le navigateur conserve le cookie de session ; le champ Authorize transmet le
jeton CSRF, pas le mot de passe. Recuperer un nouveau jeton apres une nouvelle
connexion. Les endpoints login/logout sont geres par les filtres Spring Security.

Les clients REST doivent conserver les cookies, obtenir un jeton anonyme,
effectuer le login, puis relire /auth/session pour obtenir le jeton renouvele.
Les outils Docker de validation et benchmark utilisent ce parcours via
scripts/platform-session.mjs. Les rapports ne contiennent aucun cookie ou secret.

## Tests Docker

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml --profile tools run --rm --no-deps -e RUN_GLOSSARY_TESTS=true backend-tests mvn -B '-Dtest=AuthenticationTests,ConnectionIntegrationTests,DatabaseIntegrationTests,GlossaryIntegrationTests' test
docker compose build frontend backend
```

Les anciens tests d'API utilisent de vraies sessions via AuthenticatedHttp ;
la securite n'est pas desactivee pour les integrations. Les tests dedies
verifient les refus anonymes, cookies, changement de session, CSRF, mauvais
mot de passe et deconnexion. Angular teste les redirections, l'absence de
stockage de secrets, l'echec du login et la transmission limitee du jeton CSRF.

Validation du 24 novembre 2026 : 16 tests backend cibles passes, 51 tests passes
pendant la construction de l'image (95 decouverts, 44 integrations ignorees),
52 tests Angular passes dans 12 fichiers, six tests des outils benchmark et un
test du client de session. Le demarrage non web de l'image a aussi ete verifie,
pour conserver le fonctionnement des conteneurs d'initialisation.

Le parcours navigateur verifie [la page de connexion](evidence/platform-login.jpg),
le refus d'un mauvais mot de passe, [l'acces authentifie](evidence/platform-authenticated.jpg),
la conservation de session apres actualisation et [le refus apres deconnexion](evidence/platform-logged-out.jpg).
Les API reelles repondent 401 sans session, 403 sans CSRF lors d'une ecriture,
204 au login/logout et 401 apres logout. Le schema CSRF Swagger est present.
Les 44 jobs existants sont conserves et les 15 controles fonctionnels passent
avec une session sur une extraction deja terminee, sans nouveau job Spark.
Voir [la synthese](../validation/results/platform-authentication-2026-11-24.json)
et [le controle fonctionnel](../validation/results/authentication-2026-11-24.json).

References : [form login](https://docs.spring.io/spring-security/reference/7.1/servlet/authentication/passwords/form.html),
[CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html),
[sessions](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
