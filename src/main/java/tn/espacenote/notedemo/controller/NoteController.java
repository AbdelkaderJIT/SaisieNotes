package tn.espacenote.notedemo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.espacenote.notedemo.dto.EnseignantResponse;
import tn.espacenote.notedemo.dto.MatiereResponse;
import tn.espacenote.notedemo.dto.NoteForm;
import tn.espacenote.notedemo.dto.NoteModificationForm;
import tn.espacenote.notedemo.dto.NoteResponse;
import tn.espacenote.notedemo.service.NoteService;

import java.util.List;

// Contrôleur fin : reçoit, valide le format (@Valid), délègue au service. Aucune règle métier ici.
// L'enseignant courant est l'utilisateur authentifié (HTTP Basic), jamais une valeur fournie par le client.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping("/me")
    public EnseignantResponse moi(Authentication auth) {
        return noteService.profil(auth.getName());
    }

    @GetMapping("/matieres")
    public List<MatiereResponse> matieres(Authentication auth) {
        return noteService.matieresDe(enseignantId(auth));
    }

    @GetMapping("/matieres/{matiereId}/notes")
    public List<NoteResponse> notes(Authentication auth, @PathVariable Long matiereId) {
        return noteService.notesDe(enseignantId(auth), matiereId);
    }

    @PostMapping("/matieres/{matiereId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse saisir(Authentication auth,
                               @PathVariable Long matiereId,
                               @Valid @RequestBody NoteForm form) {
        return noteService.saisir(enseignantId(auth), matiereId, form);
    }

    @PostMapping("/matieres/{matiereId}/cloturer")
    public MatiereResponse cloturer(Authentication auth, @PathVariable Long matiereId) {
        return noteService.cloturer(enseignantId(auth), matiereId);
    }

    @PutMapping("/notes/{noteId}")
    public NoteResponse modifier(Authentication auth,
                                 @PathVariable Long noteId,
                                 @Valid @RequestBody NoteModificationForm form) {
        return noteService.modifier(enseignantId(auth), noteId, form.getValeur());
    }

    private Long enseignantId(Authentication auth) {
        return noteService.idEnseignantPar(auth.getName());
    }
}
