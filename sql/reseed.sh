#!/usr/bin/env bash
set -eu
source /scripts/seed-config.sh
[[ "${CONFIRM_RESEED:-}" == "$SEED_DATABASE" ]] || { echo 'Refusing deletion: set CONFIRM_RESEED to the exact SEED_DATABASE name.' >&2; exit 1; }
echo "Explicit reseed requested for $SEED_DATABASE ($SEED_CUSTOMERS customers, $SEED_INVOICES invoices)."
seed_sql -i /scripts/03-reseed.sql
