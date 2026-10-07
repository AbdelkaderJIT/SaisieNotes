package tn.espacenote.notedemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.espacenote.notedemo.dto.EnseignantResponse;
import tn.espacenote.notedemo.dto.EtudiantResponse;
import tn.espacenote.notedemo.dto.ExamenAdminForm;
import tn.espacenote.notedemo.dto.ExamenResponse;
import tn.espacenote.notedemo.dto.GroupeResponse;
import tn.espacenote.notedemo.dto.MatiereResponse;
import tn.espacenote.notedemo.service.ExamenAdminService;

import java.util.List;

// API d'administration : CRUD des examens, et listes de référence pour trouver les ids à utiliser.
// Réservée au rôle ADMIN (voir SecurityConfig) — distincte de /api, réservée aux enseignants.
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Gestion des examens et consultation des listes de référence")
public class ExamenAdminController {

    private final ExamenAdminService service;

    @GetMapping("/enseignants")
    @Operation(summary = "Liste des enseignants (pour trouver un enseignantId)")
    public List<EnseignantResponse> enseignants() {
        return service.enseignants();
    }

    @GetMapping("/etudiants")
    @Operation(summary = "Liste des étudiants, avec leur groupe")
    public List<EtudiantResponse> etudiants() {
        return service.etudiants();
    }

    @GetMapping("/matieres")
    @Operation(summary = "Liste des matières (pour trouver un matiereId)")
    public List<MatiereResponse> matieres() {
        return service.matieres();
    }

    @GetMapping("/groupes")
    @Operation(summary = "Liste des groupes (classes), pour trouver un groupeId")
    public List<GroupeResponse> groupes() {
        return service.groupes();
    }

    @GetMapping("/examens")
    @Operation(summary = "Liste de tous les examens, tous enseignants confondus")
    public List<ExamenResponse> examens() {
        return service.lister();
    }

    @GetMapping("/examens/{id}")
    @Operation(summary = "Détail d'un examen")
    public ExamenResponse examen(@PathVariable Long id) {
        return service.obtenir(id);
    }

    @PostMapping("/examens")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un examen (matière, enseignant, session, date, groupes concernés)")
    public ExamenResponse creer(@Valid @RequestBody ExamenAdminForm form) {
        return service.creer(form);
    }

    @PutMapping("/examens/{id}")
    @Operation(summary = "Modifier un examen existant")
    public ExamenResponse modifier(@PathVariable Long id, @Valid @RequestBody ExamenAdminForm form) {
        return service.modifier(id, form);
    }

    @DeleteMapping("/examens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un examen (refusé s'il a déjà des notes)")
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
