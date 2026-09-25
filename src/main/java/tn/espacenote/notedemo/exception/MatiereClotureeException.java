package tn.espacenote.notedemo.exception;

// Après clôture, aucune saisie ni modification -> 409
public class MatiereClotureeException extends NoteException {

    public MatiereClotureeException(String libelleMatiere) {
        super("La matière « " + libelleMatiere + " » est clôturée : saisie et modification interdites");
    }
}
