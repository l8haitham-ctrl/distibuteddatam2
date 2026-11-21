package com.atlas.explorer.features.catalog.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("catalog")
public record CatalogProperties(String jdbcUrl,String registrarPassword,String readerPassword,String schemaSuffix) {
    public CatalogProperties {schemaSuffix=schemaSuffix==null?"":schemaSuffix;if(!schemaSuffix.matches("[a-z_]{0,16}"))throw new IllegalArgumentException("Invalid catalogue suffix.");}
    @Override public String toString(){return "CatalogProperties[credentials=REDACTED]";}
}
