#!/usr/bin/env bash
# Explicit integration test in a NEW, dedicated database; never runs at startup.
set -eu
export SEED_DATABASE=${SEED_DATABASE:-AtlasSeedChecks}
[[ "$SEED_DATABASE" == AtlasSeedChecks* ]] || { echo 'Test database must start with AtlasSeedChecks.' >&2; exit 1; }
source /scripts/seed-config.sh
export SEED_SCALE SEED_CUSTOMERS SEED_INVOICES
seed_sql -d master -Q "IF DB_ID(N'$SEED_DATABASE') IS NOT NULL THROW 51031, 'Test database already exists. Choose a fresh AtlasSeedChecks name.', 1;"
bash /scripts/init.sh
bash /scripts/test-seed.sh
fingerprint() { seed_sql -h -1 -W -i /scripts/04-test-seed.sql | sed -n '/^fingerprint /p'; }
before=$(fingerprint)
[[ -n "$before" ]] || { echo 'Missing seed fingerprint.' >&2; exit 1; }
bash /scripts/init.sh
[[ "$(fingerprint)" == "$before" ]] || { echo 'Idempotence check failed.' >&2; exit 1; }
CONFIRM_RESEED="$SEED_DATABASE" bash /scripts/reseed.sh
[[ "$(fingerprint)" == "$before" ]] || { echo 'Determinism check failed.' >&2; exit 1; }
bash /scripts/test-seed.sh
echo 'PASS: fresh seed, invariants, updated_at defaults, idempotence, explicit reseed determinism.'
