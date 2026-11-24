# Glossaire metier : premiere feature backend

Branche `feature/business-glossary`, base explicite `feature/release-readiness`.
Cette feature ajoute les metadonnees et l'API ; l'ecran Angular et les liens
visibles dans l'explorateur seront une feature suivante. Aucune fusion vers master.

## Modele et persistance

Un terme possede un UUID, un nom unique sans distinction de casse, une
definition, une regle de calcul textuelle facultative, un responsable facultatif,
un statut DRAFT ou VALIDATED et des dates de creation/modification.
Le nom et la definition sont nettoyes des espaces de debut et fin.

Chaque lien identifie un terme, une source configuree, un schema, une table et
eventuellement une colonne. Sans colonne, le lien concerne la table entiere.
Plusieurs sources ou colonnes peuvent correspondre au meme terme. Deux liens
identiques pour le meme terme sont refuses ; les noms differant uniquement par
la casse ne sont pas acceptes comme alias de la structure source.

Les tables business_term et business_term_link sont ajoutees par la migration
Flyway V3 dans PostgreSQL de metadonnees. Elle conserve les jobs existants et
ne modifie aucune base source. La feature reutilise le pool PostgreSQL de la
plateforme et est disponible lorsque JOBS_ENABLED=true, comme dans Compose.
Il n'y a pas de donnees de demonstration inserees automatiquement.

Le package features/glossary contient api, service, dto, connector et config.
Les services et le repository ne dependent pas de HTTP. Le controleur delegue
au service ; les erreurs SQL ne sont pas exposees dans les reponses.

## API et Swagger

| Methode | Route | Comportement |
| --- | --- | --- |
| POST | /glossary/terms | Cree un terme, reponse 201 et Location |
| GET | /glossary/terms?q=reste&limit=50&offset=0 | Recherche litterale dans nom/definition, liste resumee sans charger les liens |
| GET | /glossary/terms/{id} | Fiche avec tous ses liens |
| PUT | /glossary/terms/{id} | Remplace les champs du terme, conserve ses liens |
| POST | /glossary/terms/{id}/links | Verifie et ajoute un lien, reponse 201 |
| DELETE | /glossary/terms/{id}/links/{linkId} | Retire uniquement ce lien du terme, reponse 204 |
| GET | /glossary/links?source_id=sqlserver&schema_name=dbo&table_name=Invoice | Retrouve les liens de la table, y compris ses colonnes |

La recherche inverse accepte aussi column_name pour ne retourner que les liens
exacts de cette colonne. Elle ne recontacte pas la source : on peut retrouver
les metadonnees meme si cette base est temporairement indisponible.
Les listes utilisent limit entre 1 et 100 et offset positif ou nul ; l'ordre
est stable. Parcourir les pages jusqu'a une page plus courte que limit.

Codes : 404 terme/lien/source inconnu ; 409 nom ou lien deja present ;
422 corps invalide, cible absente, pagination invalide ou UUID mal forme ;
503 metadonnees indisponibles. Une connexion source impossible conserve le
comportement de l'API de connexion existante. Un parametre requis absent
peut retourner 400 selon la validation Spring.

Depuis [Swagger](http://localhost:8000/docs), creer le terme :

```json
{
  "name": "Reste a payer",
  "definition": "Montant restant du sur une facture.",
  "calculation_rule": "Invoice.amount moins la somme des Payment.amount de cette facture.",
  "owner": "Service finance",
  "status": "DRAFT"
}
```

Utiliser son UUID dans POST /glossary/terms/{id}/links :

```json
{
  "source_id": "sqlserver",
  "schema_name": "dbo",
  "table_name": "Invoice",
  "column_name": "amount"
}
```

Ajouter ensuite Payment.amount et les colonnes invoice_id utiles a la relation.
Le service consulte la liste des tables et les colonnes via les connecteurs
relationnels existants. Copier les noms exacts de l'explorateur ; une colonne
inventee ou une source inconnue ne produit aucun lien persiste.
Consulter ensuite la fiche, la recherche et la recherche inverse.

## Execution et tests Docker

```powershell
git switch feature/business-glossary
docker compose --profile tools run --rm --no-deps -e RUN_GLOSSARY_TESTS=true backend-tests mvn -B '-Dtest=GlossaryTests,GlossaryIntegrationTests,DatabaseIntegrationTests,ConnectionIntegrationTests' test
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml up -d --build --no-deps backend
```

Les integrations exigent atlas_test et effacent uniquement les termes qu'elles
ont crees, avec leurs liens. Elles verifient la persistance via une nouvelle
instance de repository, les doublons, la recherche, les changements de statut,
les cibles SQL Server reelles, les erreurs et la suppression d'un lien appartenant
a un autre terme. Les tests unitaires couvrent la validation avant ecriture,
la normalisation et la confidentialite des erreurs SQL.
Conserver uniquement les overrides actifs de son installation au redemarrage.
Avec l'override docker-compose.connectors.yml, RUN_PG_TESTS=true active aussi
un test reliant le meme terme a Invoice.amount dans SQL Server et a
invoice.amount dans PostgreSQL. La recherche inverse distingue les sources.

Validation du 24 novembre 2026 : 15 tests cibles passes sans echec ni test
ignore (quatre unitaires glossaire, quatre integrations glossaire, cinq
exploration et deux connexion SQL Server). La construction Docker execute
aussi la suite hors integration : 90 cas decouverts, 46 executes avec succes,
44 integrations ignorees faute de drapeaux d'environnement dans le build.
Les integrations du glossaire ont ete executees separement avec leurs services
reels, comme indique ci-dessus. Voir [la preuve](../validation/results/business-glossary-2026-11-24.json).

## Limites et suite

VALIDATED est un statut declaratif choisi par l'utilisateur : cette version
locale n'a pas de workflow d'approbation ni d'authentification multiutilisateur.
Le responsable est du texte, pas un compte utilisateur. La regle de calcul est
documentaire : elle n'execute pas de SQL et ne genere aucune extraction.
Les liens sont verifies a leur creation, sans surveillance automatique des
renommages/suppressions ulterieurs dans la source. Ils identifient la source
par sa configuration, pas une extraction ou une table Trino immuable.
La mise a jour d'un terme suit la derniere ecriture ; pas d'historique de versions.

Prochaine feature : page Glossaire Angular, formulaire de selection
source/schema/table/colonne et termes associes dans l'explorateur.
Les synonymes, audit, generation de SQL et validation des liens anciens restent
des evolutions distinctes ; les bonus du pipeline ne sont pas ajoutes.
