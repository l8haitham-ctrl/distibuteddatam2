# Choix explicite de la source

Branche `feature/explicit-source-selection`, base `feature/business-glossary-ui`.

La page d'accueil reste /sources. Elle liste les sources configurees sans
tester leur connexion ni lire leurs tables. Le bouton Tester ne teste que la
source choisie ; Explorer ouvre /explorer?source_id=IDENTIFIANT, puis verifie
cette connexion et charge sa structure. Les identifiants restent dans la
configuration du backend, sans nouvelle saisie pour une source deja configuree.

L'ouverture directe de /explorer dans une nouvelle session affiche Choisissez
une source a explorer et un lien vers la liste. Actualiser est desactive tant
qu'aucune source n'a ete selectionnee. La source choisie reste en memoire lors
des navigations de la session ; aucune preference ni aucun secret n'est ecrit
dans le stockage du navigateur. Choisir une source permet de revenir a la liste.

Un lien explicite depuis le glossaire peut toujours ouvrir directement une
source, une table et une colonne. Une source inconnue affiche une erreur sans
repli sur SQL Server. Reessayer et Actualiser reutilisent exclusivement la
source demandee, y compris apres un echec de connexion. Le mode maquette reste
illustratif ; les connexions temporaires conservent leur parcours de session.

Ce changement concerne le choix de source dans Angular. Les services Docker
de demonstration, dont SQL Server, demarrent toujours avec Compose et leurs
sondes de sante continuent a fonctionner. Le stockage des sources ajoutees par
le formulaire reste une autre fonctionnalite : elles sont temporaires dans
cette version. Les sources configurees cote backend sont conservees.

## Validation et execution

Le Dockerfile frontend execute les tests Angular et construit le bundle de
production. La suite verifie l'absence de requete a la construction de
l'explorateur, la selection explicite, les echecs, les reprises sur la meme
source et la navigation precise depuis le glossaire.

```powershell
git switch feature/explicit-source-selection
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml build frontend
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml up -d --no-deps --wait frontend
```

Conserver les overrides deja actifs. Aucun changement de base, migration,
reseed ou changement de secret n'est necessaire. Aucune fusion vers master.

Validation du 24 novembre 2026 : 47 tests Angular passes dans 11 fichiers,
bundle de production construit et frontend demarre avec succes. Le navigateur
verifie [l'explorateur sans choix](evidence/source-selection-empty.jpg),
[les sources non testees](evidence/source-selection-list.jpg), puis
[PostgreSQL apres selection et rechargement](evidence/source-selection-postgresql.jpg).
