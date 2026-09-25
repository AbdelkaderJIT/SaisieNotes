package tn.espacenote.notedemo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Modification : seule la valeur change (étudiant et matière sont fixés par la note existante).
@Getter
@Setter
@NoArgsConstructor
public class NoteModificationForm {

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("20.00")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal valeur;
}
