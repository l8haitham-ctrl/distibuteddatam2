from typing import Annotated

from fastapi import APIRouter, Depends

from app.api.dependencies import get_connection_service, get_exploration_service
from app.schemas.connections import ConnectionConfig
from app.services.connections import ConnectionService
from app.services.exploration import ExplorationService

router = APIRouter(prefix="/connections", tags=["Connexions"])


@router.post("/test")
def test_connection(config: ConnectionConfig,
                    service: Annotated[ConnectionService, Depends(get_connection_service)]):
    return service.test(config)


@router.post("/explore")
def explore_connection(config: ConnectionConfig,
                       service: Annotated[ExplorationService, Depends(get_exploration_service)]):
    return service.explore(config)
