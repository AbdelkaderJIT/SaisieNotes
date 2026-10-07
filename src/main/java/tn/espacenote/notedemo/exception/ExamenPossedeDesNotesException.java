package tn.espacenote.notedemo.exception;

// Suppression refusée : l'examen a déjà des notes, les supprimer à l'aveugle perdrait des données -> 409
public class ExamenPossedeDesNotesException extends NoteException {

    public ExamenPossedeDesNotesException(Long examenId) {
        super("L'examen " + examenId + " a déjà des notes : impossible de le supprimer");
    }
}
