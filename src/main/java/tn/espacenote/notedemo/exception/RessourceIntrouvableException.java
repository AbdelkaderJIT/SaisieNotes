package tn.espacenote.notedemo.exception;

// -> 404
public class RessourceIntrouvableException extends NoteException {

    public RessourceIntrouvableException(String ressource, Long id) {
        super(ressource + " introuvable (id = " + id + ")");
    }

    public RessourceIntrouvableException(String ressource, String identifiant) {
        super(ressource + " introuvable (" + identifiant + ")");
    }
}