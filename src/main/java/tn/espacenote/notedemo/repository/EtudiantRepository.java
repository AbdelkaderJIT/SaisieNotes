package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Groupe;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByGroupeOrderByNomAscPrenomAsc(Groupe groupe);
}
