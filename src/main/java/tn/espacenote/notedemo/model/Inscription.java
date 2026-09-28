package tn.espacenote.notedemo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Inscription d'un étudiant à une matière, avec l'enseignant qui l'a en charge.
// Deux enseignants peuvent enseigner la même matière, mais à des étudiants différents :
// un étudiant n'a qu'UN enseignant par matière, d'où l'unicité (etudiant, matiere).
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_inscription_etudiant_matiere",
        columnNames = {"etudiant_id", "matiere_id"}))
@Getter
@NoArgsConstructor
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id")
    private Etudiant etudiant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    public Inscription(Etudiant etudiant, Matiere matiere, Enseignant enseignant) {
        // On ne peut confier un étudiant qu'à un enseignant qui enseigne cette matière
        if (!enseignant.enseigne(matiere)) {
            throw new IllegalArgumentException(
                    "L'enseignant n'enseigne pas la matière « " + matiere.getLibelle() + " »");
        }
        this.etudiant = etudiant;
        this.matiere = matiere;
        this.enseignant = enseignant;
    }
}
