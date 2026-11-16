from typing import Annotated

from fastapi import APIRouter, Depends, Query

from app.api.dependencies import get_exploration_service, get_source_service
from app.services.exploration import ExplorationService
from app.services.sources import SourceService

router = APIRouter(prefix="/sources/{source_id}/tables", tags=["Exploration"])


@router.get("")
def tables(source_id: str,
           sources: Annotated[SourceService, Depends(get_source_service)],
           exploration: Annotated[ExplorationService, Depends(get_exploration_service)]):
    return exploration.list_tables(sources.get(source_id))


@router.get("/{table}")
def table_details(source_id: str, table: str,
                  sources: Annotated[SourceService, Depends(get_source_service)],
                  exploration: Annotated[ExplorationService, Depends(get_exploration_service)],
                  schema: Annotated[str, Query(min_length=1, max_length=128)] = "dbo"):
    return exploration.describe_table(sources.get(source_id), schema, table)
