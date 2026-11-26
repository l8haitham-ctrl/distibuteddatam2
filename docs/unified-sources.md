# Liste unifiee des sources

Branche `feature/unified-source-list`, base `feature/clickhouse-native-exploration`.

La page `/sources` rassemble les profils relationnels de `GET /sources` et les
profils Redis, ClickHouse et MinIO de `GET /native/sources`. Les secrets ne sont
pas transmis au navigateur. Les identifiants internes des lignes natives ont
le prefixe `native:` pour eviter les collisions avec les sources relationnelles.

Les recherches, compteurs, filtres de statut et dates du dernier test incluent
tous ces profils. La colonne « Base / espace » affiche la base SQL, l'index
Redis ou le bucket MinIO. L'affichage initial ne teste aucune connexion et ne
lance aucune exploration automatiquement. Une erreur d'un fournisseur de
listes est affichee sans masquer les sources recues de l'autre fournisseur.

Chaque bouton **Tester** appelle le connecteur correspondant. Les sources JDBC
gardent leur diagnostic en direct. Les sources natives affichent le resultat
reel du test sans inventer des etapes JDBC. Les tests concurrents d'une meme
source sont bloques, leurs resultats restent visibles apres actualisation de
la liste dans la session courante.

**Explorer** ouvre directement la source choisie : `/explorer?source_id=...`
pour le relationnel, `/sources/native?source_id=...` pour les profils natifs.
Ce dernier lien declenche seulement l'exploration du profil explicitement choisi.
La page native sans parametre attend toujours une action utilisateur.

Cette evolution Angular conserve les API et les capacites propres a chaque
connecteur. Le backend, les bases et les jobs Spark ne sont pas modifies.
La demonstration utilise les six fichiers Compose du
[guide Redis/ClickHouse](redis-clickhouse-exploration.md).

```powershell
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml -f docker-compose.soccer.yml -f docker-compose.clickhouse.yml build frontend
docker compose -f docker-compose.yml -f docker-compose.connectors.yml -f docker-compose.analytics.yml -f docker-compose.benchmarks.yml -f docker-compose.soccer.yml -f docker-compose.clickhouse.yml up -d --no-deps --wait frontend
```

Validation locale : 63 tests Angular passes et build de production reussi dans
Docker. Le navigateur confirme les sept sources dans la meme liste, les tests
natifs Redis/ClickHouse, la conservation des statuts lors du retour a la liste
et l'ouverture directe de chaque exploration choisie. La capture est disponible
dans [les preuves visuelles](evidence/unified-sources.jpg).
