from app.connectors.factory import get_connector
from app.schemas.connections import ConnectionConfig


class ExplorationService:
    def list_tables(self, config: ConnectionConfig) -> dict:
        return {"tables": get_connector(config).list_tables()}

    def describe_table(self, config: ConnectionConfig, schema: str, table: str) -> dict:
        return get_connector(config).describe_table(schema, table)

    def explore(self, config: ConnectionConfig) -> dict:
        return {"database": config.database, "tables": get_connector(config).explore()}
