package com.atlas.explorer.features.jobs.config;

import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import com.atlas.explorer.features.jobs.connector.*;
import com.atlas.explorer.features.jobs.service.*;
import com.atlas.explorer.features.storage.service.StorageService;
import com.zaxxer.hikari.*;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="jobs.enabled",havingValue="true")
@EnableConfigurationProperties(JobProperties.class)
@EnableScheduling
public class JobConfiguration {
    @Bean(destroyMethod="close") HikariDataSource jobsDataSource(JobProperties properties) {
        var config=new HikariConfig();
        config.setJdbcUrl(properties.jdbcUrl());config.setUsername(properties.username());config.setPassword(properties.password());
        config.setMaximumPoolSize(4);config.setConnectionTimeout(5000);config.setPoolName("jobs-metadata");
        return new HikariDataSource(config);
    }
    @Bean(initMethod="migrate") Flyway jobsFlyway(HikariDataSource jobsDataSource) {
        return Flyway.configure().dataSource(jobsDataSource).locations("classpath:db/jobs").load();
    }
    @Bean @DependsOn("jobsFlyway") JobRepository jobRepository(HikariDataSource jobsDataSource) { return new JdbcJobRepository(new JdbcTemplate(jobsDataSource)); }
    @Bean JobService jobService(JobRepository repository,SourceService sources,ExplorationService exploration) { return new JobService(repository,sources,exploration); }
    @Bean JobEngine jobEngine(JobProperties properties) { return new SparkJobEngine(properties); }
    @Bean @ConditionalOnProperty(name="jobs.dispatch-enabled",havingValue="true",matchIfMissing=true)
    JobDispatcher jobDispatcher(JobRepository repository,JobEngine engine,StorageService storage,JobProperties properties) { return new JobDispatcher(repository,engine,storage,properties); }
}
