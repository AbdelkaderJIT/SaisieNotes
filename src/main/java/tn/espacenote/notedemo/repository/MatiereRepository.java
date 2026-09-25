package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Matiere;

import java.util.List;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {

    // Un enseignant ne voit que les matières qui lui sont affectées
    List<Matiere> findByEnseignantsContaining(Enseignant enseignant);
}
