package tn.espacenote.notedemo.exception;

// Base de toutes les violations de règles métier. Le message est destiné à l'utilisateur.
public abstract class NoteException extends RuntimeException {

    protected NoteException(String message) {
        super(message);
    }
}