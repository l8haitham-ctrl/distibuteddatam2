package com.atlas.explorer.features.connectors.dto;

/** Connection properties shared by the API and Spark; never serialized into job arguments. */
public record JdbcSettings(String host,int port,String database,String username,String password,boolean trustCertificate) {
    public JdbcSettings {
        if(host==null||!host.matches("[A-Za-z0-9_.:-]{1,255}")||port<1||port>65535||database==null||database.isBlank()
                ||username==null||username.isBlank()||password==null||password.isBlank())throw new IllegalArgumentException("Invalid source configuration.");
    }
    public String authority(){return (host.contains(":")?"["+host+"]":host)+":"+port;}
    @Override public String toString(){return "JdbcSettings[credentials=REDACTED]";}
}
