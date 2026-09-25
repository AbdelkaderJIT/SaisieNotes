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
public class Enseignant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    // "enseigne" : matières affectées par l'administration (section 2.3)
    @ManyToMany
    @JoinTable(name = "enseigne",
            joinColumns = @JoinColumn(name = "enseignant_id"),
            inverseJoinColumns = @JoinColumn(name = "matiere_id"))
    private Set<Matiere> matieres = new HashSet<>();

    public boolean enseigne(Matiere matiere) {
        return matieres.contains(matiere);
    }
}
