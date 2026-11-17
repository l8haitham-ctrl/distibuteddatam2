package com.atlas.explorer.shared.domain;

public class ExplorerException extends RuntimeException {
    public enum Code { AUTHENTICATION, UNAVAILABLE, QUERY, SOURCE_NOT_FOUND, TABLE_NOT_FOUND }
    private final Code code;
    public ExplorerException(Code code) { super(code.name()); this.code = code; }
    public Code code() { return code; }
}
