#!/usr/bin/env bash
set -eu
source /scripts/seed-config.sh

# A master connection can succeed before recovery of existing databases ends.
# The SQL script preserves existing data and can safely be retried.
for attempt in {1..12}; do
    if seed_sql -i /scripts/01-init.sql; then
        exit 0
    fi
    if [ "$attempt" -lt 12 ]; then
        echo "Initialization attempt $attempt failed; retrying in 5 seconds." >&2
        sleep 5
    fi
done

echo 'Initialization failed after 12 attempts. Check the SQL Server logs.' >&2
exit 1
