import os

from app.schemas.connections import ConnectionConfig


def configured_source() -> ConnectionConfig:
    return ConnectionConfig(
        host=os.getenv("SOURCE_HOST", "sqlserver"),
        database=os.getenv("SOURCE_DATABASE", "BusinessDB"),
        username=os.getenv("SOURCE_USERNAME", "lake_reader"),
        password=os.environ["SOURCE_PASSWORD"],
        trust_server_certificate=True,
    )
