# Project instructions

- Implement each feature on a descriptive `feature/` branch.
- Use neutral branch names, commit messages and documentation without assistant branding.
- Commit and push that branch to `origin` after relevant checks pass.
- Do not merge into the default branch unless the user requests it.
- Never commit `.env`, credentials, database backups or generated caches.
- Use Angular for the frontend and Java 21 / Spring Boot for the backend.
- Organize the backend vertically under `features/<feature>/` with `api`,
  `service`, `dto`, `connector`, and `config` subpackages as needed.
- Put only cross-feature errors/HTTP handlers under `shared/`.
- Controllers delegate to services. Services/connectors must not depend on HTTP.
- Each backend feature needs its own buildable branch, tests, commit and push.
- Keep branch dependencies explicit when one feature builds on another.
- Build, run and test through Docker; do not require host Java, Maven or Node.
- Preserve endpoint behavior during refactoring and run the backend tests
  against the Compose SQL Server for connection and exploration changes.
- Update README.md when changing setup, architecture or feature workflows.
- Keep the requested business glossary / table and column links in the roadmap
  until the user asks to implement them. Do not silently expand the current scope.
