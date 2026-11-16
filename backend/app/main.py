from fastapi import FastAPI

from app.api.errors import register_exception_handlers
from app.api.routes import connections, exploration, health, sources


def create_app() -> FastAPI:
    application = FastAPI(
        title="Explorateur de bases de donnees",
        description="Connexion et exploration SQL Server. Les connexions soumises ne sont pas conservees.",
        version="0.2.0",
    )
    register_exception_handlers(application)
    for router in (health.router, sources.router, connections.router, exploration.router):
        application.include_router(router)
    return application


app = create_app()
