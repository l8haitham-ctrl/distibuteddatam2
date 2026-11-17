# Suite du projet

## Glossaire metier (demande utilisateur, a implementer plus tard)

- Definir des termes : chiffre d'affaires, facture impayee, paiement partiel, client actif.
- Relier chaque terme a une ou plusieurs tables et colonnes, avec le schema et
  l'identifiant de source pour eviter les ambiguites.
- Conserver definition, synonymes, responsable, regle de calcul et date de validation.
- Afficher les termes associes dans l'explorateur technique et retrouver les
  tables depuis le glossaire.
- Distinguer une definition metier validee d'une relation technique declaree.
- Prevoir une persistance dediee pour ces metadonnees ; ne pas modifier les bases sources.

## Pipeline analytique

Jobs d'extraction par lots, Parquet/CSV, stockage MinIO, partitions, catalogue,
Trino et mesures comparables. Ces composants ne sont pas encore implementes.
