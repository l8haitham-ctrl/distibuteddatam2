-- Development database. Re-running this script preserves existing data.
USE master;
GO
IF DB_ID(N'$(DatabaseName)') IS NULL
    EXEC(N'CREATE DATABASE [$(DatabaseName)]');
GO
USE [$(DatabaseName)];
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

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- Upgrade existing volumes once. Dynamic SQL avoids compilation against the old schema.
IF COL_LENGTH('dbo.Customer','updated_at') IS NULL
BEGIN
    ALTER TABLE dbo.Customer ADD updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_Customer_updated_at DEFAULT SYSUTCDATETIME() WITH VALUES;
    EXEC(N'UPDATE dbo.Customer SET updated_at=DATEADD(DAY,1,created_at)');
END;
IF COL_LENGTH('dbo.Invoice','updated_at') IS NULL
BEGIN
    ALTER TABLE dbo.Invoice ADD updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_Invoice_updated_at DEFAULT SYSUTCDATETIME() WITH VALUES;
    EXEC(N'UPDATE dbo.Invoice SET updated_at=DATEADD(DAY,2,CAST(invoice_date AS DATETIME2(0)))');
END;
IF COL_LENGTH('dbo.Payment','updated_at') IS NULL
BEGIN
    ALTER TABLE dbo.Payment ADD updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_Payment_updated_at DEFAULT SYSUTCDATETIME() WITH VALUES;
    EXEC(N'UPDATE dbo.Payment SET updated_at=DATEADD(DAY,1,CAST(payment_date AS DATETIME2(0)))');
END;
GO
:r /scripts/seed-procedure.sql
GO
EXEC dbo.SeedBusinessData @Customers=$(SeedCustomers), @Invoices=$(SeedInvoices);
GO