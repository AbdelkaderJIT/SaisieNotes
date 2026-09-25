package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.espacenote.notedemo.model.Enseignant;

import java.util.Optional;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {

    Optional<Enseignant> findByEmail(String email);
}
