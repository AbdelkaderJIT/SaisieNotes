package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.espacenote.notedemo.model.Etudiant;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
}
