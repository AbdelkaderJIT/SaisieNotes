package tn.espacenote.notedemo.dto;

import tn.espacenote.notedemo.model.Matiere;

public record MatiereResponse(Long id, String code, String libelle, int semestre, boolean cloturee) {

    public static MatiereResponse from(Matiere m) {
        return new MatiereResponse(m.getId(), m.getCode(), m.getLibelle(), m.getSemestre(), m.isCloturee());
    }
}
