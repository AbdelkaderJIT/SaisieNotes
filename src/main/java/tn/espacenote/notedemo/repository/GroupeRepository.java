package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.espacenote.notedemo.model.Groupe;

import java.util.Optional;

public interface GroupeRepository extends JpaRepository<Groupe, Long> {

    Optional<Groupe> findByNom(String nom);
}
