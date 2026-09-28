package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Inscription;
import tn.espacenote.notedemo.model.Matiere;

import java.util.List;
import java.util.Optional;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    // Au plus une inscription par (étudiant, matière) : contrainte unique en base
    Optional<Inscription> findByEtudiantAndMatiere(Etudiant etudiant, Matiere matiere);

    // Les étudiants qu'un enseignant a en charge dans une matière (liste déroulante de saisie)
    @Query("""
            select i.etudiant from Inscription i
            where i.matiere = :matiere and i.enseignant = :enseignant
            order by i.etudiant.nom, i.etudiant.prenom
            """)
    List<Etudiant> etudiantsDe(@Param("matiere") Matiere matiere, @Param("enseignant") Enseignant enseignant);
}
