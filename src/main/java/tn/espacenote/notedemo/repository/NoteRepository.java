package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.model.Note;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    boolean existsByEtudiantAndMatiere(Etudiant etudiant, Matiere matiere);

    List<Note> findByMatiere(Matiere matiere);
}
