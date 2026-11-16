from typing import Annotated

from fastapi import FastAPI, HTTPException, Query
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.database import configured_source, connect, rows
from app.explorer import describe_table, list_tables
from app.models import ConnectionConfig

app = FastAPI(
    title="Explorateur de bases de donnees",
    description="Etape 2 : connexion et exploration SQL Server. Les connexions soumises ne sont pas conservees.",
    version="0.2.0",
)


@app.exception_handler(RequestValidationError)
async def validation_error(request, exc):
    # Pydantic's default error payload includes invalid input, potentially a secret.
    return JSONResponse(status_code=422, content={"detail": [
        {"loc": list(error["loc"]), "msg": error["msg"], "type": error["type"]}
        for error in exc.errors()
    ]})


def source_config(source_id: str) -> ConnectionConfig:
    if source_id != "sqlserver":
        raise HTTPException(404, "Source inconnue.")
    return configured_source()


@app.get("/health", tags=["Service"])
def health():
    return {"status": "ok"}


@app.get("/sources", tags=["Sources"])
def sources():
    config = configured_source()
    return [{"id": "sqlserver", "engine": config.engine, "host": config.host,
             "port": config.port, "database": config.database, "username": config.username}]


def test(config: ConnectionConfig):
    with connect(config) as connection:
        result = rows(connection, "SELECT DB_NAME() AS database_name, CURRENT_USER AS database_user")[0]
    return {"status": "connected", **result}


@app.post("/connections/test", tags=["Connexions"])
def test_connection(config: ConnectionConfig):
    return test(config)


@app.post("/connections/explore", tags=["Connexions"])
def explore_connection(config: ConnectionConfig):
    with connect(config) as connection:
        tables = [describe_table(connection, t["schema_name"], t["table_name"])
                  for t in list_tables(connection)]
    return {"database": config.database, "tables": tables}


@app.post("/sources/{source_id}/test", tags=["Sources"])
def test_source(source_id: str):
    return test(source_config(source_id))


@app.get("/sources/{source_id}/tables", tags=["Exploration"])
def tables(source_id: str):
    with connect(source_config(source_id)) as connection:
        return {"tables": list_tables(connection)}


@app.get("/sources/{source_id}/tables/{table}", tags=["Exploration"])
def table_details(source_id: str, table: str,
                  schema: Annotated[str, Query(min_length=1, max_length=128)] = "dbo"):
    with connect(source_config(source_id)) as connection:
        return describe_table(connection, schema, table)
