# Parcours Angular : extraction et SQL

Branche `feature/angular-jobs-sql`. Dependance explicite :
`feature/partition-candidates` (base `feature/hive-catalog-trino`).
Aucune fusion dans la branche principale.

## Parcours

1. Ouvrir `http://localhost:3000/sources` et selectionner une table.
2. Cliquer sur **Extraire cette table**. Les connexions temporaires et la
   maquette restent limitees a l'exploration : elles ne sont pas des sources
   configurees pour le worker.
3. Choisir CSV ou Parquet, une compression compatible, les lots JDBC et le
   nombre de lecteurs demandes. Le backend determine les lecteurs effectifs.
4. Selectionner eventuellement des partitions. L'API verifie la cardinalite
   reelle dans SQL Server ; les cles primaires et colonnes non eligibles sont
   exclues. Changer la granularite annule la verification precedente. Le
   formulaire avertit du risque de petits fichiers avec plusieurs colonnes
   ou un decoupage journalier.
5. Verifier la demande JSON puis lancer. Le navigateur n'attend pas la fin de
   Spark : l'API retourne un identifiant et le suivi s'ouvre sur `/jobs/{id}`.
6. Consulter l'etat, les dates enregistrees et le resultat : lignes, fichiers,
   octets, temps d'extraction/ecriture, verification et catalogue, emplacement
   MinIO, manifeste, table et vue SQL. Les transitions intermediaires n'ont
   pas d'horodatage invente. Les anciens jobs sans catalogue sont identifies.
7. Ouvrir l'exemple dans `/sql`. La requete est pre-remplie et ne s'execute
   qu'apres un clic ou **Ctrl+Entree** (Cmd+Entree egalement accepte).

Le catalogue lateral insere un identifiant au curseur. Les resultats SQL
affichent le type de chaque colonne, les valeurs nulles, la duree, l'identifiant
Trino et les statistiques de lecture. Le plafond est de 1 000 lignes ; un
message signale les resultats tronques. L'API impose une seule instruction
en lecture seule et un delai maximal de 60 secondes.

## Suivi et erreurs

La liste contient les 100 derniers traitements. Elle est actualisee toutes
les deux secondes tant qu'un job visible est actif. Le polling s'arrete
quand tous sont termines, se suspend lorsque l'onglet est masque et reprend
au retour. Quitter la page annule ses requetes et minuteries.

Un double clic ne soumet pas deux demandes. Une erreur reseau, un delai
depasse ou une erreur serveur lors de la creation peut masquer une demande
deja recue : l'interface demande de consulter la liste et ne relance pas
automatiquement. `SUBMISSION_UNCERTAIN` est un code d'erreur backend affiche
comme **Soumission incertaine**, pas un nouvel etat persistant.

## Architecture frontend et design

Angular Router charge les pages sources, jobs et SQL a la demande. Les stores
utilisent des signals ; les stores jobs/SQL appartiennent a leur page et
liberent leurs abonnements a sa destruction. `PipelineApi` centralise les
appels HTTP relatifs a `/api`, transmis par Nginx au backend.

La maquette fournie guide la navigation laterale, les cartes et les etats
vides. Les couleurs existantes et les polices DM Sans / Manrope sont
conservees. Les nouvelles regles communes sont dans `styles/pipeline.css` :
cartes, tableaux operationnels, assistant et console responsive. Les icones
passent par le composant partage. Aucun historique de connexion ou mot de
passe n'est stocke dans le navigateur. La gestion persistante de plusieurs
sources et le glossaire metier restent au backlog.

## Execution et validation

```powershell
docker compose build backend frontend
docker compose up -d --no-deps --wait backend frontend
docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm --no-deps frontend npm test
```

Les builds frontend executent aussi les tests et la compilation de production.
Les tests couvrent les controles de partitions/compression, la revue JSON,
la soumission ambigue, l'annulation des verifications obsoletes, le polling,
la visibilite de l'onglet, les erreurs SQL, les limites de lignes, l'insertion
des identifiants, le raccourci clavier et l'affichage des valeurs comme texte.

Prochaine etape : benchmarks comparables sur le meme jeu de donnees, sans
implementer les bonus Iceberg, incremental ou relance.

### Verification locale du 21 novembre 2026

- 23 tests backend cibles passes avec la base SQL Server Compose : jobs,
  connexion, exploration et candidats de partitionnement.
- Build backend Docker : 29 tests unitaires passes ; 24 tests d'integration
  exclus de ce build (les verifications de connexion/exploration ci-dessus
  sont executees separement avec les conteneurs).
- 19 tests Angular couvrent les stores et les composants ; compilation de
  production dans Docker.
- Extraction declenchee depuis l'assistant :
  `b0aa7532-90fe-4bca-8eea-361748e53e90`, Parquet Snappy, dates par mois,
  lots JDBC de 1 000, quatre lecteurs demandes. Resultat : 5 000 lignes,
  96 fichiers, 331 451 octets, etat COMPLETED.
- Lien vers la console puis requete Trino sur `hive.runs.invoice_b0aa7532` :
  COUNT = 5 000, SUM(amount) = 4 892 425, en 1 990 ms pour cette execution.
  Ce test de fonctionnement ne constitue pas un benchmark comparatif.
- Ouverture directe et actualisation de `/jobs/{id}` verifiees apres ajout
  du chemin de base Angular. Navigation vers SQL et execution par Ctrl+Entree
  revalidees sur l'image finale : memes resultats en 998 ms.
