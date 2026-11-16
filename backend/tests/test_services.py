import unittest
from unittest.mock import Mock, patch

from fastapi.testclient import TestClient

from app.api.dependencies import get_connection_service
from app.main import create_app
from app.schemas.connections import ConnectionConfig
from app.services.exploration import ExplorationService


class ServiceBoundaryTests(unittest.TestCase):
    def test_route_uses_replaceable_service_without_accessing_database(self):
        application = create_app()
        service = Mock()
        service.test.return_value = {"status": "connected", "database_name": "demo"}
        application.dependency_overrides[get_connection_service] = lambda: service
        with TestClient(application) as client:
            response = client.post("/connections/test", json={
                "host": "not-a-real-server", "database": "demo", "username": "reader",
                "password": "secret",
            })
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["database_name"], "demo")
        self.assertEqual(service.test.call_args.args[0].host, "not-a-real-server")

    def test_exploration_service_works_with_another_connector(self):
        config = ConnectionConfig(host="demo", database="demo", username="reader", password="secret")
        connector = Mock()
        connector.explore.return_value = [{"schema_name": "sales", "table_name": "Orders"}]
        with patch("app.services.exploration.get_connector", return_value=connector):
            result = ExplorationService().explore(config)
        self.assertEqual(result, {"database": "demo", "tables": connector.explore.return_value})
