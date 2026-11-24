# Demonstration du projet

Demarrer la version complete selon [le guide de livraison](delivery.md).
La demonstration principale utilise SQL Server ; PostgreSQL et Redis sont
des extensions optionnelles, pas des prerequisites du parcours du sujet.

## 1. Connexion et structure

Ouvrir http://localhost:3000/sources. Tester la source sqlserver puis cliquer
Explorer. Montrer Customer, Invoice, Payment, les types, cles et relations.
Le formulaire Ajouter une source permet une connexion temporaire ; son
mot de passe n'est pas conserve dans le navigateur. Les sources permanentes
de la demonstration sont configurees cote serveur.

## 2. Extraction et suivi

Selectionner Invoice puis Extraire cette table. Choisir Parquet/Snappy,
invoice_date avec granularite annee/mois, lot JDBC 1000, deux lecteurs Spark.
Verifier le JSON, lancer une seule fois, puis ouvrir le detail du traitement.
Expliquer QUEUED, SUBMITTING, RUNNING, REGISTERING et COMPLETED.
Attendre COMPLETED avant de presenter les resultats SQL. Les autres tables
peuvent etre extraites sans partitionnement.

Montrer les lignes, fichiers, octets, chemin MinIO, table du catalogue et
requete d'exemple du resultat. Un lot JDBC est un reglage de lecture ;
il ne correspond pas automatiquement a un fichier. Les donnees sont lues
par Spark puis converties et ecrites en flux.

## 3. SQL et metier

Depuis le resultat ouvrir la console SQL, ou le menu Projet local > Console SQL.
Utiliser catalog_table de l'execution pour figer la preuve. Les vues courantes
sont utiles a la demonstration mais peuvent pointer sur une extraction plus recente.

```sql
SELECT status, COUNT(*) AS invoices, SUM(amount) AS total
FROM hive.sqlserver_dbo.invoice
GROUP BY status
ORDER BY status;
```

Pour une demonstration de jointure, extraire aussi Customer et Payment :

```sql
WITH paid AS (
  SELECT invoice_id, SUM(amount) AS amount_paid
  FROM hive.sqlserver_dbo.payment GROUP BY invoice_id
)
SELECT i.status, COUNT(*) AS invoices,
       SUM(i.amount) AS invoiced, SUM(COALESCE(p.amount_paid,0)) AS paid
FROM hive.sqlserver_dbo.invoice i
JOIN hive.sqlserver_dbo.customer c ON c.customer_id=i.customer_id
LEFT JOIN paid p ON p.invoice_id=i.invoice_id
GROUP BY i.status ORDER BY i.status;
```

L'agregation des paiements avant la jointure evite de compter plusieurs fois
le montant d'une facture ayant plusieurs versements.

## 4. Qualite et performance

Dans Swagger POST /quality/reports, fournir les trois UUID des extractions.
Montrer les comptes, montants, anomalies et le statut global. Le rapport
fourni passe 92 controles ; expliquer aussi les limites des controles par
agregats et l'absence de snapshot commun sur une source active.

Ouvrir [le rapport des benchmarks](benchmark-report.md). Expliquer le gain de
stockage de Zstd, les 48 petits fichiers mensuels contre 2 sans partition,
et l'absence de gain de latence sur 100000 lignes malgre l'elagage reussi.
Les chiffres sont relies aux jobs/requetes bruts, pas codes en dur dans l'UI.

## Extensions et limites a annoncer

GET /connectors decouvre les plugins installes. PostgreSQL a ete valide en
extraction ; Redis et MinIO ont une exploration native distincte, accessible
par Connecter Redis ou MinIO. Redis est interroge via son catalogue Trino.
Ni l'extraction Redis vers Parquet, ni l'enregistrement automatique de tout
fichier MinIO ne sont annonces. Le glossaire, Iceberg et l'incremental restent
hors du perimetre livre. Les branches sont preparees pour integration, sans fusion.
