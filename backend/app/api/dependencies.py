from app.services.connections import ConnectionService
from app.services.exploration import ExplorationService
from app.services.sources import SourceService


def get_source_service() -> SourceService:
    return SourceService()


def get_connection_service() -> ConnectionService:
    return ConnectionService()


def get_exploration_service() -> ExplorationService:
    return ExplorationService()
