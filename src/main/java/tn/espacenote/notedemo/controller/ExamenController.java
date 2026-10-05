package tn.espacenote.notedemo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.espacenote.notedemo.dto.EnseignantResponse;
import tn.espacenote.notedemo.dto.EtudiantResponse;
import tn.espacenote.notedemo.dto.ExamenResponse;
import tn.espacenote.notedemo.dto.GroupeResponse;
import tn.espacenote.notedemo.dto.MatiereResponse;
import tn.espacenote.notedemo.dto.NoteForm;
import tn.espacenote.notedemo.dto.NoteModificationForm;
import tn.espacenote.notedemo.dto.NoteResponse;
import tn.espacenote.notedemo.service.ExamenService;

import java.util.List;

// Contrôleur fin : reçoit, valide le format (@Valid), délègue au service. Aucune règle métier ici.
// L'enseignant courant est l'utilisateur authentifié (HTTP Basic), jamais une valeur fournie par le client.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExamenController {

    private final ExamenService examenService;

    @GetMapping("/me")
    public EnseignantResponse moi(Authentication auth) {
        return examenService.profil(auth.getName());
    }

    @GetMapping("/matieres")
    public List<MatiereResponse> matieres(Authentication auth) {
        return examenService.matieresDe(enseignantId(auth));
    }

    @GetMapping("/groupes")
    public List<GroupeResponse> groupes() {
        return examenService.groupes();
    }

    @GetMapping("/examens")
    public List<ExamenResponse> examens(Authentication auth,
                                        @RequestParam(required = false) Long matiereId,
                                        @RequestParam(required = false) Long groupeId) {
        return examenService.examensDe(enseignantId(auth), matiereId, groupeId);
    }

    @GetMapping("/examens/{examenId}")
    public ExamenResponse examen(Authentication auth, @PathVariable Long examenId) {
        return examenService.examenDe(enseignantId(auth), examenId);
    }

    @GetMapping("/examens/{examenId}/groupes")
    public List<GroupeResponse> groupesDeLExamen(Authentication auth, @PathVariable Long examenId) {
        return examenService.groupesDe(enseignantId(auth), examenId);
    }

    @GetMapping("/examens/{examenId}/groupes/{groupeId}/etudiants")
    public List<EtudiantResponse> etudiants(Authentication auth,
                                            @PathVariable Long examenId, @PathVariable Long groupeId) {
        return examenService.etudiantsSansNote(enseignantId(auth), examenId, groupeId);
    }

    @GetMapping("/examens/{examenId}/groupes/{groupeId}/notes")
    public List<NoteResponse> notes(Authentication auth,
                                    @PathVariable Long examenId, @PathVariable Long groupeId) {
        return examenService.notesDe(enseignantId(auth), examenId, groupeId);
    }

    @PostMapping("/examens/{examenId}/groupes/{groupeId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse saisir(Authentication auth,
                               @PathVariable Long examenId, @PathVariable Long groupeId,
                               @Valid @RequestBody NoteForm form) {
        return examenService.saisir(enseignantId(auth), examenId, groupeId, form);
    }

    @PostMapping("/examens/{examenId}/cloturer")
    public ExamenResponse cloturer(Authentication auth, @PathVariable Long examenId) {
        return examenService.cloturer(enseignantId(auth), examenId);
    }

    @PutMapping("/notes/{noteId}")
    public NoteResponse modifier(Authentication auth,
                                 @PathVariable Long noteId,
                                 @Valid @RequestBody NoteModificationForm form) {
        return examenService.modifier(enseignantId(auth), noteId, form.getValeur());
    }

    private Long enseignantId(Authentication auth) {
        return examenService.idEnseignantPar(auth.getName());
    }
}
