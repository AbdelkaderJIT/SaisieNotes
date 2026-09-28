package tn.espacenote.notedemo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numInscription;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String filiere;

    private String niveau;

    // Les matières de l'étudiant (et leur enseignant) sont portées par Inscription.
}
