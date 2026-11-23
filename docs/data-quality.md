# Rapport qualite Customer / Invoice / Payment

Branche `feature/data-quality-report`, base `feature/reproducible-benchmarks`.
Le profil metier `customer_invoice_payment_v1` compare la source SQL Server
ou PostgreSQL aux trois tables immuables d'extractions terminees dans Trino.
Il ne modifie ni les tables sources, ni les fichiers, ni le catalogue.

## Utilisation

Extraire Customer, Invoice et Payment depuis la meme source et le schema
par defaut de son plugin. Conserver leurs trois identifiants de jobs.
Dans Swagger ou POST /quality/reports :

```json
{
  "source_id": "sqlserver",
  "customer_job_id": "<UUID du job Customer>",
  "invoice_job_id": "<UUID du job Invoice>",
  "payment_job_id": "<UUID du job Payment>"
}
```

Les jobs doivent etre COMPLETED, porter les tables attendues et appartenir
a la meme source/schema. Le controle utilise catalog_table de chaque
resultat, pas les vues courantes susceptibles de changer apres une extraction.
Une selection incompatible retourne 422 ; une indisponibilite SQL retourne
503 sans message interne ou identifiants de connexion.

## Regles

- Completude : lignes dont au moins une colonne obligatoire est NULL.
- Unicite : identifiants distincts et doublons d'email client.
- Email : presence rudimentaire de @ et absence de chaine vide, pas validation RFC.
- Domaines : montants strictement positifs, statuts paid/partial/unpaid,
  moyens de paiement card/transfer/cash.
- Dates : updated_at ne precede pas la date metier ; paiement non anterieur a la facture.
- References : factures sans client et paiements sans facture.
- Soldes : facture paid totalement reglee, partial strictement entre zero
  et son montant, unpaid sans paiement.
- Reconciliation source/Trino : comptes, sommes d'identifiants, montants
  et compteurs d'anomalies, en decimal exact pour les montants.
- Stabilite : les agregats source sont relus apres les controles Trino.

Le resultat contient status=passed/failed, les observations et attentes par
regle, les metriques agregees, les identifiants de jobs et les tables utilisees.
Un echec de regle retourne 200 avec status=failed : ce n'est pas une erreur
HTTP d'infrastructure. Les donnees personnelles et mots de passe ne sont
pas inclus dans le rapport. Les delais JDBC sont limites a 30 s par requete.

## Limites

Ce profil correspond au modele metier de demonstration, pas a toutes les
tables d'une entreprise. Les sommes/comptages ne prouvent pas l'identite
de chaque cellule. Les extractions des trois tables et les lectures source
ne forment pas un snapshot transactionnel commun. Le controle de stabilite
signale certains changements concurrents, mais ne peut pas tous les detecter.
Executer la reconciliation sur une source stable pour une preuve comparable.
Les rapports sont renvoyes par l'API puis enregistres comme livrables JSON ;
aucun historique de rapports ni ecran de glossaire n'est ajoute.

## Verification

Les tests unitaires injectent des ecarts de montant, NULL et changement
source. Les tests d'integration executent les requetes sur des tables VALUES
Trino contenant volontairement doublons, orphelins, montants negatifs et
soldes incorrects, sans ecrire de donnees persistantes.
Le parcours reel compare trois extractions SQL Server aux sources.

Le rapport mesure est conserve dans
[le resultat JSON](../validation/results/data-quality-2026-11-23.json).

## Resultat de la validation

Le rapport SQL Server / Parquet / Trino a passe 92 controles, sans anomalie :

| Table | Lignes source et Trino | Somme des montants source et Trino |
| --- | ---: | ---: |
| Customer | 1000 | Sans objet |
| Invoice | 5000 | 4892425.00 |
| Payment | 8000 | 3913605.00 |

NULL obligatoires, doublons, orphelins, dates invalides et soldes incoherents :
zero dans la source et dans les extractions. Les agregats source sont restes
stables entre les deux lectures. Les tests du profil ont aussi execute les
regles sur la source PostgreSQL reelle et les anomalies VALUES dans Trino.
