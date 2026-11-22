package com.atlas.explorer.features.connection.service;

import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connectors.connector.*;
import com.atlas.explorer.shared.domain.ExplorerException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@Service
public class SourceService {
    private final Environment environment;private final PluginRegistry plugins;
    public SourceService(Environment environment){this(environment,PluginRegistry.discover());}
    @Autowired public SourceService(Environment environment,PluginRegistry plugins){this.environment=environment;this.plugins=plugins;}
    private String value(String name){
        String property=switch(name){case "SOURCE_HOST"->"source.host";case "SOURCE_PORT"->"source.port";case "SOURCE_DATABASE"->"source.database";case "SOURCE_USERNAME"->"source.username";case "SOURCE_PASSWORD"->"source.password";case "SOURCE_TRUST_SERVER_CERTIFICATE"->"source.trust-server-certificate";default->name;};
        return environment.getProperty(property,environment.getProperty(name));
    }
    public ConnectionConfig get(String id){
        if(!SourceDefinitions.ids(this::value).contains(id))throw new ExplorerException(ExplorerException.Code.SOURCE_NOT_FOUND);
        try{var d=SourceDefinitions.get(id,this::value,plugins);var s=d.settings();return new ConnectionConfig(d.engine(),s.host(),s.port(),s.database(),s.username(),s.password(),s.trustCertificate());}
        catch(IllegalArgumentException e){throw new ExplorerException(ExplorerException.Code.UNAVAILABLE);}
    }
    public String defaultSchema(String id){return plugins.get(get(id).engine()).defaultSchema();}
    public List<Map<String,Object>> list(){return SourceDefinitions.ids(this::value).stream().map(id->{var c=get(id);return Map.<String,Object>of("id",id,"engine",c.engine(),"host",c.host(),"port",c.port(),"database",c.database(),"username",c.username(),"default_schema",plugins.get(c.engine()).defaultSchema());}).toList();}
}
