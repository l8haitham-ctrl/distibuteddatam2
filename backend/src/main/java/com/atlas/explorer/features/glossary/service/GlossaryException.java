package com.atlas.explorer.features.glossary.service;

public class GlossaryException extends RuntimeException {
    public enum Code { NOT_FOUND, CONFLICT, INVALID_TARGET }
    private final Code code;
    public GlossaryException(Code code) { super(switch(code) {
        case NOT_FOUND -> "Terme ou lien introuvable.";
        case CONFLICT -> "Ce nom de terme ou ce lien existe deja.";
        case INVALID_TARGET -> "Colonne introuvable ou cible invalide.";
    }); this.code=code; }
    public Code code() { return code; }
}
