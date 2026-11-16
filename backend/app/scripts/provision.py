"""One-shot provisioning; admin credentials never enter the API container."""
import os

import pyodbc

from app.connectors.sqlserver_connection import open_connection
from app.schemas.connections import ConnectionConfig


def main():
    config = ConnectionConfig(host="sqlserver", database="master", username="sa",
                              password=os.environ["MSSQL_SA_PASSWORD"], trust_server_certificate=True)
    connection = open_connection(config)
    try:
        cursor = connection.cursor()
        try:
            # Parameter binding + QUOTENAME protects the dynamic password literal.
            cursor.execute("""
                DECLARE @password nvarchar(128) = ?;
                DECLARE @sql nvarchar(max);
                IF SUSER_ID(N'lake_reader') IS NULL
                    SET @sql = N'CREATE LOGIN lake_reader WITH PASSWORD = ' + QUOTENAME(@password, '''');
                ELSE
                    SET @sql = N'ALTER LOGIN lake_reader WITH PASSWORD = ' + QUOTENAME(@password, '''');
                EXEC(@sql);
            """, os.environ["SOURCE_PASSWORD"])
            cursor.execute("""
                USE BusinessDB;
                IF USER_ID(N'lake_reader') IS NULL CREATE USER lake_reader FOR LOGIN lake_reader;
                IF IS_ROLEMEMBER('db_datareader', 'lake_reader') <> 1
                    ALTER ROLE db_datareader ADD MEMBER lake_reader;
                GRANT VIEW DEFINITION TO lake_reader;
            """)
        finally:
            cursor.close()
    finally:
        connection.close()
    print("Read-only source account ready.")


if __name__ == "__main__":
    try:
        main()
    except pyodbc.Error:
        raise SystemExit("Source provisioning failed. Check SQL Server readiness and admin credentials.") from None
