package tn.espacenote.notedemo.dto;

import tn.espacenote.notedemo.model.Groupe;

public record GroupeResponse(Long id, String nom) {

    public static GroupeResponse from(Groupe g) {
        return new GroupeResponse(g.getId(), g.getNom());
    }
}
