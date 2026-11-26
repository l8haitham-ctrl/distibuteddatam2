package com.atlas.explorer.features.nativeingestion.config;
import com.atlas.explorer.features.nativeingestion.connector.NativeIngestionSources;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
@Configuration(proxyBeanMethods=false)
public class NativeIngestionConfiguration {
    @Bean NativeIngestionSources nativeIngestionSources(Environment environment){return new NativeIngestionSources(environment::getProperty);}
}
