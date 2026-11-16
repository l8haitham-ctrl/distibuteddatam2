from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, SecretStr


class ConnectionConfig(BaseModel):
    model_config = ConfigDict(extra="forbid")

    engine: Literal["sqlserver"] = "sqlserver"
    host: str = Field(min_length=1, max_length=255, pattern=r"^[a-zA-Z0-9_.:-]+$")
    port: int = Field(default=1433, ge=1, le=65535)
    database: str = Field(min_length=1, max_length=128)
    username: str = Field(min_length=1, max_length=128)
    password: SecretStr = Field(min_length=1, max_length=128)
    # Only enable for a known local server with a self-signed certificate.
    trust_server_certificate: bool = False
