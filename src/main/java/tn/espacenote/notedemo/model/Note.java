package tn.espacenote.notedemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_note_etudiant_examen",
        columnNames = {"etudiant_id", "examen_id"}))
@Getter
@NoArgsConstructor
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Notes sur 20 avec décimales : BigDecimal évite les erreurs d'arrondi de double
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal valeur;

    @Column(nullable = false)
    private LocalDateTime dateSaisie;

    private LocalDateTime dateModification;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id")
    private Etudiant etudiant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id")
    private Examen examen;

    // Traçabilité : l'enseignant qui a saisi la note
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    public Note(BigDecimal valeur, Etudiant etudiant, Examen examen, Enseignant enseignant) {
        this.valeur = valeur;
        this.etudiant = etudiant;
        this.examen = examen;
        this.enseignant = enseignant;
        this.dateSaisie = LocalDateTime.now();
    }

    public void modifier(BigDecimal v) {
        this.valeur = v;
        this.dateModification = LocalDateTime.now();
    }
}
