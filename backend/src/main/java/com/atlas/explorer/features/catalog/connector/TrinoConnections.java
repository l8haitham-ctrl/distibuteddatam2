package com.atlas.explorer.features.catalog.connector;
import com.atlas.explorer.features.catalog.config.CatalogProperties;
import java.sql.*;
import java.util.Properties;
public class TrinoConnections {
    private final CatalogProperties config;
    public TrinoConnections(CatalogProperties config){this.config=config;}
    public Connection open(boolean registrar) throws SQLException {
        var properties=new Properties();properties.setProperty("user",registrar?"atlas_registrar":"atlas_reader");
        properties.setProperty("password",registrar?config.registrarPassword():config.readerPassword());
        properties.setProperty("source","atlas-api");
        return DriverManager.getConnection(config.jdbcUrl(),properties);
    }
}
