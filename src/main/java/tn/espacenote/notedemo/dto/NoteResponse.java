package tn.espacenote.notedemo.dto;

import tn.espacenote.notedemo.model.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Vue "à plat" d'une note : évite de sérialiser les relations lazy de l'entité.
public record NoteResponse(
        Long id,
        BigDecimal valeur,
        Long examenId,
        Long matiereId,
        String matiereLibelle,
        Long etudiantId,
        String etudiantNumInscription,
        String etudiantNom,
        String etudiantPrenom,
        Long enseignantId,
        LocalDateTime dateSaisie,
        LocalDateTime dateModification) {

    public static NoteResponse from(Note n) {
        return new NoteResponse(
                n.getId(),
                n.getValeur(),
                n.getExamen().getId(),
                n.getExamen().getMatiere().getId(),
                n.getExamen().getMatiere().getLibelle(),
                n.getEtudiant().getId(),
                n.getEtudiant().getNumInscription(),
                n.getEtudiant().getNom(),
                n.getEtudiant().getPrenom(),
                n.getEnseignant().getId(),
                n.getDateSaisie(),
                n.getDateModification());
    }
}
