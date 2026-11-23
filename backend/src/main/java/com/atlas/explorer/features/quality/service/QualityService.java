package com.atlas.explorer.features.quality.service;
import com.atlas.explorer.features.quality.dto.QualityRequest;
import com.atlas.explorer.features.quality.connector.*;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.connection.connector.JdbcConnections;
import com.atlas.explorer.features.connectors.connector.PluginRegistry;
import com.atlas.explorer.features.jobs.service.JobService;
import com.atlas.explorer.features.catalog.connector.TrinoConnections;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Instant;
import java.util.*;
@Service
@ConditionalOnProperty(name={"jobs.enabled","catalog.enabled"},havingValue="true")
public class QualityService {
    private final JobService jobs;private final SourceService sources;private final PluginRegistry plugins;private final TrinoConnections trino;private final QualityReader reader;
    public QualityService(JobService jobs,SourceService sources,PluginRegistry plugins,TrinoConnections trino,QualityReader reader){this.jobs=jobs;this.sources=sources;this.plugins=plugins;this.trino=trino;this.reader=reader;}
    public Map<String,Object> report(QualityRequest request){
        var config=sources.get(request.sourceId());var plugin=plugins.get(config.engine());
        if(!Set.of("sqlserver","postgresql").contains(plugin.engine()))throw new QualityException(true);
        var tables=new LinkedHashMap<String,String>();var selected=Map.of("Customer",request.customerJobId(),"Invoice",request.invoiceJobId(),"Payment",request.paymentJobId());
        String schema=sources.defaultSchema(request.sourceId());
        for(var entry:selected.entrySet()){
            var job=jobs.get(entry.getValue());
            if(!"COMPLETED".equals(job.status())||!request.sourceId().equals(job.request().sourceId())||!schema.equals(job.request().schema())||!entry.getKey().equalsIgnoreCase(job.request().table())||job.result()==null)throw new QualityException(true);
            String table=Objects.toString(job.result().get("catalog_table"),"");
            if(!table.matches("hive\\.[a-z_][a-z0-9_]*\\.[a-z_][a-z0-9_]*"))throw new QualityException(true);
            tables.put(entry.getKey(),table);
        }
        var sourceQueries=QualityQueries.build(name->plugin.qualified(schema,plugin.engine().equals("postgresql")?name.toLowerCase(Locale.ROOT):name));
        try(var source=plugin.open(JdbcConnections.settings(config));var lake=trino.open(false)){
            var before=reader.read(source,sourceQueries);
            var stored=reader.read(lake,QualityQueries.build(tables::get));
            var after=reader.read(source,sourceQueries);
            var checks=QualityRules.evaluate(before,after,stored);
            return Map.of("report_id",UUID.randomUUID().toString(),"generated_at",Instant.now().toString(),"profile","customer_invoice_payment_v1","source_id",request.sourceId(),
                "jobs",selected,"catalog_tables",tables,"source_metrics",before,"lake_metrics",stored,"checks",checks,
                "status",checks.stream().allMatch(c->"passed".equals(c.get("status")))?"passed":"failed");
        }catch(Exception error){throw new QualityException(false);}
    }
}
