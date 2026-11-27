---
name: atlas-auth
description: "Maintenir, expliquer ou vérifier la session, le login Angular, le logout et la protection CSRF dans Atlas."
---

> **Contexte de branche :** lire [references/branche.md](references/branche.md) avant toute intervention. Les descriptions globales et les liens de code figés concernent le snapshot `eee650a`, pas toutes les capacités de cette branche historique. Inspecter le code de la branche avant toute annonce de disponibilité.

# Authentification

## Portée et lecture ciblée

Appliquer ce skill pour la session, le login Angular, le logout et la protection CSRF. Lire les fichiers utiles à la tâche avant de changer le comportement ; les repères ci-dessous décrivent la version `eee650a`, pas les stashes en cours.

- [AuthConfiguration.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/auth/config/AuthConfiguration.java)
- [AuthController.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/auth/api/AuthController.java)
- [SessionInfo.java](https://github.com/l8haitham-ctrl/distibuteddatam2/blob/eee650a12f1895f44bcc61951babd0423cba95e1/backend/src/main/java/com/atlas/explorer/features/auth/dto/SessionInfo.java)

## Invariants propres à cette feature

Login traité par les filtres Spring Security, pas par AuthController. Cookie ATLAS_SESSION HttpOnly/SameSite strict ; session 30 minutes. GET /auth/session fournit csrf_token, y compris avant le login. POST /auth/login reçoit un formulaire username/password et X-CSRF-TOKEN ; succès 204. Recharger le jeton après authentification. Ne pas remplacer ce contrat par un JWT sans demande.

## Travail à effectuer

Identifier l’entrée utilisateur, le service responsable, la dépendance appelée et la sortie observable. Pour une modification, préserver les contrats HTTP et les fonctionnalités existantes. Respecter AGENTS.md et organiser le backend verticalement. Pour expliquer, relier les classes au parcours concret sans présenter un projet envisagé comme opérationnel.

## Vérification pertinente

Login correct/refusé, route anonyme 401, jeton absent 403, fixation de session, logout et garde Angular.

Pour une demande de contrats HTTP complets, lire [le skill API](../atlas-api/SKILL.md). Pour une modification de code ou une intervention Docker/Git, appliquer [le workflow de maintenance](../atlas-maintenance-flow/SKILL.md). Ne pas lancer de benchmark ou modifier une source juste pour documenter cette feature.
