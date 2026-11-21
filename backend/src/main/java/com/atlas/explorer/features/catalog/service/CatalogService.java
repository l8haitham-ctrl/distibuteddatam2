package com.atlas.explorer.features.catalog.service;
import com.atlas.explorer.features.catalog.connector.TrinoConnections;
import com.atlas.explorer.features.jobs.dto.StorageJob;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
public class CatalogService {
    private final TrinoConnections connections;
    private final JdbcTemplate jdbc;
    private final String suffix;
    private final ObjectMapper json=new ObjectMapper();
    public CatalogService(TrinoConnections connections,JdbcTemplate jdbc,com.atlas.explorer.features.catalog.config.CatalogProperties properties){this.connections=connections;this.jdbc=jdbc;this.suffix=properties.schemaSuffix();}
    public Map<String,Object> register(StorageJob job,Map<String,Object> manifest) {
        long started=System.nanoTime();var definition=new TableDefinition(job.id(),manifest,suffix);
        try(var connection=connections.open(true);var statement=connection.createStatement()) {
            statement.setQueryTimeout(60);
            statement.execute("CREATE SCHEMA IF NOT EXISTS hive."+definition.runSchema);
            statement.execute("CREATE TABLE IF NOT EXISTS "+definition.table+" ("+definition.columns+") WITH ("+definition.properties+")");
            try(var ddl=statement.executeQuery("SHOW CREATE TABLE "+definition.table)) {
                ddl.next();if(!definition.matchesLocation(ddl.getString(1)))throw new CatalogException(); // UUID-prefix collisions never point to another run.
            }
            if(definition.partitioned)statement.execute("CALL hive.system.sync_partition_metadata("+TableDefinition.literal(definition.runSchema)+","+TableDefinition.literal(definition.table.substring(definition.table.lastIndexOf('.')+1))+",'FULL')");
            // Queryability and row count are checked before exposing the current view.
            try(var count=statement.executeQuery("SELECT count(*) FROM "+definition.table)) {
                count.next();if(count.getLong(1)!=((Number)manifest.get("row_count")).longValue())throw new CatalogException();
            }
            var result=new LinkedHashMap<String,Object>(manifest);
            result.put("catalog_table",definition.table);result.put("current_view",definition.parquet?definition.view:null);
            Long newer=jdbc.queryForObject("""
                SELECT count(*) FROM storage_job WHERE status='COMPLETED' AND catalog_view=? AND created_at>?
                """,Long.class,definition.view,java.sql.Timestamp.from(job.createdAt()));
            if(definition.parquet && newer==0) {
                String schema=definition.view.substring(0,definition.view.lastIndexOf('.'));
                statement.execute("CREATE SCHEMA IF NOT EXISTS "+schema);
                statement.execute("CREATE OR REPLACE VIEW "+definition.view+" SECURITY INVOKER AS SELECT "+definition.projection+" FROM "+definition.table);
            }
            result.put("registration_ms",(System.nanoTime()-started)/1_000_000);
            result.put("example_query","SELECT * FROM "+definition.table+" LIMIT 100");
            return result;
        } catch(Exception exception){throw new CatalogException();}
    }
    public boolean discard(StorageJob job,Map<String,Object> manifest) {
        // Only this UUID's external metadata is removed. Never delete source files or the current view.
        TableDefinition d;
        try {d=new TableDefinition(job.id(),manifest,suffix);}catch(CatalogException invalid){return true;}
        try(var c=connections.open(true);var s=c.createStatement()) {
            s.setQueryTimeout(60);
            try(var exists=s.executeQuery("SELECT table_name FROM hive.information_schema.tables WHERE table_schema="+TableDefinition.literal(d.runSchema)+" AND table_name="+TableDefinition.literal(d.table.substring(d.table.lastIndexOf('.')+1)))) {
                if(!exists.next())return true;
            }
            try(var ddl=s.executeQuery("SHOW CREATE TABLE "+d.table)) {
                if(!ddl.next() || !d.matchesLocation(ddl.getString(1)))return true;
            }
            if(d.parquet) {
                boolean ownView=false;
                try(var view=s.executeQuery("SHOW CREATE VIEW "+d.view)) {ownView=view.next()&&view.getString(1).replace("\"","").contains(d.table);}
                catch(java.sql.SQLException missing) { /* A registration may fail before creating the view. */ }
                if(ownView) {
                    var previous=jdbc.queryForList("SELECT result_json FROM storage_job WHERE status='COMPLETED' AND catalog_view=? AND id<>? ORDER BY finished_at DESC LIMIT 1",String.class,d.view,job.id());
                    if(previous.isEmpty())s.execute("DROP VIEW "+d.view);
                    else {
                        var oldManifest=json.readValue(previous.getFirst(),new TypeReference<Map<String,Object>>(){});
                        var old=new TableDefinition(UUID.fromString(oldManifest.get("job_id").toString()),oldManifest,suffix);
                        s.execute("CREATE OR REPLACE VIEW "+old.view+" SECURITY INVOKER AS SELECT "+old.projection+" FROM "+old.table);
                    }
                }
            }
            s.execute("DROP TABLE IF EXISTS "+d.table);
            return true;
        }catch(Exception unavailable){return false;}
    }
    public List<Map<String,Object>> tables() {
        return jdbc.query("SELECT id,result_json FROM storage_job WHERE status='COMPLETED' AND catalog_table IS NOT NULL ORDER BY finished_at DESC",(rs,index)->{
            try {var result=json.readValue(rs.getString("result_json"),new TypeReference<Map<String,Object>>(){});result.put("job_id",rs.getObject("id").toString());
                result.put("location","s3://"+result.get("bucket")+"/"+result.get("prefix")+"/data/");return result;
            }catch(Exception e){throw new CatalogException();}
        });
    }
}
