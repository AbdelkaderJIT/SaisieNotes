package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Examen;
import tn.espacenote.notedemo.model.Groupe;
import tn.espacenote.notedemo.model.Note;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    boolean existsByEtudiantAndExamen(Etudiant etudiant, Examen examen);

    // Les notes d'un examen, pour un groupe donné. join fetch : charge l'étudiant avec la note
    // (évite une requête supplémentaire par ligne).
    @Query("""
            select n from Note n join fetch n.etudiant
            where n.examen = :examen and n.etudiant.groupe = :groupe
            """)
    List<Note> findVisiblesPar(@Param("examen") Examen examen, @Param("groupe") Groupe groupe);
}
