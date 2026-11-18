#!/bin/sh
set -eu
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=ON_ERROR_STOP=1 --set=jobs_password="$JOBS_PASSWORD" <<'SQL'
CREATE ROLE atlas LOGIN PASSWORD :'jobs_password';
GRANT CONNECT ON DATABASE atlas TO atlas;
GRANT USAGE, CREATE ON SCHEMA public TO atlas;
CREATE DATABASE atlas_test OWNER atlas;
SQL
