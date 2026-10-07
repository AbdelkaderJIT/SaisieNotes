package tn.espacenote.notedemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.espacenote.notedemo.dto.EnseignantResponse;
import tn.espacenote.notedemo.dto.EtudiantResponse;
import tn.espacenote.notedemo.dto.ExamenAdminForm;
import tn.espacenote.notedemo.dto.ExamenResponse;
import tn.espacenote.notedemo.dto.GroupeResponse;
import tn.espacenote.notedemo.dto.MatiereResponse;
import tn.espacenote.notedemo.exception.ExamenPossedeDesNotesException;
import tn.espacenote.notedemo.exception.RessourceIntrouvableException;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Examen;
import tn.espacenote.notedemo.model.Groupe;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.ExamenRepository;
import tn.espacenote.notedemo.repository.GroupeRepository;
import tn.espacenote.notedemo.repository.MatiereRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// CRUD administration des examens, et listes de référence (enseignants, étudiants, matières, groupes)
// pour que l'administration puisse découvrir les ids à utiliser. Aucune règle "mon examen à moi" ici :
// contrairement à ExamenService (côté enseignant), ce service voit et modifie tout.
@Service
@RequiredArgsConstructor
@Transactional
public class ExamenAdminService {

    private final EnseignantRepository enseignantRepository;
    private final EtudiantRepository etudiantRepository;
    private final MatiereRepository matiereRepository;
    private final GroupeRepository groupeRepository;
    private final ExamenRepository examenRepository;
    private final NoteRepository noteRepository;

    // ---- listes de référence ----

    @Transactional(readOnly = true)
    public List<EnseignantResponse> enseignants() {
        return enseignantRepository.findAll().stream().map(EnseignantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EtudiantResponse> etudiants() {
        return etudiantRepository.findAll().stream().map(EtudiantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MatiereResponse> matieres() {
        return matiereRepository.findAll().stream().map(MatiereResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupeResponse> groupes() {
        return groupeRepository.findAll().stream().map(GroupeResponse::from).toList();
    }

    // ---- CRUD examens ----

    @Transactional(readOnly = true)
    public List<ExamenResponse> lister() {
        return examenRepository.findAllAvecGroupes().stream().map(ExamenResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ExamenResponse obtenir(Long id) {
        return ExamenResponse.from(examen(id));
    }

    public ExamenResponse creer(ExamenAdminForm form) {
        Examen examen = new Examen(matiere(form.getMatiereId()), enseignant(form.getEnseignantId()),
                form.getSession(), form.getDate());
        examen.getGroupes().addAll(groupes(form.getGroupeIds()));
        return ExamenResponse.from(examenRepository.save(examen));
    }

    public ExamenResponse modifier(Long id, ExamenAdminForm form) {
        Examen examen = examen(id);
        examen.setMatiere(matiere(form.getMatiereId()));
        examen.setEnseignant(enseignant(form.getEnseignantId()));
        examen.setSession(form.getSession());
        examen.setDate(form.getDate());
        examen.getGroupes().clear();
        examen.getGroupes().addAll(groupes(form.getGroupeIds()));
        return ExamenResponse.from(examen);
    }

    // Refusée si des notes existent déjà : on ne perd pas des notes saisies à cause d'une suppression.
    public void supprimer(Long id) {
        Examen examen = examen(id);
        if (noteRepository.existsByExamen(examen)) {
            throw new ExamenPossedeDesNotesException(id);
        }
        examenRepository.delete(examen);
    }

    // ---- utilitaires ----

    private Enseignant enseignant(Long id) {
        return enseignantRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant", id));
    }

    private Matiere matiere(Long id) {
        return matiereRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Matière", id));
    }

    private Examen examen(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Examen", id));
    }

    private Set<Groupe> groupes(List<Long> ids) {
        Set<Groupe> groupes = new HashSet<>();
        for (Long id : ids) {
            groupes.add(groupeRepository.findById(id)
                    .orElseThrow(() -> new RessourceIntrouvableException("Groupe", id)));
        }
        return groupes;
    }
}
