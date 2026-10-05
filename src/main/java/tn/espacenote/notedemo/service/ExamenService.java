package tn.espacenote.notedemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.espacenote.notedemo.dto.EnseignantResponse;
import tn.espacenote.notedemo.dto.EtudiantResponse;
import tn.espacenote.notedemo.dto.ExamenResponse;
import tn.espacenote.notedemo.dto.GroupeResponse;
import tn.espacenote.notedemo.dto.MatiereResponse;
import tn.espacenote.notedemo.dto.NoteForm;
import tn.espacenote.notedemo.dto.NoteResponse;
import tn.espacenote.notedemo.exception.EtudiantHorsGroupeException;
import tn.espacenote.notedemo.exception.ExamenAccesRefuseException;
import tn.espacenote.notedemo.exception.ExamenClotureException;
import tn.espacenote.notedemo.exception.NoteDejaExistanteException;
import tn.espacenote.notedemo.exception.RessourceIntrouvableException;
import tn.espacenote.notedemo.exception.ValeurInvalideException;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Examen;
import tn.espacenote.notedemo.model.Groupe;
import tn.espacenote.notedemo.model.Note;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.ExamenRepository;
import tn.espacenote.notedemo.repository.GroupeRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Toutes les règles métier vivent ici. Aucune dépendance web : le service reçoit des
// identifiants, renvoie des DTO et signale les violations par des NoteException.
@Service
@RequiredArgsConstructor
@Transactional
public class ExamenService {

    private static final BigDecimal MAX = new BigDecimal("20");

    private final EnseignantRepository enseignantRepository;
    private final GroupeRepository groupeRepository;
    private final EtudiantRepository etudiantRepository;
    private final ExamenRepository examenRepository;
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

    // Matières affectées à l'enseignant : alimente le filtre "matière" de la liste d'examens.
    @Transactional(readOnly = true)
    public List<MatiereResponse> matieresDe(Long enseignantId) {
        return enseignant(enseignantId).getMatieres().stream()
                .sorted((a, b) -> a.getCode().compareTo(b.getCode()))
                .map(MatiereResponse::from)
                .toList();
    }

    // Tous les groupes existants : ce ne sont que des noms de classe, pas une donnée sensible.
    @Transactional(readOnly = true)
    public List<GroupeResponse> groupes() {
        return groupeRepository.findAll().stream()
                .sorted((a, b) -> a.getNom().compareTo(b.getNom()))
                .map(GroupeResponse::from)
                .toList();
    }

    // Les examens de CET enseignant, éventuellement filtrés par matière et/ou par groupe.
    @Transactional(readOnly = true)
    public List<ExamenResponse> examensDe(Long enseignantId, Long matiereId, Long groupeId) {
        return examenRepository.findByEnseignant(enseignant(enseignantId)).stream()
                .filter(e -> matiereId == null || e.getMatiere().getId().equals(matiereId))
                .filter(e -> groupeId == null || e.getGroupes().stream().anyMatch(g -> g.getId().equals(groupeId)))
                .map(ExamenResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamenResponse examenDe(Long enseignantId, Long examenId) {
        Examen examen = examen(examenId);
        verifierProprietaire(enseignant(enseignantId), examen);
        return ExamenResponse.from(examen);
    }

    // Les groupes de CET examen : l'écran "choisir un groupe" après avoir cliqué sur l'examen.
    @Transactional(readOnly = true)
    public List<GroupeResponse> groupesDe(Long enseignantId, Long examenId) {
        Examen examen = examen(examenId);
        verifierProprietaire(enseignant(enseignantId), examen);
        return examen.getGroupes().stream()
                .sorted((a, b) -> a.getNom().compareTo(b.getNom()))
                .map(GroupeResponse::from)
                .toList();
    }

    // Les étudiants du groupe qui n'ont pas encore de note pour cet examen (liste déroulante de saisie).
    @Transactional(readOnly = true)
    public List<EtudiantResponse> etudiantsSansNote(Long enseignantId, Long examenId, Long groupeId) {
        Examen examen = examen(examenId);
        Enseignant enseignant = enseignant(enseignantId);
        verifierProprietaire(enseignant, examen);
        Groupe groupe = verifierGroupeDeLExamen(examen, groupeId);
        return etudiantRepository.findByGroupeOrderByNomAscPrenomAsc(groupe).stream()
                .filter(etu -> !noteRepository.existsByEtudiantAndExamen(etu, examen))
                .map(EtudiantResponse::from)
                .toList();
    }

    // Les notes déjà saisies pour ce groupe, dans cet examen.
    @Transactional(readOnly = true)
    public List<NoteResponse> notesDe(Long enseignantId, Long examenId, Long groupeId) {
        Examen examen = examen(examenId);
        Enseignant enseignant = enseignant(enseignantId);
        verifierProprietaire(enseignant, examen);
        Groupe groupe = verifierGroupeDeLExamen(examen, groupeId);
        return noteRepository.findVisiblesPar(examen, groupe).stream()
                .map(NoteResponse::from)
                .toList();
    }

    public NoteResponse saisir(Long enseignantId, Long examenId, Long groupeId, NoteForm form) {
        Enseignant enseignant = enseignant(enseignantId);
        Examen examen = examen(examenId);

        verifierProprietaire(enseignant, examen);                  // 1. l'examen est le sien
        Groupe groupe = verifierGroupeDeLExamen(examen, groupeId);  // 2. le groupe fait partie de cet examen
        verifierNonCloture(examen);                                 // 3. examen non clôturé

        Etudiant etudiant = etudiantRepository.findById(form.getEtudiantId())
                .orElseThrow(() -> new RessourceIntrouvableException("Étudiant", form.getEtudiantId()));
        verifierEtudiantDuGroupe(etudiant, groupe);                 // 4. l'étudiant appartient à CE groupe

        verifierValeur(form.getValeur());                           // 5. 0 <= valeur <= 20

        if (noteRepository.existsByEtudiantAndExamen(etudiant, examen)) {   // 6. unicité
            throw new NoteDejaExistanteException(nomComplet(etudiant), libelle(examen));
        }

        Note note = new Note(normaliser(form.getValeur()), etudiant, examen, enseignant);
        return NoteResponse.from(noteRepository.save(note));
    }

    public NoteResponse modifier(Long enseignantId, Long noteId, BigDecimal valeur) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Note", noteId));
        Examen examen = note.getExamen();
        Enseignant enseignant = enseignant(enseignantId);

        verifierProprietaire(enseignant, examen);
        verifierNonCloture(examen);
        verifierValeur(valeur);

        note.modifier(normaliser(valeur));   // met à jour dateModification ; flush au commit
        return NoteResponse.from(note);
    }

    // Clôture définitive de l'examen (tous ses groupes à la fois) : plus aucune saisie ni modification ensuite.
    public ExamenResponse cloturer(Long enseignantId, Long examenId) {
        Examen examen = examen(examenId);
        verifierProprietaire(enseignant(enseignantId), examen);
        examen.cloturer();
        return ExamenResponse.from(examen);
    }

    // ---- règles ----

    private void verifierProprietaire(Enseignant enseignant, Examen examen) {
        if (!examen.getEnseignant().getId().equals(enseignant.getId())) {
            throw new ExamenAccesRefuseException(libelle(examen));
        }
    }

    // Le groupe demandé doit faire partie de cet examen (sinon, on ne sait pas de qui on parle).
    private Groupe verifierGroupeDeLExamen(Examen examen, Long groupeId) {
        return examen.getGroupes().stream()
                .filter(g -> g.getId().equals(groupeId))
                .findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Groupe dans cet examen", groupeId));
    }

    // Un étudiant ne peut être noté que dans SON groupe : on ne lui prête pas celui d'un autre.
    private void verifierEtudiantDuGroupe(Etudiant etudiant, Groupe groupe) {
        if (!etudiant.getGroupe().getId().equals(groupe.getId())) {
            throw new EtudiantHorsGroupeException(nomComplet(etudiant));
        }
    }

    private void verifierNonCloture(Examen examen) {
        if (examen.isCloturee()) {
            throw new ExamenClotureException(libelle(examen));
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

    private String libelle(Examen e) {
        return e.getMatiere().getLibelle() + " (" + e.getSession() + ")";
    }

    private Enseignant enseignant(Long id) {
        return enseignantRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant", id));
    }

    private Examen examen(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Examen", id));
    }
}
