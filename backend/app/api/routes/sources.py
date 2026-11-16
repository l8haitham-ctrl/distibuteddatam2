from typing import Annotated

from fastapi import APIRouter, Depends

from app.api.dependencies import get_connection_service, get_source_service
from app.services.connections import ConnectionService
from app.services.sources import SourceService

router = APIRouter(prefix="/sources", tags=["Sources"])


@router.get("")
def sources(service: Annotated[SourceService, Depends(get_source_service)]):
    return service.list()


@router.post("/{source_id}/test")
def test_source(source_id: str,
                sources: Annotated[SourceService, Depends(get_source_service)],
                connections: Annotated[ConnectionService, Depends(get_connection_service)]):
    return connections.test(sources.get(source_id))
