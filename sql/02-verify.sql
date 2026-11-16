USE BusinessDB;
GO
SET NOCOUNT ON;

-- Validate relationships and business consistency without assuming exact row counts.
IF EXISTS (
    SELECT 1 FROM dbo.Invoice i LEFT JOIN dbo.Customer c ON c.customer_id = i.customer_id
    WHERE c.customer_id IS NULL
)
    THROW 51000, 'Invoice without customer.', 1;

IF EXISTS (
    SELECT 1 FROM dbo.Payment p LEFT JOIN dbo.Invoice i ON i.invoice_id = p.invoice_id
    WHERE i.invoice_id IS NULL OR p.payment_date < i.invoice_date
)
    THROW 51001, 'Payment without invoice or with invalid date.', 1;

IF EXISTS (
    SELECT 1 FROM dbo.Invoice i
    LEFT JOIN (SELECT invoice_id, SUM(amount) AS paid FROM dbo.Payment GROUP BY invoice_id) p
        ON p.invoice_id = i.invoice_id
    WHERE (i.status = 'paid' AND COALESCE(p.paid, 0) <> i.amount)
       OR (i.status = 'partial' AND (COALESCE(p.paid, 0) <= 0 OR p.paid >= i.amount))
       OR (i.status = 'unpaid' AND COALESCE(p.paid, 0) <> 0)
)
    THROW 51002, 'Invoice status does not match payments.', 1;

SELECT N'Customer' AS table_name, COUNT_BIG(*) AS row_count FROM dbo.Customer
UNION ALL SELECT N'Invoice', COUNT_BIG(*) FROM dbo.Invoice
UNION ALL SELECT N'Payment', COUNT_BIG(*) FROM dbo.Payment;

SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dbo'
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT name AS foreign_key, OBJECT_NAME(parent_object_id) AS source_table,
    OBJECT_NAME(referenced_object_id) AS target_table
FROM sys.foreign_keys;

PRINT 'Verification passed.';
GO
