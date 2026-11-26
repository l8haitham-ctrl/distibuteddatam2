"""Import the public Kaggle SQLite dataset into a new SQL Server database."""
import hashlib
from datetime import datetime
from itertools import zip_longest
import json
import os
from pathlib import Path
import shutil
import sqlite3
import tempfile
import urllib.request
import zipfile
import pymssql

URL = "https://www.kaggle.com/api/v1/datasets/download/hugomathien/soccer?datasetVersionNumber=10"
TABLES = ("Country", "League", "Team", "Player", "Match", "Team_Attributes", "Player_Attributes")
EXPECTED = dict(zip(TABLES, (11, 11, 299, 11060, 25979, 1458, 183978)))


def quote(name):
    return "[" + name.replace("]", "]]") + "]"


def sql_type(name, declared):
    if name in ("date", "birthday"):
        return "datetime2(0)"
    # SQLite's INTEGER affinity does not prevent fractional heights.
    if name == "height" or declared in ("NUMERIC", "REAL"):
        return "float"
    if declared == "INTEGER":
        return "bigint"
    if declared == "TEXT":
        return "nvarchar(max)"
    raise ValueError(f"Unsupported SQLite type: {declared}")


def download(directory):
    archive = Path(directory) / "soccer.zip"
    request = urllib.request.Request(URL, headers={"User-Agent": "DataPlatform/1.0"})
    with urllib.request.urlopen(request, timeout=120) as response, archive.open("wb") as output:
        shutil.copyfileobj(response, output)
    with archive.open("rb") as content:
        digest = hashlib.file_digest(content, "sha256").hexdigest()
    with zipfile.ZipFile(archive) as source:
        members = [item for item in source.infolist() if Path(item.filename).name == "database.sqlite"]
        if len(members) != 1:
            raise ValueError("Expected one database.sqlite in the Kaggle archive")
        database = Path(directory) / "database.sqlite"
        with source.open(members[0]) as data, database.open("wb") as output:
            shutil.copyfileobj(data, output)
    archive.unlink()
    return database, digest


def main():
    target = pymssql.connect(server="sqlserver", user="sa", password=os.environ["MSSQL_SA_PASSWORD"],
                             database="master", charset="UTF-8", login_timeout=15, timeout=120,
                             autocommit=True)
    cursor = target.cursor()
    cursor.execute("SELECT DB_ID('EuropeanSoccer')")
    if cursor.fetchone()[0] is not None:
        raise RuntimeError("EuropeanSoccer already exists; refusing to overwrite it.")
    print("Downloading Kaggle soccer version 10...", flush=True)
    with tempfile.TemporaryDirectory() as directory:
        database, digest = download(directory)
        source = sqlite3.connect(database)
        counts = {table: source.execute(f'SELECT COUNT(*) FROM "{table}"').fetchone()[0] for table in TABLES}
        if counts != EXPECTED:
            raise ValueError("Unexpected dataset row counts; nothing imported")
        cursor.execute("CREATE DATABASE EuropeanSoccer")
        cursor.execute("ALTER DATABASE EuropeanSoccer SET RECOVERY SIMPLE")
        cursor.execute("USE EuropeanSoccer")
        target.autocommit(False)
        report = {"dataset": "hugomathien/soccer", "version": 10, "archive_sha256": digest,
                  "database": "EuropeanSoccer", "tables": {}, "foreign_keys": []}
        try:
            for table in TABLES:
                columns = source.execute(f'PRAGMA table_info("{table}")').fetchall()
                definitions = [f'{quote(c[1])} {sql_type(c[1], c[2])}' +
                               (" NOT NULL PRIMARY KEY" if c[5] else " NULL") for c in columns]
                cursor.execute(f'CREATE TABLE dbo.{quote(table)} ({",".join(definitions)})')
                reader = source.execute(f'SELECT * FROM "{table}" ORDER BY id')
                query = f'INSERT INTO dbo.{quote(table)} VALUES ({",".join(["%s"] * len(columns))})'
                imported_rows = 0
                while rows := reader.fetchmany(100):
                    cursor.executemany(query, rows, batch_size=100)
                    imported_rows += len(rows)
                    if imported_rows % 5000 == 0:
                        print(f"Loading {table}: {imported_rows}/{counts[table]}", flush=True)
                cursor.execute(f'SELECT COUNT_BIG(*) FROM dbo.{quote(table)}')
                actual = cursor.fetchone()[0]
                if actual != counts[table]:
                    raise ValueError(f"Row count mismatch: {table}")
                # Read every row back, including long XML/text, fractional heights and NULLs.
                reader = source.execute(f'SELECT * FROM "{table}" ORDER BY id')
                cursor.execute(f'SELECT * FROM dbo.{quote(table)} ORDER BY id')
                for original, imported in zip_longest(reader, cursor):
                    expected = tuple(datetime.fromisoformat(value) if value is not None and
                                     columns[index][1] in ("date", "birthday") else value
                                     for index, value in enumerate(original))
                    if expected != imported:
                        raise ValueError(f"Value mismatch: {table}, id={original[0]}")
                report["tables"][table] = {"rows": actual, "columns": len(columns), "all_values_verified": True}
                print(f"Verified {table}: {actual} rows", flush=True)
            for table, key in (("Team", "team_api_id"), ("Player", "player_api_id")):
                cursor.execute(f'ALTER TABLE dbo.{quote(table)} ADD CONSTRAINT {quote("UQ_"+table+"_"+key)} UNIQUE ({quote(key)})')
            relations = [("League", "country_id", "Country", "id"),
                         ("Match", "country_id", "Country", "id"),
                         ("Match", "league_id", "League", "id"),
                         ("Match", "home_team_api_id", "Team", "team_api_id"),
                         ("Match", "away_team_api_id", "Team", "team_api_id"),
                         ("Team_Attributes", "team_api_id", "Team", "team_api_id"),
                         ("Player_Attributes", "player_api_id", "Player", "player_api_id")]
            for child, column, parent, key in relations:
                name = f"FK_{child}_{column}"
                cursor.execute(f'ALTER TABLE dbo.{quote(child)} WITH CHECK ADD CONSTRAINT {quote(name)} '
                               f'FOREIGN KEY ({quote(column)}) REFERENCES dbo.{quote(parent)} ({quote(key)})')
                cursor.execute(f'CREATE INDEX {quote("IX_"+child+"_"+column)} ON dbo.{quote(child)} ({quote(column)})')
                report["foreign_keys"].append(name)
            cursor.execute("CREATE USER lake_reader FOR LOGIN lake_reader")
            cursor.execute("GRANT SELECT ON SCHEMA::dbo TO lake_reader")
            target.commit()
        except Exception:
            target.rollback()
            raise
        Path("/results").mkdir(exist_ok=True)
        Path("/results/import.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print("EuropeanSoccer imported, verified and granted read-only access.", flush=True)
        source.close()
    target.close()


if __name__ == "__main__":
    main()
