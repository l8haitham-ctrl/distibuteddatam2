from app.connectors.factory import get_connector
from app.schemas.connections import ConnectionConfig


class ConnectionService:
    def test(self, config: ConnectionConfig) -> dict:
        return {"status": "connected", **get_connector(config).test_connection()}
