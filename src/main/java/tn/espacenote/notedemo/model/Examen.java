package tn.espacenote.notedemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

// Un examen : une matière, donnée par un enseignant, à une date, pour une session (DS1, DS2, Examen final).
// Il peut couvrir plusieurs groupes à la fois (le même examen donné à toutes les classes de l'enseignant).
// Créé et modifié par l'administration via l'API /api/admin (ExamenAdminController) ; l'enseignant ne fait
// que le consulter et y saisir des notes, jamais le créer.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @Column(nullable = false)
    private String session;

    @Column(nullable = false)
    private LocalDate date;

    // Décidée dans l'application, jamais par le webservice source : lui ne sait pas où en est la correction.
    private boolean cloturee = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "examen_groupe",
            joinColumns = @JoinColumn(name = "examen_id"),
            inverseJoinColumns = @JoinColumn(name = "groupe_id"))
    private Set<Groupe> groupes = new HashSet<>();

    public Examen(Matiere matiere, Enseignant enseignant, String session, LocalDate date) {
        this.matiere = matiere;
        this.enseignant = enseignant;
        this.session = session;
        this.date = date;
    }

    public void cloturer() {
        this.cloturee = true;
    }
}
