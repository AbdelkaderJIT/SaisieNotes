package tn.espacenote.notedemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.espacenote.notedemo.dto.EnseignantResponse;
import tn.espacenote.notedemo.dto.EtudiantResponse;
import tn.espacenote.notedemo.dto.MatiereResponse;
import tn.espacenote.notedemo.dto.NoteForm;
import tn.espacenote.notedemo.dto.NoteResponse;
import tn.espacenote.notedemo.exception.*;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.model.Note;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.MatiereRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Toutes les règles métier vivent ici. Aucune dépendance web : le service reçoit des
// identifiants, renvoie des DTO et signale les violations par des NoteException.
@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private static final BigDecimal MAX = new BigDecimal("20");

    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final EtudiantRepository etudiantRepository;
    private final NoteRepository noteRepository;

    // Relie l'utilisateur authentifié (son email) à l'Enseignant en base
    @Transactional(readOnly = true)
    public Long idEnseignantPar(String email) {
        return enseignantRepository.findByEmail(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant", email))
                .getId();
    }

    // Profil de l'utilisateur connecté (le front l'affiche et s'en sert pour valider le login)
    @Transactional(readOnly = true)
    public EnseignantResponse profil(String email) {
        return enseignantRepository.findByEmail(email)
                .map(EnseignantResponse::from)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant", email));
    }

    @Transactional(readOnly = true)
    public List<MatiereResponse> matieresDe(Long enseignantId) {
        Enseignant enseignant = enseignant(enseignantId);
        return matiereRepository.findByEnseignantsContaining(enseignant).stream()
                .map(MatiereResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MatiereResponse matiereDe(Long enseignantId, Long matiereId) {
        Matiere matiere = matiere(matiereId);
        verifierAffectation(enseignant(enseignantId), matiere);
        return MatiereResponse.from(matiere);
    }

    // Étudiants inscrits à la matière (alimente la liste déroulante du formulaire de saisie)
    @Transactional(readOnly = true)
    public List<EtudiantResponse> etudiantsDe(Long enseignantId, Long matiereId) {
        Matiere matiere = matiere(matiereId);
        verifierAffectation(enseignant(enseignantId), matiere);
        return etudiantRepository.findByMatieresContainingOrderByNomAscPrenomAsc(matiere).stream()
                .map(EtudiantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> notesDe(Long enseignantId, Long matiereId) {
        Matiere matiere = matiere(matiereId);
        verifierAffectation(enseignant(enseignantId), matiere);
        return noteRepository.findByMatiere(matiere).stream()
                .map(NoteResponse::from)
                .toList();
    }

    public NoteResponse saisir(Long enseignantId, Long matiereId, NoteForm form) {
        Enseignant enseignant = enseignant(enseignantId);
        Matiere matiere = matiere(matiereId);

        verifierAffectation(enseignant, matiere);          // 1. enseignant affecté
        verifierNonCloturee(matiere);                      // 2. matière non clôturée

        Etudiant etudiant = etudiantRepository.findById(form.getEtudiantId())
                .orElseThrow(() -> new RessourceIntrouvableException("Étudiant", form.getEtudiantId()));
        if (!etudiant.estInscritA(matiere)) {              // 3. étudiant inscrit
            throw new EtudiantNonInscritException(nomComplet(etudiant), matiere.getLibelle());
        }

        verifierValeur(form.getValeur());                  // 4. 0 <= valeur <= 20

        if (noteRepository.existsByEtudiantAndMatiere(etudiant, matiere)) {   // 5. unicité
            throw new NoteDejaExistanteException(nomComplet(etudiant), matiere.getLibelle());
        }

        Note note = new Note(normaliser(form.getValeur()), etudiant, matiere, enseignant);
        return NoteResponse.from(noteRepository.save(note));
    }

    public NoteResponse modifier(Long enseignantId, Long noteId, BigDecimal valeur) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Note", noteId));
        Matiere matiere = note.getMatiere();

        verifierAffectation(enseignant(enseignantId), matiere);
        verifierNonCloturee(matiere);
        verifierValeur(valeur);

        note.modifier(normaliser(valeur));   // met à jour dateModification ; flush au commit
        return NoteResponse.from(note);
    }

    // Clôture définitive : plus aucune saisie ni modification ensuite. Idempotent si déjà clôturée.
    public MatiereResponse cloturer(Long enseignantId, Long matiereId) {
        Matiere matiere = matiere(matiereId);
        verifierAffectation(enseignant(enseignantId), matiere);
        matiere.cloturer();
        return MatiereResponse.from(matiere);
    }

    // ---- règles ----

    private void verifierAffectation(Enseignant enseignant, Matiere matiere) {
        if (!enseignant.enseigne(matiere)) {
            throw new AccesMatiereRefuseException(matiere.getLibelle());
        }
    }

    private void verifierNonCloturee(Matiere matiere) {
        if (matiere.isCloturee()) {
            throw new MatiereClotureeException(matiere.getLibelle());
        }
    }

    private void verifierValeur(BigDecimal valeur) {
        if (valeur == null || valeur.signum() < 0 || valeur.compareTo(MAX) > 0) {
            throw new ValeurInvalideException();
        }
    }

    // ---- utilitaires ----

    private BigDecimal normaliser(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }

    private String nomComplet(Etudiant e) {
        return e.getPrenom() + " " + e.getNom();
    }

    private Enseignant enseignant(Long id) {
        return enseignantRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant", id));
    }

    private Matiere matiere(Long id) {
        return matiereRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Matière", id));
    }
}
