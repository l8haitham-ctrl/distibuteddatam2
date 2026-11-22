USE BusinessBenchmark;
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF OBJECT_ID(N'dbo.Invoice',N'U') IS NULL THROW 51100,'Initialize the large BusinessBenchmark dataset first.',1;
IF (SELECT COUNT_BIG(*) FROM dbo.Invoice WHERE invoice_id<=100000)<>100000 THROW 51101,'Expected at least 100000 deterministic invoices.',1;
BEGIN TRANSACTION;
IF SCHEMA_ID(N'benchmark') IS NULL EXEC(N'CREATE SCHEMA benchmark');
IF OBJECT_ID(N'benchmark.Invoice100k',N'U') IS NULL
BEGIN
    SELECT invoice_id,customer_id,invoice_date,amount,status,updated_at
    INTO benchmark.Invoice100k FROM dbo.Invoice WHERE invoice_id<=100000;
    ALTER TABLE benchmark.Invoice100k ADD CONSTRAINT PK_BenchmarkInvoice100k PRIMARY KEY(invoice_id);
END;
IF EXISTS(SELECT * FROM benchmark.Invoice100k EXCEPT SELECT * FROM dbo.Invoice WHERE invoice_id<=100000)
 OR EXISTS(SELECT * FROM dbo.Invoice WHERE invoice_id<=100000 EXCEPT SELECT * FROM benchmark.Invoice100k)
 THROW 51102,'Existing benchmark fixture differs; no rows were replaced.',1;
IF USER_ID(N'lake_reader') IS NULL CREATE USER lake_reader FOR LOGIN lake_reader;
GRANT CONNECT TO lake_reader;
GRANT SELECT ON OBJECT::benchmark.Invoice100k TO lake_reader;
COMMIT;
SELECT 'benchmark' AS source_id,'benchmark' AS schema_name,'Invoice100k' AS table_name,
 COUNT_BIG(*) AS row_count,CONVERT(varchar(40),SUM(amount)) AS amount_sum,
 SUM(CONVERT(bigint,invoice_id)) AS id_sum,
 CONVERT(varchar(10),MIN(invoice_date),23) AS min_date,CONVERT(varchar(10),MAX(invoice_date),23) AS max_date,
 (SELECT COUNT_BIG(*) AS row_count,CONVERT(varchar(40),SUM(amount)) AS amount_sum
  FROM benchmark.Invoice100k WHERE invoice_date>='20250101' AND invoice_date<'20250201' FOR JSON PATH,WITHOUT_ARRAY_WRAPPER) AS january,
 (SELECT status,COUNT_BIG(*) AS row_count,CONVERT(varchar(40),SUM(amount)) AS amount_sum FROM benchmark.Invoice100k GROUP BY status ORDER BY status FOR JSON PATH) AS statuses
FROM benchmark.Invoice100k FOR JSON PATH,WITHOUT_ARRAY_WRAPPER;
