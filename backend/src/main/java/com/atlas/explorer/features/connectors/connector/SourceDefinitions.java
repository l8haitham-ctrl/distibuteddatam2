package com.atlas.explorer.features.connectors.connector;

import com.atlas.explorer.features.connectors.dto.*;
import java.util.*;
import java.util.function.Function;

/** The API and workers resolve the same source IDs from trusted environment configuration. */
public final class SourceDefinitions {
    private SourceDefinitions(){}
    public static List<String> ids(Function<String,String> environment){
        String raw=value(environment,"SOURCE_IDS","sqlserver");
        var ids=Arrays.stream(raw.split(",",-1)).map(String::trim).toList();
        if(ids.isEmpty()||new HashSet<>(ids).size()!=ids.size()||ids.stream().anyMatch(s->!s.matches("[a-z][a-z0-9_]{0,31}")))throw new IllegalArgumentException("Invalid configured source IDs.");
        return ids;
    }
    public static SourceDefinition get(String id,Function<String,String> environment,PluginRegistry plugins){
        if(!ids(environment).contains(id))throw new IllegalArgumentException("Unknown configured source.");
        String prefix=id.equals("sqlserver")?"SOURCE_":"SOURCE_"+id.toUpperCase(Locale.ROOT)+"_";
        String engine=value(environment,prefix+"ENGINE",id.equals("sqlserver")?"sqlserver":null);
        var plugin=plugins.get(engine);
        var settings=new JdbcSettings(value(environment,prefix+"HOST",id.equals("sqlserver")?"sqlserver":null),
            Integer.parseInt(value(environment,prefix+"PORT",Integer.toString(plugin.defaultPort()))),
            value(environment,prefix+"DATABASE",id.equals("sqlserver")?"BusinessDB":null),
            value(environment,prefix+"USERNAME",id.equals("sqlserver")?"lake_reader":null),
            value(environment,prefix+"PASSWORD",null),Boolean.parseBoolean(value(environment,prefix+"TRUST_SERVER_CERTIFICATE","false")));
        return new SourceDefinition(id,engine,settings);
    }
    private static String value(Function<String,String> environment,String name,String fallback){
        String value=environment.apply(name);if(value==null||value.isBlank())value=fallback;
        if(value==null)throw new IllegalArgumentException("Missing configured source property.");return value;
    }
}
