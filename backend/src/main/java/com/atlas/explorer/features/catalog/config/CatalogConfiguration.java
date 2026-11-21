package com.atlas.explorer.features.catalog.config;
import com.atlas.explorer.features.catalog.connector.TrinoConnections;
import com.atlas.explorer.features.catalog.service.CatalogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="catalog.enabled",havingValue="true")
@EnableConfigurationProperties(CatalogProperties.class)
public class CatalogConfiguration {
    @Bean TrinoConnections trinoConnections(CatalogProperties properties){return new TrinoConnections(properties);}
    @Bean @DependsOn("jobsFlyway") CatalogService catalogService(TrinoConnections connections,DataSource jobsDataSource,CatalogProperties properties){return new CatalogService(connections,new JdbcTemplate(jobsDataSource),properties);}
}
