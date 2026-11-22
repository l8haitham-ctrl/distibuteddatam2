package com.atlas.explorer.features.connectors.config;
import com.atlas.explorer.features.connectors.connector.PluginRegistry;
import org.springframework.context.annotation.*;
@Configuration(proxyBeanMethods=false)
public class ConnectorConfiguration {
    @Bean PluginRegistry relationalPlugins(){return PluginRegistry.discover();}
}
