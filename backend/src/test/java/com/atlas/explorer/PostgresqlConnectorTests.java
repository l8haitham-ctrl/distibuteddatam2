package com.atlas.explorer;
import com.atlas.explorer.features.connectors.connector.PostgresqlPlugin;
import com.atlas.explorer.features.connectors.dto.JdbcSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PostgresqlConnectorTests {
    @Test void urlEncodingCannotInjectConnectionOptionsAndTlsVerificationIsDefault(){
        var plugin=new PostgresqlPlugin();var config=new JdbcSettings("::1",5432,"business?sslmode=disable","reader","not-a-real-secret",false);
        assertEquals("jdbc:postgresql://[::1]:5432/business%3Fsslmode%3Ddisable",plugin.jdbcUrl(config));
        assertEquals("verify-full",plugin.properties(config).get("sslmode"));assertEquals("always",plugin.properties(config).get("readOnlyMode"));
        assertEquals("require",plugin.properties(new JdbcSettings("host",5432,"business","reader","test-only",true)).get("sslmode"));
        assertEquals("\"a\"\"b\"",plugin.quote("a\"b"));assertEquals("public",plugin.defaultSchema());
    }
}
