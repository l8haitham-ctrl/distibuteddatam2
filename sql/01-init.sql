-- Development database. Re-running this script preserves existing data.
USE master;
GO
IF DB_ID(N'BusinessDB') IS NULL
    EXEC(N'CREATE DATABASE BusinessDB');
GO
USE BusinessDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.Customer', N'U') IS NULL
    CREATE TABLE dbo.Customer (
        customer_id INT NOT NULL CONSTRAINT PK_Customer PRIMARY KEY,
        first_name NVARCHAR(100) NOT NULL,
        last_name NVARCHAR(100) NOT NULL,
        email NVARCHAR(255) NOT NULL CONSTRAINT UQ_Customer_Email UNIQUE,
        city NVARCHAR(100) NOT NULL,
        created_at DATETIME2(0) NOT NULL
    );

    IF OBJECT_ID(N'dbo.Invoice', N'U') IS NULL
    CREATE TABLE dbo.Invoice (
        invoice_id INT NOT NULL CONSTRAINT PK_Invoice PRIMARY KEY,
        customer_id INT NOT NULL,
        invoice_date DATE NOT NULL,
        amount DECIMAL(12, 2) NOT NULL,
        status VARCHAR(20) NOT NULL,
        CONSTRAINT FK_Invoice_Customer FOREIGN KEY (customer_id)
            REFERENCES dbo.Customer(customer_id),
        CONSTRAINT CK_Invoice_Amount CHECK (amount > 0),
        CONSTRAINT CK_Invoice_Status CHECK (status IN ('paid', 'partial', 'unpaid'))
    );

    IF OBJECT_ID(N'dbo.Payment', N'U') IS NULL
    CREATE TABLE dbo.Payment (
        payment_id INT NOT NULL CONSTRAINT PK_Payment PRIMARY KEY,
        invoice_id INT NOT NULL,
        payment_date DATE NOT NULL,
        amount DECIMAL(12, 2) NOT NULL,
        payment_method VARCHAR(30) NOT NULL,
        CONSTRAINT FK_Payment_Invoice FOREIGN KEY (invoice_id)
            REFERENCES dbo.Invoice(invoice_id),
        CONSTRAINT CK_Payment_Amount CHECK (amount > 0),
        CONSTRAINT CK_Payment_Method CHECK (payment_method IN ('card', 'transfer', 'cash'))
    );

    IF NOT EXISTS (SELECT 1 FROM dbo.Customer)
       AND NOT EXISTS (SELECT 1 FROM dbo.Invoice)
       AND NOT EXISTS (SELECT 1 FROM dbo.Payment)
    BEGIN
        -- Deterministic seed: 1,000 customers and 5,000 invoices over two years.
        ;WITH digits(n) AS (
            SELECT n FROM (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)) v(n)
        ), numbers(n) AS (
            SELECT 1 + a.n + 10 * b.n + 100 * c.n
            FROM digits a CROSS JOIN digits b CROSS JOIN digits c
        )
        INSERT INTO dbo.Customer (customer_id, first_name, last_name, email, city, created_at)
        SELECT n, CONCAT(N'Prenom', n), CONCAT(N'Nom', n),
            CONCAT(N'client', n, N'@example.com'),
            CHOOSE(1 + n % 5, N'Casablanca', N'Rabat', N'Fes', N'Marrakech', N'Tanger'),
            DATEADD(DAY, n % 365, CONVERT(DATETIME2(0), '20240101', 112))
        FROM numbers;

        ;WITH digits(n) AS (
            SELECT n FROM (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)) v(n)
        ), numbers(n) AS (
            SELECT 1 + a.n + 10 * b.n + 100 * c.n + 1000 * d.n
            FROM digits a CROSS JOIN digits b CROSS JOIN digits c CROSS JOIN digits d
        )
        INSERT INTO dbo.Invoice (invoice_id, customer_id, invoice_date, amount, status)
        SELECT n, 1 + (n - 1) % 1000,
            DATEADD(DAY, (n - 1) % 730, CONVERT(DATE, '20250101', 112)),
            CAST(50 + (n * 137 % 200000) / 100.0 AS DECIMAL(12, 2)),
            CASE WHEN n % 10 = 0 THEN 'unpaid'
                 WHEN n % 10 IN (8, 9) THEN 'partial' ELSE 'paid' END
        FROM numbers WHERE n <= 5000;

        -- Paid invoices receive two installments, partial invoices one half-payment.
        -- This deliberately exercises the one-to-many Invoice -> Payment relation.
        INSERT INTO dbo.Payment (payment_id, invoice_id, payment_date, amount, payment_method)
        SELECT 2 * i.invoice_id - 1,
            i.invoice_id, DATEADD(DAY, 3, i.invoice_date),
            CAST(ROUND(i.amount / 2, 2) AS DECIMAL(12, 2)),
            CHOOSE(1 + i.invoice_id % 3, 'card', 'transfer', 'cash')
        FROM dbo.Invoice i WHERE i.status IN ('paid', 'partial');

        INSERT INTO dbo.Payment (payment_id, invoice_id, payment_date, amount, payment_method)
        SELECT 2 * i.invoice_id,
            i.invoice_id, DATEADD(DAY, 10, i.invoice_date),
            i.amount - CAST(ROUND(i.amount / 2, 2) AS DECIMAL(12, 2)),
            CHOOSE(1 + i.invoice_id % 3, 'card', 'transfer', 'cash')
        FROM dbo.Invoice i WHERE i.status = 'paid';

        PRINT 'Seed created: 1000 customers, 5000 invoices, 8000 payments.';
    END
    ELSE
        PRINT 'Existing data detected; seed skipped.';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

SELECT N'Customer' AS table_name, COUNT_BIG(*) AS row_count FROM dbo.Customer
UNION ALL SELECT N'Invoice', COUNT_BIG(*) FROM dbo.Invoice
UNION ALL SELECT N'Payment', COUNT_BIG(*) FROM dbo.Payment;
GO
