#!/bin/sh
set -eu
psql --username "$POSTGRES_USER" --dbname postgres --set=ON_ERROR_STOP=1 --set=metastore_password="$METASTORE_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE metastore LOGIN PASSWORD %L', :'metastore_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname='metastore') \gexec
SELECT 'CREATE DATABASE metastore OWNER metastore' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='metastore') \gexec
SQL
