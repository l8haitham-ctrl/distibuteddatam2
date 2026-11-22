package com.atlas.explorer.features.nativesources.service;

public class NativeSourceException extends RuntimeException {
    public NativeSourceException(){super("Connexion ou exploration impossible. Verifiez le serveur, les identifiants, TLS et les droits de lecture.");}
}
