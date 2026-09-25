package tn.espacenote.notedemo.exception;

// {unique (etudiant, matiere)} : il faut modifier la note existante -> 409
public class NoteDejaExistanteException extends NoteException {

    public NoteDejaExistanteException(String nomComplet, String libelleMatiere) {
        super("Une note existe déjà pour " + nomComplet + " en « " + libelleMatiere
                + " » : modifiez-la au lieu d'en créer une nouvelle");
    }
}
