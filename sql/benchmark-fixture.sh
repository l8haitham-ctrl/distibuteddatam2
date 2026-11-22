#!/usr/bin/env bash
set -euo pipefail
export SQLCMDPASSWORD="${MSSQL_SA_PASSWORD:?Configure the SQL Server administrator secret}"
exec /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -y 0 -w 65535 -i /tmp/benchmark-fixture.sql
