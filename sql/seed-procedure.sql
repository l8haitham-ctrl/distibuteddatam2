CREATE OR ALTER PROCEDURE dbo.SeedBusinessData @Customers int, @Invoices int
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF @Customers NOT BETWEEN 1 AND 10000000 OR @Invoices NOT BETWEEN 1 AND 10000000
        THROW 51010, 'Seed counts must be between 1 and 10000000.', 1;
    DECLARE @lock int;
    EXEC @lock=sys.sp_getapplock @Resource=N'AtlasSeed', @LockMode='Exclusive', @LockOwner='Session', @LockTimeout=0;
    IF @lock<0 THROW 51011, 'Another seed is running.', 1;
    BEGIN TRY
        IF EXISTS(SELECT 1 FROM sys.extended_properties WHERE class=0 AND name='AtlasSeedInProgress')
            THROW 51012, 'Interrupted seed detected. Use the explicit reseed procedure.', 1;
        IF EXISTS(SELECT 1 FROM dbo.Customer) OR EXISTS(SELECT 1 FROM dbo.Invoice) OR EXISTS(SELECT 1 FROM dbo.Payment)
        BEGIN
            PRINT 'Existing data detected; seed skipped. Counts and values preserved.';
            EXEC sys.sp_releaseapplock @Resource=N'AtlasSeed', @LockOwner='Session';
            RETURN;
        END;
        EXEC sys.sp_addextendedproperty @name=N'AtlasSeedInProgress',@value=1;
        DECLARE @started datetime2(3)=SYSUTCDATETIME(), @start int=1;
        -- 100,000 reusable offsets. All inserts are set-based; the loops commit batches.
        ;WITH d(n) AS(SELECT n FROM(VALUES(0),(1),(2),(3),(4),(5),(6),(7),(8),(9))v(n))
        SELECT a.n+10*b.n+100*c.n+1000*e.n+10000*f.n AS n INTO #numbers
        FROM d a CROSS JOIN d b CROSS JOIN d c CROSS JOIN d e CROSS JOIN d f;
        CREATE UNIQUE CLUSTERED INDEX ix_numbers ON #numbers(n);
        WHILE @start<=@Customers
        BEGIN
            BEGIN TRANSACTION;
            INSERT dbo.Customer(customer_id,first_name,last_name,email,city,created_at,updated_at)
            SELECT id,CONCAT(N'Prenom',id),CONCAT(N'Nom',id),CONCAT(N'client',id,N'@example.com'),
                CHOOSE(1+id%20,N'Casablanca',N'Rabat',N'Fes',N'Marrakech',N'Tanger',N'Agadir',N'Oujda',N'Meknes',N'Kenitra',N'Tetouan',
                  N'Safi',N'El Jadida',N'Nador',N'Beni Mellal',N'Taza',N'Essaouira',N'Laayoune',N'Dakhla',N'Khouribga',N'Settat'),
                DATEADD(DAY,id%365,CONVERT(datetime2(0),'20240101',112)),
                DATEADD(DAY,1+id%365,CONVERT(datetime2(0),'20240101',112))
            FROM #numbers CROSS APPLY(VALUES(@start+n))v(id) WHERE id<=@Customers;
            COMMIT;
            SET @start+=100000;
        END;
        SET @start=1;
        WHILE @start<=@Invoices
        BEGIN
            BEGIN TRANSACTION;
            INSERT dbo.Invoice(invoice_id,customer_id,invoice_date,amount,status,updated_at)
            SELECT id,
                CASE WHEN id%10<6 THEN 1+((id-1)/10)%CASE WHEN @Customers/5>0 THEN @Customers/5 ELSE 1 END
                     ELSE 1+(CAST(id AS bigint)*7919)%@Customers END,
                DATEADD(DAY,(id-1)%730,CONVERT(date,'20250101',112)),
                CAST(50+(CAST(id AS bigint)*137%200000)/100.0 AS decimal(12,2)),
                CASE WHEN id%10=0 THEN 'unpaid' WHEN id%10 IN(8,9) THEN 'partial' ELSE 'paid' END,
                DATEADD(DAY,2+(id-1)%730,CONVERT(datetime2(0),'20250101',112))
            FROM #numbers CROSS APPLY(VALUES(@start+n))v(id) WHERE id<=@Invoices;
            COMMIT;
            SET @start+=100000;
        END;
        SET @start=1;
        WHILE @start<=@Invoices
        BEGIN
            BEGIN TRANSACTION;
            INSERT dbo.Payment(payment_id,invoice_id,payment_date,amount,payment_method,updated_at)
            SELECT 2*i.invoice_id-2+installment, i.invoice_id, DATEADD(DAY,delay,i.invoice_date),
                CASE WHEN installment=1 THEN CAST(ROUND(i.amount/2,2) AS decimal(12,2))
                     ELSE i.amount-CAST(ROUND(i.amount/2,2) AS decimal(12,2)) END,
                CHOOSE(1+i.invoice_id%3,'card','transfer','cash'),
                DATEADD(DAY,delay+1,CAST(i.invoice_date AS datetime2(0)))
            FROM dbo.Invoice i CROSS JOIN(VALUES(1,3),(2,10))p(installment,delay)
            WHERE i.invoice_id>=@start AND i.invoice_id<@start+50000
                AND(i.status='paid' OR(i.status='partial' AND installment=1));
            COMMIT;
            SET @start+=50000;
        END;
        EXEC sys.sp_dropextendedproperty @name=N'AtlasSeedInProgress';
        SELECT DB_NAME() AS database_name,@Customers AS customers,@Invoices AS invoices,
            (SELECT COUNT_BIG(*) FROM dbo.Payment) AS payments,
            DATEDIFF_BIG(MILLISECOND,@started,SYSUTCDATETIME()) AS generation_ms;
        EXEC sys.sp_releaseapplock @Resource=N'AtlasSeed', @LockOwner='Session';
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT>0 ROLLBACK;
        EXEC sys.sp_releaseapplock @Resource=N'AtlasSeed', @LockOwner='Session';
        THROW;
    END CATCH;
END;
