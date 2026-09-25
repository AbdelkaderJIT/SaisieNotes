package tn.espacenote.notedemo.dto;

import tn.espacenote.notedemo.model.Enseignant;

public record EnseignantResponse(Long id, String nom, String prenom, String email) {

    public static EnseignantResponse from(Enseignant e) {
        return new EnseignantResponse(e.getId(), e.getNom(), e.getPrenom(), e.getEmail());
    }
}
