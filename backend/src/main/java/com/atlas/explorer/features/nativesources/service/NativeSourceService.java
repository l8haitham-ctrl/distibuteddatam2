package com.atlas.explorer.features.nativesources.service;

import com.atlas.explorer.features.nativesources.connector.NativeConnector;
import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class NativeSourceService {
    private final Map<String,NativeConnector> connectors;
    public NativeSourceService(List<NativeConnector> providers) {
        var map=new LinkedHashMap<String,NativeConnector>();
        for(var provider:providers)if(map.putIfAbsent(provider.engine(),provider)!=null)throw new IllegalArgumentException("Duplicate native connector");
        connectors=Collections.unmodifiableMap(map);
    }
    public List<Map<String,Object>> list(){return connectors.values().stream().map(provider->Map.<String,Object>of(
        "engine",provider.engine(),"name",provider.displayName(),"default_port",provider.defaultPort(),
        "capabilities",List.of("connection","exploration"))).toList();}
    public Map<String,Object> test(NativeConnection config){return execute(config,false);}
    public Map<String,Object> explore(NativeConnection config){return execute(config,true);}
    private Map<String,Object> execute(NativeConnection config,boolean explore) {
        try {
            var connector=connectors.get(config.engine());if(connector==null)throw new NativeSourceException();
            return explore?connector.explore(config):connector.test(config);
        }catch(Exception e){throw new NativeSourceException();}
    }
}
