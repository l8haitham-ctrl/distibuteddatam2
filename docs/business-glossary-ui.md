# Interface du glossaire metier

Branche `feature/business-glossary-ui`, base explicite `feature/business-glossary`.
Le backend et sa migration PostgreSQL restent ceux de [la feature precedente](business-glossary.md).
Cette extension Angular utilise les API reelles ; aucune definition illustrative
n'est substituee en cas d'erreur et aucune nouvelle extraction n'est declenchee.

## Parcours

1. Ouvrir Glossaire metier dans la navigation principale.
2. Rechercher dans les noms et definitions, avec pagination serveur de 20 termes.
3. Creer un terme : nom, definition, regle de calcul, responsable et statut.
4. Consulter ou modifier sa fiche ; les associations existantes sont conservees.
5. Choisir une source configuree, puis une table et eventuellement une colonne.
   Sans colonne, le lien vise toute la table. Confirmer avec Associer au terme.
6. Suivre un lien de la fiche vers l'explorateur : la source et la table exactes
   sont chargees, la colonne liee est mise en evidence.
7. Dans l'explorateur, consulter Termes metier associes, filtrer par colonne ou
   suivre Associer un terme pour preselectionner cette donnee dans le glossaire.

Les fiches sont accessibles avec /glossary?term=UUID et peuvent etre rechargees.
Les liens vers l'explorateur portent source_id, schema_name, table_name et
column_name. La selection d'une table dans une autre source annule les lectures
de structure precedentes et efface la colonne selectionnee.

La suppression d'un lien ne supprime ni le terme, ni la colonne source. Il
n'existe pas de suppression de terme dans cette version. Une erreur de doublon
conserve le formulaire pour correction. Pendant une ecriture, les controles
concernes empechent une double soumission. Une ecriture en timeout n'est pas
relancee automatiquement : consulter les donnees avant de recommencer.

Les associations sont disponibles pour les sources configurees du backend.
Les connexions temporaires et le mode maquette n'enregistrent pas de liens.
Le glossaire est masque dans la navigation maquette pour conserver le wireframe
de reference. Les definitions et regles sont affichees en texte, sans HTML
interprete ni execution SQL. Le statut Valide reste declaratif.

## Construction et demarrage Docker

```powershell
git switch feature/business-glossary-ui
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml build frontend
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml up -d --no-deps --wait frontend
```

Conserver les overrides actifs de l'installation. Le backend doit deja
inclure feature/business-glossary. Pour une installation neuve, suivre
[le guide de livraison](delivery.md), puis construire cette branche complete.
Les tests Angular et le build de production sont executes par le Dockerfile.
Aucun Node local n'est necessaire.

Les tests couvrent le rendu texte, les doublons et la conservation du formulaire,
les changements de source, la pagination, l'association et le retrait d'un lien,
l'absence de soumission de jobs Spark, la recherche inverse et l'annulation des
reponses obsoletes, ainsi que l'ouverture de la bonne source depuis le glossaire.
L'interface reste adaptee aux petites largeurs avec des panneaux empiles.

## Validation du 24 novembre 2026

La suite Angular comporte 44 tests passes dans 11 fichiers. Le Dockerfile
construit aussi le bundle de production. Le parcours navigateur verifie un
terme explicitement nomme Reste a payer (demonstration), conserve en brouillon
dans PostgreSQL local : creation, modification du responsable, associations
Invoice.amount et Payment.amount, refus d'un doublon et rechargement de la fiche.
Ce terme sert d'exemple local ; aucune definition n'est injectee dans les migrations.

Le test visuel a revele une selection de colonne affichee incorrectement lors
de l'ouverture d'un lien profond. La liaison Angular du select a ete corrigee
et un test verifie a la fois le filtre transmis a l'API et l'option affichee.
La [fiche](evidence/glossary-page.jpg), [l'explorateur](evidence/glossary-explorer.jpg)
et [la preuve de validation](../validation/results/business-glossary-ui-2026-11-24.json)
accompagnent la feature. Le retour depuis l'explorateur preselectionne la source,
la table et la colonne ; la navigation utilise toujours le schema exact.
Les 44 jobs presents avant le parcours sont conserves : aucune extraction n'est lancee.

L'export initial d'une image de compilation a echoue par expiration de lease
Docker apres la reussite des premiers tests. L'image runtime finale a ensuite
ete construite et demarree avec succes, y compris le correctif du filtre.

Les synonymes, l'historique de versions, l'approbation par role, la generation
SQL et la surveillance des liens devenus obsoletes restent des evolutions.
Les bonus du pipeline restent exclus et aucune fusion vers master n'est realisee.
