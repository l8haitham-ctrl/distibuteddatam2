package com.atlas.explorer;
import com.atlas.explorer.features.connectors.connector.*;
import com.atlas.explorer.features.connectors.dto.*;
import com.atlas.explorer.features.connectors.service.ConnectorService;
import com.atlas.explorer.features.connection.connector.*;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.shared.domain.ExplorerException;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class ConnectorPluginTests {
    public static class ExamplePlugin extends SqlCompatibleProvider { public String engine(){return "example";} }
    public static class SqlCompatibleProvider implements RelationalPlugin {
        public String engine(){return "example";}public String displayName(){return "Example JDBC";}
        public String driverClass(){return "com.microsoft.sqlserver.jdbc.SQLServerDriver";}public int defaultPort(){return 9999;}
        public String defaultSchema(){return "public";}public String jdbcUrl(JdbcSettings s){return "jdbc:example://"+s.authority();}
        public String defaultSchema(JdbcSettings settings){return settings.database();}
    }
    @Test void discoversAProviderFromAJarServiceDescriptorWithoutAnEngineSwitch()throws Exception{
        var path=Files.createTempFile("connector-test-",".jar");
        try{
            try(var jar=new JarOutputStream(Files.newOutputStream(path))){jar.putNextEntry(new JarEntry("META-INF/services/"+RelationalPlugin.class.getName()));jar.write((ExamplePlugin.class.getName()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));jar.closeEntry();}
            try(var loader=new URLClassLoader(new java.net.URL[]{path.toUri().toURL()},getClass().getClassLoader())){
                var registry=new PluginRegistry(ServiceLoader.load(RelationalPlugin.class,loader));
                assertEquals("public",registry.get("example").defaultSchema());assertEquals("dbo",registry.get("sqlserver").defaultSchema());
                assertTrue(new ConnectorService(registry).list().stream().anyMatch(p->p.get("engine").equals("example")));
            }
        }finally{Files.deleteIfExists(path);}
    }
    @Test void rejectsDuplicateProvidersAndUnsupportedEnginesBeforeConnecting(){
        assertThrows(IllegalStateException.class,()->new PluginRegistry(List.of(new SqlServerPlugin(),new SqlServerPlugin())));
        var error=assertThrows(ExplorerException.class,()->new ConnectorRegistry(List.of()).get("unknown"));assertEquals(ExplorerException.Code.CONNECTOR_NOT_FOUND,error.code());
    }
    @Test void configurationIsResolvedBySourceIdAndNeverLeaksPasswords(){
        var env=new HashMap<String,String>();env.put("SOURCE_IDS","sqlserver,warehouse");env.put("SOURCE_PASSWORD","private-first");
        env.put("SOURCE_WAREHOUSE_ENGINE","example");env.put("SOURCE_WAREHOUSE_HOST","warehouse");env.put("SOURCE_WAREHOUSE_DATABASE","BusinessDB");env.put("SOURCE_WAREHOUSE_USERNAME","reader");env.put("SOURCE_WAREHOUSE_PASSWORD","private-second");
        var registry=new PluginRegistry(List.of(new SqlServerPlugin(),new ExamplePlugin()));
        var source=SourceDefinitions.get("warehouse",env::get,registry);assertEquals(9999,source.settings().port());assertEquals("example",source.engine());
        assertEquals("BusinessDB",registry.get("example").defaultSchema(source.settings()));
        assertFalse(source.toString().contains("private"));assertFalse(source.settings().toString().contains("private"));
        assertThrows(IllegalArgumentException.class,()->SourceDefinitions.get("warehouse;injected",env::get,registry));
        env.put("SOURCE_IDS","sqlserver,sqlserver");assertThrows(IllegalArgumentException.class,()->SourceDefinitions.ids(env::get));
    }
    @Test void sqlServerKeepsDatabaseNamesAndCredentialsOutOfTheJdbcUrl(){
        var settings=new JdbcSettings("localhost",1433,"demo;encrypt=false","reader","private-password",false);var plugin=new SqlServerPlugin();
        assertEquals("jdbc:sqlserver://localhost:1433",plugin.jdbcUrl(settings));assertEquals("demo;encrypt=false",plugin.properties(settings).get("databaseName"));
        assertEquals("true",plugin.properties(settings).get("encrypt"));assertEquals("[a]]b]",plugin.quote("a]b"));
        assertFalse(plugin.jdbcUrl(settings).contains("private-password"));
    }
}
