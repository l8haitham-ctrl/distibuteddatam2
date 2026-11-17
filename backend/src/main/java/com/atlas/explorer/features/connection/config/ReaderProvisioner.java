package com.atlas.explorer.features.connection.config;

import com.atlas.explorer.features.connection.connector.SqlServerConnections;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.provision", havingValue = "true")
public class ReaderProvisioner implements ApplicationRunner {
    private final ConfigurableApplicationContext context;
    public ReaderProvisioner(ConfigurableApplicationContext context) { this.context = context; }
    public void run(ApplicationArguments args) {
        var env = context.getEnvironment();
        var config = new ConnectionConfig("sqlserver", "sqlserver", 1433, "master", "sa", env.getRequiredProperty("MSSQL_SA_PASSWORD"), true);
        try (var connection = SqlServerConnections.open(config)) {
            try (var statement = connection.prepareStatement("""
                DECLARE @password nvarchar(128) = ?;
                DECLARE @sql nvarchar(max);
                IF SUSER_ID(N'lake_reader') IS NULL
                    SET @sql = N'CREATE LOGIN lake_reader WITH PASSWORD = ' + QUOTENAME(@password, '''');
                ELSE SET @sql = N'ALTER LOGIN lake_reader WITH PASSWORD = ' + QUOTENAME(@password, '''');
                EXEC(@sql);
                """)) {
                statement.setString(1, env.getRequiredProperty("SOURCE_PASSWORD")); statement.execute();
            }
            try (var statement = connection.createStatement()) {
                statement.execute("""
                    USE BusinessDB;
                    IF USER_ID(N'lake_reader') IS NULL CREATE USER lake_reader FOR LOGIN lake_reader;
                    IF IS_ROLEMEMBER('db_datareader', 'lake_reader') <> 1 ALTER ROLE db_datareader ADD MEMBER lake_reader;
                    GRANT VIEW DEFINITION TO lake_reader;
                    """);
            }
            System.out.println("Read-only source account ready.");
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Reader provisioning failed; check SQL Server readiness and administrator credentials.");
        }
        context.close();
    }
}
