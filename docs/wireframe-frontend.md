# Interface conforme au wireframe

Branche `feature/wireframe-frontend`, base `feature/angular-jobs-sql`.
La reference est le PDF fourni **Data Platform — Wireframe**, sept pages.

## Ecrans

- `/sources` : liste des sources, recherche sur le nom/l'hote/la base, compteurs
  et filtres, tests reels, acces a l'exploration, etats vide/aucun resultat.
- `/sources/new` : formulaire pleine page, sections connexion/serveur/
  authentification/parametres avances, panneau de test, succes et erreurs.
- `/explorer` : structure de la base et assistant d'extraction existant.
- `/storage` : resultats enregistres dans le catalogue, emplacements et acces SQL.
- `/jobs` et `/jobs/{id}` : historique et suivi des traitements existants.
- `/sql` : console existante, accessible depuis Storage, un resultat de job ou
  le menu du profil.

La navigation reprend les quatre entrees du PDF : Data Sources, Source
Explorer, Storage, Storage Jobs. L'habillage utilise une colonne de navigation
de 200 px, un bandeau de 54 px, des cartes a bordure fine, une palette grise et
la police Balsamiq Sans auto-hebergee. Les ecrans qui ne figurent pas dans le PDF
reprennent ces composants. Les vues etroites empilent les colonnes du formulaire
et permettent le defilement horizontal des tableaux sans reduire le texte.

## Donnees reelles et etats de reference

Le mode normal charge `/api/sources`. Aucune connexion fictive ne remplace une
erreur API. Les statuts et dates de test sont conserves en memoire pendant la
session et correspondent aux tests effectues depuis cette interface.

Le backend ne propose actuellement ni creation permanente de source ni suivi
progressif des phases JDBC. Le frontend conserve donc les capacites suivantes :

- **Enregistrer la connexion** verifie l'acces et lit la structure avec
  `/api/connections/explore`, puis conserve uniquement les metadonnees et le
  schema pour la session. Cette limite est affichee sous le formulaire. La
  connexion temporaire ne peut pas servir de source a un job Spark ; l'extraction
  reste disponible pour les sources configurees dans le backend.
- Aucun mot de passe n'entre dans le store de sources, localStorage ou
  sessionStorage. Le mot de passe est efface apres enregistrement et a la sortie.
- Pendant un test reel, l'interface affiche l'attente globale sans inventer
  de transitions. Un succes confirme les quatre etapes ; une erreur
  d'authentification signale les identifiants et l'ouverture de base ignoree.
  Les autres erreurs restent globales, car l'API n'en precise pas la phase.
- TLS reste obligatoire. Le delai du formulaire borne l'attente du test HTTP ;
  les delais JDBC du serveur peuvent interrompre l'operation avant.
- La version SQL Server n'est pas retournee par l'API : le panneau affiche
  « Non communiquee » plutot qu'une version inventee.

Pour comparer la mise en page aux sept pages du PDF, des donnees strictement
illustratives sont disponibles explicitement en mode maquette :

| URL | Reference |
| --- | --- |
| `/sources?maquette=1` | Liste de six sources |
| `/sources?maquette=1&state=empty` | Liste vide |
| `/sources?maquette=1` puis rechercher `oracle` | Aucun resultat |
| `/sources/new?maquette=1` | Formulaire initial |
| `/sources/new?maquette=1&state=testing` | Test en cours illustre |
| `/sources/new?maquette=1&state=success` | Test reussi illustre |
| `/sources/new?maquette=1&state=error` | Erreur d'authentification illustree |

Le mode maquette est identifie dans la navigation ; aucune action reseau de
connexion n'y est executee. Les noms et le profil du PDF sont reserves a ce mode.

## Validation Docker

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm --no-deps frontend npm test
docker compose build frontend
docker compose up -d --no-deps --wait frontend
```

Les tests existants des jobs, de la console SQL et de l'exploration sont
conserves. Les tests ajoutes couvrent recherche/filtres, statuts de test,
erreurs API, champs verrouilles, annulation, invalidation d'un test apres
modification et conservation exclusive des metadonnees de session.

La persistance serveur de nouvelles sources et les phases de test en direct
necessiteront une fonctionnalite backend separee. Le glossaire metier reste
au backlog ; les benchmarks restent la prochaine etape du parcours principal.

## Verification du 22 novembre 2026

- 27 tests Angular passes dans Docker, puis compilation de production.
- Comparaisons visuelles a 1 200 px des sept etats du PDF ; controle mobile a
  390 px, sans debordement horizontal du formulaire.
- Test de la source SQL Server configuree : passage reel de Non testee a
  Connectee avec date de test ; exploration de BusinessDB et lecture des
  candidats de partitionnement depuis l'assistant d'extraction.
- Test d'une connexion invalide : erreur API affichee, saisies conservees,
  aucune etape intermediaire annoncee comme reussie sans confirmation.
- Navigation depuis le stockage vers la console et execution de COUNT(*)
  sur `hive.runs.customer_51127d19` : 1 000 clients retrouves, pour verifier
  le parcours SQL conserve. Aucun nouveau job Spark n'a ete lance pour ce controle.
