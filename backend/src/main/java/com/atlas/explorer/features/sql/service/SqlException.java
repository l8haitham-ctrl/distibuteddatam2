package com.atlas.explorer.features.sql.service;
public class SqlException extends RuntimeException {
    private final boolean unavailable;
    public SqlException(boolean unavailable){super(unavailable?"Service SQL indisponible.":"Requete refusee ou invalide ; utiliser SELECT, WITH, SHOW ou DESCRIBE en lecture seule.");this.unavailable=unavailable;}
    public boolean unavailable(){return unavailable;}
}
