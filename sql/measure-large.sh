#!/usr/bin/env bash
set -eu
export SEED_DATABASE=${SEED_DATABASE:-BusinessBenchmark}
export SEED_SCALE=large
source /scripts/seed-config.sh
# Fail rather than measuring an existing database or replacing someone else's data.
seed_sql -d master -Q "IF DB_ID(N'$SEED_DATABASE') IS NOT NULL THROW 51030, 'Benchmark database already exists. Choose a new SEED_DATABASE.', 1;"
started=$(date +%s%3N)
bash /scripts/init.sh
finished=$(date +%s%3N)
echo "Initialization wall time: $((finished-started)) ms"
bash /scripts/test-seed.sh
seed_sql -d "$SEED_DATABASE" -Q "SET NOCOUNT ON; SELECT type_desc,CAST(size*8.0/1024 AS decimal(12,2)) AS allocated_mb,CAST(FILEPROPERTY(name,'SpaceUsed')*8.0/1024 AS decimal(12,2)) AS used_mb FROM sys.database_files; SELECT CAST(total_log_size_in_bytes/1048576.0 AS decimal(12,2)) AS log_allocated_mb,CAST(used_log_space_in_bytes/1048576.0 AS decimal(12,2)) AS log_used_mb FROM sys.dm_db_log_space_usage;"
