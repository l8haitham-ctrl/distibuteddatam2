package com.atlas.explorer.features.connectors.service;
import com.atlas.explorer.features.connectors.connector.PluginRegistry;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class ConnectorService {
    private final PluginRegistry plugins;
    public ConnectorService(PluginRegistry plugins){this.plugins=plugins;}
    public List<Map<String,Object>> list(){return plugins.all().stream().map(p->{
        boolean available;try{Class.forName(p.driverClass(),false,p.getClass().getClassLoader());available=true;}catch(ClassNotFoundException e){available=false;}
        return Map.<String,Object>of("engine",p.engine(),"name",p.displayName(),"default_port",p.defaultPort(),"default_schema",p.defaultSchema(),"available",available,
            "capabilities",List.of("connection","exploration","extraction"));
    }).toList();}
}
