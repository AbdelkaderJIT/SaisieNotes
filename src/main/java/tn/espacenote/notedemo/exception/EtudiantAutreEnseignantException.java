package tn.espacenote.notedemo.exception;

// L'étudiant est inscrit à la matière, mais suivi par un autre enseignant -> 403
public class EtudiantAutreEnseignantException extends NoteException {

    public EtudiantAutreEnseignantException(String nomComplet, String libelleMatiere) {
        super("L'étudiant " + nomComplet + " est suivi par un autre enseignant pour la matière « "
                + libelleMatiere + " »");
    }
}
