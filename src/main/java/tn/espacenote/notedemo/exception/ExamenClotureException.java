package tn.espacenote.notedemo.exception;

// Après clôture, aucune saisie ni modification -> 409
public class ExamenClotureException extends NoteException {

    public ExamenClotureException(String libelleExamen) {
        super("L'examen " + libelleExamen + " est clôturé : saisie et modification interdites");
    }
}
