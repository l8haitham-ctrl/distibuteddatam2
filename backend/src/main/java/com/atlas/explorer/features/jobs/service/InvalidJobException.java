package com.atlas.explorer.features.jobs.service;

/** A fixed, safe explanation without source values or connection details. */
public class InvalidJobException extends RuntimeException {
    private final String field;
    public InvalidJobException(String field,String message) { super(message);this.field=field; }
    public String field() { return field; }
}
