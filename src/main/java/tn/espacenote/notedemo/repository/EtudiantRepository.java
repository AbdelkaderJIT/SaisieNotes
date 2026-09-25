package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Matiere;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByMatieresContainingOrderByNomAscPrenomAsc(Matiere matiere);
}
