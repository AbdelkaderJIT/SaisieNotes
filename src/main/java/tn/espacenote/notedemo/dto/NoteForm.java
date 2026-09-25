package tn.espacenote.notedemo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Objet lié au formulaire : seuls les champs saisis par l'enseignant.
// La matière vient de l'URL, l'enseignant de la session, les dates du service.
@Getter
@Setter
@NoArgsConstructor
public class NoteForm {

    @NotNull
    private Long etudiantId;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("20.00")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal valeur;
}