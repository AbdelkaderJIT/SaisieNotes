package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.model.Note;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    boolean existsByEtudiantAndMatiere(Etudiant etudiant, Matiere matiere);

    // Les notes d'une matière qu'un enseignant a le droit de voir : celles de SES étudiants.
    // Deux enseignants d'une même matière ne voient donc pas les notes l'un de l'autre.
    // join fetch : charge l'étudiant avec la note (évite une requête supplémentaire par ligne).
    @Query("""
            select n from Note n join fetch n.etudiant
            where n.matiere = :matiere
              and exists (select 1 from Inscription i
                          where i.etudiant = n.etudiant
                            and i.matiere = n.matiere
                            and i.enseignant = :enseignant)
            """)
    List<Note> findVisiblesPar(@Param("matiere") Matiere matiere, @Param("enseignant") Enseignant enseignant);
}
