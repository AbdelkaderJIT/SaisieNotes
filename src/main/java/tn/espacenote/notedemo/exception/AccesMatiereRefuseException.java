package tn.espacenote.notedemo.exception;

// Section 2.3 : un enseignant n'accède qu'aux matières qui lui sont affectées -> 403
public class AccesMatiereRefuseException extends NoteException {

    public AccesMatiereRefuseException(String libelleMatiere) {
        super("Vous n'êtes pas affecté à la matière « " + libelleMatiere + " »");
    }
}
