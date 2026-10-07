package tn.espacenote.notedemo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// Corps de POST/PUT /api/admin/examens : identifie matière, enseignant et groupes par leur id, pas par
// leur nom, pour rester un CRUD classique (l'administration les découvre via les endpoints de lecture).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamenAdminForm {

    @NotNull
    private Long matiereId;

    @NotNull
    private Long enseignantId;

    @NotEmpty
    private String session;

    @NotNull
    private LocalDate date;

    @NotEmpty
    private List<Long> groupeIds;
}
