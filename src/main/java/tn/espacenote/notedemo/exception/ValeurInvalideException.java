package tn.espacenote.notedemo.exception;

// {0 <= valeur <= 20}, revérifié côté service -> 422
public class ValeurInvalideException extends NoteException {

    public ValeurInvalideException() {
        super("La note doit être comprise entre 0 et 20");
    }
}
