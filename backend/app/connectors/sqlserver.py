from app.connectors.sqlserver_connection import connect, rows
from app.connectors.sqlserver_metadata import describe_table, list_tables
from app.schemas.connections import ConnectionConfig


class SQLServerConnector:
    def __init__(self, config: ConnectionConfig):
        self.config = config

    def test_connection(self) -> dict:
        with connect(self.config) as connection:
            return rows(connection, "SELECT DB_NAME() AS database_name, CURRENT_USER AS database_user")[0]

    def list_tables(self) -> list[dict]:
        with connect(self.config) as connection:
            return list_tables(connection)

    def describe_table(self, schema: str, table: str) -> dict:
        with connect(self.config) as connection:
            return describe_table(connection, schema, table)

    def explore(self) -> list[dict]:
        with connect(self.config) as connection:
            return [describe_table(connection, table["schema_name"], table["table_name"])
                    for table in list_tables(connection)]
