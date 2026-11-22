package com.atlas.explorer.features.connectors.dto;

public record SourceDefinition(String id,String engine,JdbcSettings settings) {
    @Override public String toString(){return "SourceDefinition[id="+id+",engine="+engine+",credentials=REDACTED]";}
}
