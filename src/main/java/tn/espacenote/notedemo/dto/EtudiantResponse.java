package tn.espacenote.notedemo.dto;

import tn.espacenote.notedemo.model.Etudiant;

public record EtudiantResponse(Long id, String numInscription, String nom, String prenom) {

    public static EtudiantResponse from(Etudiant e) {
        return new EtudiantResponse(e.getId(), e.getNumInscription(), e.getNom(), e.getPrenom());
    }
}
