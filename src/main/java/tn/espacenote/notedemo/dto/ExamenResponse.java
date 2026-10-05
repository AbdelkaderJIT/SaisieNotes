package tn.espacenote.notedemo.dto;

import tn.espacenote.notedemo.model.Examen;

import java.time.LocalDate;
import java.util.List;

public record ExamenResponse(
        Long id,
        Long matiereId,
        String matiereCode,
        String matiereLibelle,
        String session,
        LocalDate date,
        boolean cloturee,
        List<GroupeResponse> groupes) {

    public static ExamenResponse from(Examen e) {
        return new ExamenResponse(
                e.getId(),
                e.getMatiere().getId(),
                e.getMatiere().getCode(),
                e.getMatiere().getLibelle(),
                e.getSession(),
                e.getDate(),
                e.isCloturee(),
                e.getGroupes().stream()
                        .sorted((a, b) -> a.getNom().compareTo(b.getNom()))
                        .map(GroupeResponse::from)
                        .toList());
    }
}
