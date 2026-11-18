package com.atlas.explorer.shared.domain;

public class ExplorerException extends RuntimeException {
    public enum Code { AUTHENTICATION, UNAVAILABLE, QUERY, SOURCE_NOT_FOUND, TABLE_NOT_FOUND,
        STORAGE_UNAVAILABLE, OBJECT_NOT_FOUND, STORAGE_INVALID, JOB_NOT_FOUND, JOB_NOT_COMPLETE,
        JOB_ENGINE_UNAVAILABLE, JOB_RESULT_INVALID }
    private final Code code;
    public ExplorerException(Code code) { super(code.name()); this.code = code; }
    public Code code() { return code; }
}
