package tn.espacenote.notedemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Examen;

import java.util.List;

public interface ExamenRepository extends JpaRepository<Examen, Long> {

    // Les examens de CET enseignant, avec leurs groupes chargés en une requête (évite le N+1).
    @Query("select distinct e from Examen e left join fetch e.groupes where e.enseignant = :enseignant "
            + "order by e.date desc, e.id")
    List<Examen> findByEnseignant(@Param("enseignant") Enseignant enseignant);
}
