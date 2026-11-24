package com.atlas.explorer.features.glossary.config;

import com.atlas.explorer.features.glossary.connector.*;
import com.atlas.explorer.features.glossary.service.GlossaryService;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="jobs.enabled",havingValue="true")
public class GlossaryConfiguration {
    @Bean @DependsOn("jobsFlyway") GlossaryRepository glossaryRepository(HikariDataSource jobsDataSource) {
        return new JdbcGlossaryRepository(new JdbcTemplate(jobsDataSource));
    }
    @Bean GlossaryService glossaryService(GlossaryRepository repository,SourceService sources,ExplorationService exploration) {
        return new GlossaryService(repository,sources,exploration);
    }
}
