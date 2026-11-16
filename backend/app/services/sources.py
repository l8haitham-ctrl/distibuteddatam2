from app.core.config import configured_source
from app.core.errors import SourceNotFound
from app.schemas.connections import ConnectionConfig


class SourceService:
    def get(self, source_id: str) -> ConnectionConfig:
        if source_id != "sqlserver":
            raise SourceNotFound()
        return configured_source()

    def list(self) -> list[dict]:
        config = self.get("sqlserver")
        return [{"id": "sqlserver", "engine": config.engine, "host": config.host,
                 "port": config.port, "database": config.database, "username": config.username}]
