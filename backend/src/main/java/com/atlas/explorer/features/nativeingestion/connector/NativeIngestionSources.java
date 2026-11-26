package com.atlas.explorer.features.nativeingestion.connector;
import com.atlas.explorer.features.nativeingestion.dto.*;
import java.util.function.Function;
public final class NativeIngestionSources {
    private final ClickhouseExtraction clickhouse;
    public NativeIngestionSources(Function<String,String> env){clickhouse=new ClickhouseExtraction(env);}
    public boolean supports(String source){return "clickhouse".equals(source)&&clickhouse.enabled();}
    public String defaultSchema(String source){if(!supports(source))throw new IllegalArgumentException("Source native non configuree.");return clickhouse.database();}
    public NativeDataset describe(String source,String schema,String table){if(!supports(source))throw new IllegalArgumentException("Source native non configuree.");return clickhouse.describe(schema,table);}
    public RowStream rows(NativeDataset dataset){if(!supports(dataset.source()))throw new IllegalArgumentException("Source native non configuree.");return clickhouse.rows(dataset);}
}
