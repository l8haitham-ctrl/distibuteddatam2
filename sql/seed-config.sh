#!/usr/bin/env bash
# Shared validation before any SQLCMD textual substitution.
set -eu
SEED_DATABASE=${SEED_DATABASE:-BusinessDB}
SEED_SCALE=${SEED_SCALE:-small}
[[ "$SEED_DATABASE" =~ ^[A-Za-z_][A-Za-z0-9_]{0,127}$ ]] || { echo 'Invalid SEED_DATABASE.' >&2; exit 1; }
case "$SEED_SCALE" in
    small) default_customers=1000; default_invoices=5000 ;;
    large) default_customers=100000; default_invoices=1000000 ;;
    *) echo 'SEED_SCALE must be small or large.' >&2; exit 1 ;;
esac
SEED_CUSTOMERS=${SEED_CUSTOMERS:-$default_customers}
SEED_INVOICES=${SEED_INVOICES:-$default_invoices}
for value in "$SEED_CUSTOMERS" "$SEED_INVOICES"; do
    [[ "$value" =~ ^[1-9][0-9]{0,7}$ ]] && ((value<=10000000)) || { echo 'Seed counts must be integers from 1 to 10000000.' >&2; exit 1; }
done
seed_sql() {
    /opt/mssql-tools18/bin/sqlcmd -S sqlserver -U sa -C -b -l 5 \
      -v DatabaseName="$SEED_DATABASE" SeedCustomers="$SEED_CUSTOMERS" SeedInvoices="$SEED_INVOICES" "$@"
}
