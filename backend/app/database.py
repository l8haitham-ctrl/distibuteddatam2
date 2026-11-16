import os
from contextlib import contextmanager

import pyodbc
from fastapi import HTTPException

from app.models import ConnectionConfig


def configured_source() -> ConnectionConfig:
    return ConnectionConfig(
        host=os.getenv("SOURCE_HOST", "sqlserver"),
        database=os.getenv("SOURCE_DATABASE", "BusinessDB"),
        username=os.getenv("SOURCE_USERNAME", "lake_reader"),
        password=os.environ["SOURCE_PASSWORD"],
        trust_server_certificate=True,
    )


def odbc_value(value: str) -> str:
    # ODBC braces protect semicolons and escaped closing braces in credentials.
    return "{" + value.replace("}", "}}") + "}"


def connection_string(config: ConnectionConfig) -> str:
    host = f"[{config.host}]" if ":" in config.host else config.host
    fields = {
        "DRIVER": "ODBC Driver 18 for SQL Server",
        "SERVER": f"tcp:{host},{config.port}",
        "DATABASE": config.database,
        "UID": config.username,
        "PWD": config.password.get_secret_value(),
        "Encrypt": "yes",
        "TrustServerCertificate": "yes" if config.trust_server_certificate else "no",
    }
    return ";".join(f"{key}={odbc_value(value)}" for key, value in fields.items())


def open_connection(config: ConnectionConfig):
    connection = pyodbc.connect(connection_string(config), timeout=5, autocommit=True)
    connection.timeout = 10  # Bound individual metadata queries as well as login.
    return connection


@contextmanager
def connect(config: ConnectionConfig):
    connection = None
    try:
        connection = open_connection(config)
        yield connection
    except pyodbc.Error as exc:
        # Never expose driver messages, connection strings or submitted credentials.
        state = str(exc.args[0]) if exc.args else ""
        if state == "28000":
            raise HTTPException(400, "Authentification refusee : verifier les identifiants et la base.") from None
        if state.startswith("08") or state in {"HYT00", "HYT01"}:
            raise HTTPException(503, "Connexion impossible : verifier le serveur, le port et le certificat.") from None
        raise HTTPException(502, "La base a refuse l'operation. Verifier les droits de lecture.") from None
    finally:
        if connection is not None:
            connection.close()


def rows(connection, query: str, *params) -> list[dict]:
    cursor = connection.cursor()
    try:
        cursor.execute(query, params)
        names = [column[0] for column in cursor.description]
        return [dict(zip(names, row)) for row in cursor.fetchall()]
    finally:
        cursor.close()
