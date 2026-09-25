package tn.espacenote.notedemo.exception;

// -> 422
public class EtudiantNonInscritException extends NoteException {

    public EtudiantNonInscritException(String nomComplet, String libelleMatiere) {
        super("L'étudiant " + nomComplet + " n'est pas inscrit à la matière « " + libelleMatiere + " »");
    }
}
