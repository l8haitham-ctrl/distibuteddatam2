from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.errors import (
    AuthenticationFailed, DatabaseOperationFailed, DatabaseUnavailable,
    DomainError, SourceNotFound, TableNotFound,
)

ERROR_RESPONSES = {
    AuthenticationFailed: (400, "Authentification refusee : verifier les identifiants et la base."),
    DatabaseUnavailable: (503, "Connexion impossible : verifier le serveur, le port et le certificat."),
    DatabaseOperationFailed: (502, "La base a refuse l'operation. Verifier les droits de lecture."),
    SourceNotFound: (404, "Source inconnue."),
    TableNotFound: (404, "Table introuvable ou non accessible."),
}


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(RequestValidationError)
    async def validation_error(request, exc):
        # Invalid input may contain credentials; return only error descriptions.
        return JSONResponse(status_code=422, content={"detail": [
            {"loc": list(error["loc"]), "msg": error["msg"], "type": error["type"]}
            for error in exc.errors()
        ]})

    @app.exception_handler(DomainError)
    async def domain_error(request, exc):
        status, message = ERROR_RESPONSES.get(type(exc), (500, "Erreur interne."))
        return JSONResponse(status_code=status, content={"detail": message})
