package tn.espacenote.notedemo.examensource;

import java.time.LocalDate;
import java.util.List;

// Forme du JSON tel qu'un vrai système d'examens (celui de l'université) l'enverrait un jour :
// des clés métier (code de matière, email d'enseignant, noms de groupes), jamais nos ids internes.
public record ExamenSourceDto(
        String matiereCode,
        String enseignantEmail,
        String session,
        LocalDate date,
        List<String> groupeNoms) {
}
