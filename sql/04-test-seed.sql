USE [$(DatabaseName)];
GO
SET NOCOUNT ON;
IF EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name IN('FK_Invoice_Customer','FK_Payment_Invoice') AND(is_disabled=1 OR is_not_trusted=1))
    THROW 51025,'Foreign keys must remain enabled and trusted.',1;
IF (SELECT COUNT_BIG(*) FROM dbo.Customer)<>$(SeedCustomers)
 OR (SELECT COUNT_BIG(*) FROM dbo.Invoice)<>$(SeedInvoices)
    THROW 51020, 'Unexpected seed counts.', 1;
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id IN(OBJECT_ID('dbo.Customer'),OBJECT_ID('dbo.Invoice'),OBJECT_ID('dbo.Payment'))
    AND name='updated_at' AND(is_nullable<>0 OR scale<>0 OR TYPE_NAME(system_type_id)<>'datetime2' OR default_object_id=0))
 OR (SELECT COUNT(*) FROM sys.columns WHERE object_id IN(OBJECT_ID('dbo.Customer'),OBJECT_ID('dbo.Invoice'),OBJECT_ID('dbo.Payment')) AND name='updated_at')<>3
    THROW 51021, 'updated_at must be non-null datetime2(0) with a default.', 1;
IF $(SeedInvoices)>=730 AND ((SELECT MIN(invoice_date) FROM dbo.Invoice)<>CONVERT(date,'20250101',112)
 OR(SELECT MAX(invoice_date) FROM dbo.Invoice)<>CONVERT(date,'20261231',112))
    THROW 51022, 'Seed does not cover both complete years.', 1;
IF EXISTS(SELECT 1 FROM dbo.Customer WHERE updated_at<>DATEADD(DAY,1,created_at))
 OR EXISTS(SELECT 1 FROM dbo.Invoice WHERE updated_at<>DATEADD(DAY,2,CAST(invoice_date AS datetime2(0))))
 OR EXISTS(SELECT 1 FROM dbo.Payment WHERE updated_at<>DATEADD(DAY,1,CAST(payment_date AS datetime2(0))))
    THROW 51023, 'Seed timestamps are not deterministic.', 1;
SELECT 'fingerprint' AS metric,
    (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(customer_id,first_name,last_name,email,city,created_at,updated_at)) FROM dbo.Customer) AS customers,
    (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(invoice_id,customer_id,invoice_date,amount,status,updated_at)) FROM dbo.Invoice) AS invoices,
    (SELECT CHECKSUM_AGG(BINARY_CHECKSUM(payment_id,invoice_id,payment_date,amount,payment_method,updated_at)) FROM dbo.Payment) AS payments;
PRINT 'Seed assertions passed.';
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @before datetime2(0)=SYSUTCDATETIME();
    INSERT dbo.Customer(customer_id,first_name,last_name,email,city,created_at)
      VALUES(2147483647,N'Test',N'Default',N'default-probe@example.com',N'Test',SYSUTCDATETIME());
    INSERT dbo.Invoice(invoice_id,customer_id,invoice_date,amount,status)
      VALUES(2147483647,2147483647,'20250101',1,'partial');
    INSERT dbo.Payment(payment_id,invoice_id,payment_date,amount,payment_method)
      VALUES(2147483647,2147483647,'20250102',0.5,'card');
    IF EXISTS(SELECT updated_at FROM dbo.Customer WHERE customer_id=2147483647 AND updated_at<@before)
      OR EXISTS(SELECT updated_at FROM dbo.Invoice WHERE invoice_id=2147483647 AND updated_at<@before)
      OR EXISTS(SELECT updated_at FROM dbo.Payment WHERE payment_id=2147483647 AND updated_at<@before)
        THROW 51024,'New rows did not receive their current updated_at default.',1;
    ROLLBACK;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
GO
