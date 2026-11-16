from fastapi import HTTPException

from app.database import rows


def list_tables(connection) -> list[dict]:
    return rows(connection, """
        SELECT s.name AS schema_name, t.name AS table_name
        FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
        WHERE t.is_ms_shipped = 0 ORDER BY s.name, t.name
    """)


def describe_table(connection, schema: str, table: str) -> dict:
    found = rows(connection, """
        SELECT t.object_id FROM sys.tables t
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        WHERE s.name = ? AND t.name = ? AND t.is_ms_shipped = 0
    """, schema, table)
    if not found:
        raise HTTPException(404, "Table introuvable ou non accessible.")
    object_id = found[0]["object_id"]
    columns = rows(connection, """
        SELECT c.name AS name, ty.name AS data_type, c.column_id AS position,
            c.max_length AS max_length_bytes, c.precision, c.scale,
            c.is_nullable AS nullable, c.is_identity AS is_identity,
            dc.definition AS default_value
        FROM sys.columns c JOIN sys.types ty ON ty.user_type_id = c.user_type_id
        LEFT JOIN sys.default_constraints dc ON dc.object_id = c.default_object_id
        WHERE c.object_id = ? ORDER BY c.column_id
    """, object_id)
    primary_key = rows(connection, """
        SELECT kc.name AS constraint_name, c.name AS column_name, ic.key_ordinal AS position
        FROM sys.key_constraints kc
        JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id
            AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE kc.parent_object_id = ? AND kc.type = 'PK' ORDER BY ic.key_ordinal
    """, object_id)
    foreign_keys = rows(connection, """
        SELECT fk.name AS constraint_name, pc.name AS column_name,
            rs.name AS referenced_schema, rt.name AS referenced_table,
            rc.name AS referenced_column, fkc.constraint_column_id AS position
        FROM sys.foreign_keys fk
        JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
        JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
        JOIN sys.tables rt ON rt.object_id = fkc.referenced_object_id
        JOIN sys.schemas rs ON rs.schema_id = rt.schema_id
        JOIN sys.columns rc ON rc.object_id = rt.object_id AND rc.column_id = fkc.referenced_column_id
        WHERE fk.parent_object_id = ? ORDER BY fk.name, fkc.constraint_column_id
    """, object_id)
    return {"schema_name": schema, "table_name": table, "columns": columns,
            "primary_key": primary_key, "foreign_keys": foreign_keys}
