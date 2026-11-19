-- Destructive, explicit only. reseed.sh validates and confirms the target name.
USE [$(DatabaseName)];
GO
SET XACT_ABORT ON;
DECLARE @lock int;
EXEC @lock=sys.sp_getapplock @Resource=N'AtlasSeed',@LockMode='Exclusive',@LockOwner='Session',@LockTimeout=0;
IF @lock<0 THROW 51011,'Another seed is running.',1;
BEGIN TRY
    BEGIN TRANSACTION;
    -- SQL Server disallows TRUNCATE of referenced tables even when children are empty.
    ALTER TABLE dbo.Payment DROP CONSTRAINT FK_Payment_Invoice;
    ALTER TABLE dbo.Invoice DROP CONSTRAINT FK_Invoice_Customer;
    TRUNCATE TABLE dbo.Payment;
    TRUNCATE TABLE dbo.Invoice;
    TRUNCATE TABLE dbo.Customer;
    ALTER TABLE dbo.Invoice WITH CHECK ADD CONSTRAINT FK_Invoice_Customer FOREIGN KEY(customer_id) REFERENCES dbo.Customer(customer_id);
    ALTER TABLE dbo.Payment WITH CHECK ADD CONSTRAINT FK_Payment_Invoice FOREIGN KEY(invoice_id) REFERENCES dbo.Invoice(invoice_id);
    IF EXISTS(SELECT 1 FROM sys.extended_properties WHERE class=0 AND name='AtlasSeedInProgress')
        EXEC sys.sp_dropextendedproperty @name=N'AtlasSeedInProgress';
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    EXEC sys.sp_releaseapplock @Resource=N'AtlasSeed',@LockOwner='Session';
    THROW;
END CATCH;
GO
EXEC dbo.SeedBusinessData @Customers=$(SeedCustomers),@Invoices=$(SeedInvoices);
EXEC sys.sp_releaseapplock @Resource=N'AtlasSeed',@LockOwner='Session';
GO
