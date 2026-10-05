package tn.espacenote.notedemo.exception;

// L'étudiant visé n'appartient pas au groupe choisi pour cet examen -> 422
public class EtudiantHorsGroupeException extends NoteException {

    public EtudiantHorsGroupeException(String nomComplet) {
        super("L'étudiant " + nomComplet + " n'appartient pas à ce groupe");
    }
}
