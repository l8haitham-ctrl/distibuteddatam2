# Project instructions

- Implement each backend feature on a descriptive `codex/` branch.
- Commit and push that branch to `origin` after relevant checks pass.
- Do not merge into the default branch unless the user requests it.
- Never commit `.env`, credentials, database backups or generated caches.
- Keep HTTP routes in `api/routes`, use cases in `services`, database-specific
  code in `connectors`, models in `schemas`, configuration and domain errors
  in `core`, and administrative entry points in `scripts`.
- Routes delegate to services. Services and connectors must not import FastAPI.
- Preserve endpoint behavior during refactoring and run the backend tests
  against the Compose SQL Server for connection and exploration changes.
- Update README.md when changing setup, architecture or feature workflows.
