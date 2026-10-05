package tn.espacenote.notedemo.exception;

// L'examen appartient à un autre enseignant -> 403
public class ExamenAccesRefuseException extends NoteException {

    public ExamenAccesRefuseException(String libelleExamen) {
        super("Cet examen (" + libelleExamen + ") ne vous appartient pas");
    }
}
