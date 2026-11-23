package com.atlas.explorer.features.quality.service;
public class QualityException extends RuntimeException {
    private final boolean invalid;
    public QualityException(boolean invalid){super(invalid?"Selectionner trois jobs termines, de la meme source et du schema attendu, pour Customer, Invoice et Payment.":"Controle qualite indisponible. Verifier les sources, le catalogue et le schema du profil metier.");this.invalid=invalid;}
    public boolean invalid(){return invalid;}
}
