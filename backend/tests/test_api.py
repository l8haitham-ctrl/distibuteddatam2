import unittest
from unittest.mock import patch

import pyodbc
from fastapi.testclient import TestClient

from app.database import configured_source, connect, connection_string, rows
from app.main import app
from app.models import ConnectionConfig


class ErrorHandlingTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_validation_does_not_echo_submitted_password(self):
        password = "sensitive-input-" * 20
        response = self.client.post("/connections/test", json={
            "engine": "oracle", "host": "sqlserver", "database": "BusinessDB",
            "username": "someone", "password": password,
        })
        self.assertEqual(response.status_code, 422)
        self.assertNotIn(password, response.text)
        self.assertNotIn('"input"', response.text)

    def test_driver_errors_do_not_leak_credentials(self):
        secret = "never-echo-this-password"
        with patch("app.database.open_connection", side_effect=pyodbc.Error("28000", secret)):
            response = self.client.post("/connections/test", json={
                "host": "sqlserver", "database": "BusinessDB", "username": "someone", "password": secret,
            })
        self.assertEqual(response.status_code, 400)
        self.assertNotIn(secret, response.text)

    def test_odbc_credentials_cannot_add_connection_options(self):
        config = ConnectionConfig(host="server", database="db", username="reader",
                                  password="abc};Encrypt=no;{xyz")
        self.assertIn("PWD={abc}};Encrypt=no;{xyz}", connection_string(config))
        self.assertTrue(connection_string(config).endswith("TrustServerCertificate={no}"))


class DatabaseIntegrationTests(unittest.TestCase):
    """Runs against the actual Compose SQL Server using the reader account."""

    def setUp(self):
        self.client = TestClient(app)

    def test_source_list_has_no_password(self):
        response = self.client.get("/sources")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()[0]["username"], "lake_reader")
        self.assertNotIn("password", response.text.lower())

    def test_connection_and_tables(self):
        response = self.client.post("/sources/sqlserver/test")
        self.assertEqual(response.status_code, 200, response.text)
        self.assertEqual(response.json()["database_user"], "lake_reader")
        tables = self.client.get("/sources/sqlserver/tables").json()["tables"]
        self.assertEqual({t["table_name"] for t in tables}, {"Customer", "Invoice", "Payment"})

    def test_types_and_relations(self):
        response = self.client.get("/sources/sqlserver/tables/Invoice")
        self.assertEqual(response.status_code, 200, response.text)
        table = response.json()
        amount = next(c for c in table["columns"] if c["name"] == "amount")
        self.assertEqual((amount["data_type"], amount["precision"], amount["scale"]), ("decimal", 12, 2))
        self.assertFalse(amount["nullable"])
        self.assertEqual(table["primary_key"][0]["column_name"], "invoice_id")
        self.assertEqual(table["foreign_keys"][0]["referenced_table"], "Customer")

    def test_missing_source_table_and_parameter_injection(self):
        self.assertEqual(self.client.get("/sources/unknown/tables").status_code, 404)
        self.assertEqual(self.client.get("/sources/sqlserver/tables/Missing").status_code, 404)
        response = self.client.get("/sources/sqlserver/tables/Invoice", params={"schema": "dbo'; DROP TABLE Invoice;--"})
        self.assertEqual(response.status_code, 404)
        self.assertEqual(self.client.get("/sources/sqlserver/tables/Invoice").status_code, 200)

    def test_submitted_connection_explores_structure_without_echoing_credentials(self):
        config = configured_source()
        payload = config.model_dump(exclude={"password"})
        payload["password"] = config.password.get_secret_value()
        response = self.client.post("/connections/explore", json=payload)
        self.assertEqual(response.status_code, 200, response.text)
        self.assertEqual(len(response.json()["tables"]), 3)
        self.assertNotIn(payload["password"], response.text)

    def test_wrong_password_returns_clear_error(self):
        config = configured_source()
        payload = config.model_dump(exclude={"password"})
        payload["password"] = "Incorrect_TestOnly_123!"
        response = self.client.post("/connections/test", json=payload)
        self.assertEqual(response.status_code, 400, response.text)
        self.assertNotIn(payload["password"], response.text)

    def test_reader_cannot_write_or_administer(self):
        with connect(configured_source()) as connection:
            permissions = rows(connection, """
                SELECT HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'SELECT') AS can_read,
                    HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'INSERT') AS can_insert,
                    HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'UPDATE') AS can_update,
                    HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'DELETE') AS can_delete,
                    IS_SRVROLEMEMBER('sysadmin') AS is_admin
            """)[0]
        self.assertEqual(permissions, {"can_read": 1, "can_insert": 0, "can_update": 0,
                                       "can_delete": 0, "is_admin": 0})


if __name__ == "__main__":
    unittest.main()
