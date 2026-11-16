from app.connectors.base import DatabaseConnector
from app.connectors.sqlserver import SQLServerConnector
from app.schemas.connections import ConnectionConfig


def get_connector(config: ConnectionConfig) -> DatabaseConnector:
    # Extend the registry and request schema when implementing another engine.
    connectors = {"sqlserver": SQLServerConnector}
    return connectors[config.engine](config)
