package tn.espacenote.notedemo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Matiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String libelle;

    private int semestre;

    // Une fois clôturée, aucune note ne peut être ajoutée ni modifiée
    private boolean cloturee = false;

    @ManyToMany(mappedBy = "matieres")
    private Set<Enseignant> enseignants = new HashSet<>();

    @ManyToMany(mappedBy = "matieres")
    private Set<Etudiant> etudiants = new HashSet<>();

    public void cloturer() {
        this.cloturee = true;
    }
}
