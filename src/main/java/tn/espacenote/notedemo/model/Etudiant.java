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

    // "inscrit" : permet de refuser une note pour un étudiant non inscrit
    @ManyToMany
    @JoinTable(name = "inscrit",
            joinColumns = @JoinColumn(name = "etudiant_id"),
            inverseJoinColumns = @JoinColumn(name = "matiere_id"))
    private Set<Matiere> matieres = new HashSet<>();

    public boolean estInscritA(Matiere matiere) {
        return matieres.contains(matiere);
    }
}
