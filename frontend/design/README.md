# Maquette : Atlas, explorateur de données

La maquette interactive est accessible avec `/?maquette=1`. Elle utilise les
memes composants Angular que l'application finale, avec un jeu explicite de metadonnees
illustratives dans `src/design/mockData.ts`. Elle ne contacte jamais l'API et
ne sert jamais de repli lorsque la vraie connexion echoue.

## Intention visuelle

- Navigation fixe vert profond, espace de travail clair, accents sauge.
- Typographie Manrope pour les titres et DM Sans pour les textes, avec replis
  systeme si les polices ne sont pas disponibles.
- Parcours en trois etapes : connexion, exploration, extraction future.
- Source active, arborescence des schemas/tables, puis detail de la selection.
- Deux vues du detail : colonnes et relations sortantes declarees.
- Formulaire dans une boite de dialogue, avec test, erreurs et chargement.

## Etats couverts dans l'application

Connexion en cours, source inaccessible avec relance, liste vide, recherche
sans resultat, chargement d'une table, erreur de lecture d'une table, test de
connexion reussi/refuse, connexion temporaire et retour a la source du projet.

Le mot de passe n'est conserve ni dans localStorage ni dans sessionStorage.
Une connexion personnalisee charge la structure puis abandonne les identifiants.
Pour actualiser cette structure, l'utilisateur renseigne a nouveau ses identifiants.
