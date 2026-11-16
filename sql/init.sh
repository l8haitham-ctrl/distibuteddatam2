#!/usr/bin/env bash
set -eu

# A master connection can succeed before recovery of existing databases ends.
# The SQL script preserves existing data and can safely be retried.
for attempt in {1..12}; do
    if /opt/mssql-tools18/bin/sqlcmd -S sqlserver -U sa -C -b -l 5 -i /scripts/01-init.sql; then
        exit 0
    fi
    if [ "$attempt" -lt 12 ]; then
        echo "Initialization attempt $attempt failed; retrying in 5 seconds." >&2
        sleep 5
    fi
done

echo 'Initialization failed after 12 attempts. Check the SQL Server logs.' >&2
exit 1
