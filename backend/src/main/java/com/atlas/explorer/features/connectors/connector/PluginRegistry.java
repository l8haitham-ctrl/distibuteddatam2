package com.atlas.explorer.features.connectors.connector;

import java.util.*;

public final class PluginRegistry {
    private final Map<String,RelationalPlugin> plugins;
    public PluginRegistry(Iterable<RelationalPlugin> providers){
        var found=new TreeMap<String,RelationalPlugin>();
        for(var plugin:providers){
            if(!plugin.engine().matches("[a-z][a-z0-9_]{0,31}")||found.putIfAbsent(plugin.engine(),plugin)!=null)
                throw new IllegalStateException("Duplicate or invalid connector plugin.");
        }
        plugins=Collections.unmodifiableMap(found);
    }
    public static PluginRegistry discover(){return new PluginRegistry(ServiceLoader.load(RelationalPlugin.class));}
    public RelationalPlugin get(String engine){var result=plugins.get(engine);if(result==null)throw new IllegalArgumentException("Unsupported connector.");return result;}
    public List<RelationalPlugin> all(){return List.copyOf(plugins.values());}
}
