package com.atlas.explorer.features.quality.connector;
import java.sql.Connection;
import java.util.Map;
public interface QualityReader {
    Map<String,Map<String,String>> read(Connection connection,Map<String,String> queries) throws Exception;
}
