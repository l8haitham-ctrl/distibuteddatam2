CREATE TABLE business_term (
    id uuid PRIMARY KEY,
    name varchar(160) NOT NULL CHECK (length(btrim(name)) > 0),
    definition varchar(8000) NOT NULL CHECK (length(btrim(definition)) > 0),
    calculation_rule varchar(8000) NOT NULL DEFAULT '',
    owner varchar(160) NOT NULL DEFAULT '',
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT','VALIDATED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX business_term_name_idx ON business_term(lower(btrim(name)));
CREATE TABLE business_term_link (
    id uuid PRIMARY KEY,
    term_id uuid NOT NULL REFERENCES business_term(id) ON DELETE CASCADE,
    source_id varchar(128) NOT NULL,
    schema_name varchar(128) NOT NULL,
    table_name varchar(128) NOT NULL,
    column_name varchar(128),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX business_term_link_target_idx ON business_term_link
    (term_id, source_id, schema_name, table_name, coalesce(column_name, ''));
CREATE INDEX business_term_link_lookup_idx ON business_term_link(source_id, schema_name, table_name);
