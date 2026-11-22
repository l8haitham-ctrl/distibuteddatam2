#!/bin/sh
set -eu
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 \
  --set reader_password="$PG_SOURCE_READER_PASSWORD" <<'SQL'
CREATE ROLE atlas_reader LOGIN PASSWORD :'reader_password' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
ALTER ROLE atlas_reader SET default_transaction_read_only = on;
REVOKE ALL ON DATABASE business FROM PUBLIC;
GRANT CONNECT ON DATABASE business TO atlas_reader;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO atlas_reader;
CREATE TABLE customer(customer_id integer PRIMARY KEY, first_name varchar(100) NOT NULL,last_name varchar(100) NOT NULL,email varchar(255) NOT NULL,city varchar(100) NOT NULL,created_at timestamp NOT NULL,updated_at timestamp NOT NULL);
CREATE TABLE invoice(invoice_id integer PRIMARY KEY,customer_id integer NOT NULL REFERENCES customer(customer_id),invoice_date date NOT NULL,amount numeric(12,2) NOT NULL,status varchar(20) NOT NULL,updated_at timestamp NOT NULL);
CREATE TABLE payment(payment_id integer PRIMARY KEY,invoice_id integer NOT NULL REFERENCES invoice(invoice_id),payment_date date NOT NULL,amount numeric(12,2) NOT NULL,payment_method varchar(30) NOT NULL,updated_at timestamp NOT NULL);
INSERT INTO customer SELECT n,'First'||n,'Last'||n,'client'||n||'@example.test',CASE n%3 WHEN 0 THEN 'Rabat' WHEN 1 THEN 'Casablanca' ELSE 'Fes' END,timestamp '2024-01-01'+n*interval '1 minute',timestamp '2024-01-02'+n*interval '1 minute' FROM generate_series(1,1000)n;
INSERT INTO invoice SELECT n,1+(n-1)%1000,date '2024-01-01'+n%730,(100+(n%10000)/10.0)::numeric(12,2),CASE WHEN n%10<7 THEN 'paid' WHEN n%10<9 THEN 'partial' ELSE 'unpaid' END,timestamp '2026-01-01' FROM generate_series(1,5000)n;
INSERT INTO payment SELECT row_number() OVER(ORDER BY i.invoice_id,v.part),i.invoice_id,i.invoice_date+v.part,i.amount/2,CASE i.invoice_id%2 WHEN 0 THEN 'card' ELSE 'transfer' END,timestamp '2026-01-02' FROM invoice i CROSS JOIN (VALUES(1),(2))v(part) WHERE i.status='paid' OR (i.status='partial' AND v.part=1);
GRANT SELECT ON ALL TABLES IN SCHEMA public TO atlas_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO atlas_reader;
SQL
{ printf 'hostnossl all all all reject\n'; cat "$PGDATA/pg_hba.conf"; } > "$PGDATA/pg_hba.conf.next"
mv "$PGDATA/pg_hba.conf.next" "$PGDATA/pg_hba.conf"
touch "$PGDATA/source-ready"
