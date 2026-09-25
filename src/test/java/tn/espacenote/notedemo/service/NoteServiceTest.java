package tn.espacenote.notedemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Test unitaire des règles métier : repositories simulés (Mockito), aucune base de données.
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final Long ALI_ID = 1L;
    private static final Long SONIA_ID = 2L;
    private static final Long MATIERE_ID = 10L;
    private static final Long ETUDIANT_ID = 20L;
    private static final Long NOTE_ID = 30L;

    @Mock EnseignantRepository enseignantRepository;
    @Mock MatiereRepository matiereRepository;
    @Mock EtudiantRepository etudiantRepository;
    @Mock NoteRepository noteRepository;
    @InjectMocks NoteService service;

    private Enseignant ali;      // affecté à la matière
    private Enseignant sonia;    // non affectée
    private Matiere algo;
    private Etudiant amine;      // inscrit à la matière

    @BeforeEach
    void fixtures() {
        algo = new Matiere();
        algo.setLibelle("Algorithmique");

        ali = new Enseignant();
        ali.getMatieres().add(algo);
        sonia = new Enseignant();

        amine = new Etudiant();
        amine.setNom("Gharbi");
        amine.setPrenom("Amine");
        amine.getMatieres().add(algo);

        lenient().when(enseignantRepository.findById(ALI_ID)).thenReturn(Optional.of(ali));
        lenient().when(enseignantRepository.findById(SONIA_ID)).thenReturn(Optional.of(sonia));
        lenient().when(matiereRepository.findById(MATIERE_ID)).thenReturn(Optional.of(algo));
        lenient().when(etudiantRepository.findById(ETUDIANT_ID)).thenReturn(Optional.of(amine));
        lenient().when(noteRepository.save(any(Note.class))).thenAnswer(i -> i.getArgument(0));
    }

    private NoteForm form(String valeur) {
        NoteForm f = new NoteForm();
        f.setEtudiantId(ETUDIANT_ID);
        f.setValeur(valeur == null ? null : new BigDecimal(valeur));
        return f;
    }

    // ---------- saisie : {0 <= valeur <= 20} ----------

    @Test
    void saisir_noteSuperieureA20_estRejetee() {
        assertThrows(ValeurInvalideException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form("20.01")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_noteNegative_estRejetee() {
        assertThrows(ValeurInvalideException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form("-0.5")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_noteAbsente_estRejetee() {
        assertThrows(ValeurInvalideException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form(null)));
    }

    @Test
    void saisir_bornes0Et20_sontAcceptees() {
        assertEquals(0, service.saisir(ALI_ID, MATIERE_ID, form("0")).valeur().compareTo(BigDecimal.ZERO));
        assertEquals(0, service.saisir(ALI_ID, MATIERE_ID, form("20")).valeur().compareTo(new BigDecimal("20")));
    }

    // ---------- saisie : autres règles ----------

    @Test
    void saisir_enseignantNonAffecte_estRefuse() {
        assertThrows(AccesMatiereRefuseException.class, () -> service.saisir(SONIA_ID, MATIERE_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_matiereCloturee_estRefusee() {
        algo.cloturer();
        assertThrows(MatiereClotureeException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_etudiantNonInscrit_estRefuse() {
        amine.getMatieres().clear();
        assertThrows(EtudiantNonInscritException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_noteDejaExistante_estRefusee() {
        when(noteRepository.existsByEtudiantAndMatiere(amine, algo)).thenReturn(true);
        assertThrows(NoteDejaExistanteException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_etudiantInconnu_donne404() {
        when(etudiantRepository.findById(ETUDIANT_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form("12")));
    }

    @Test
    void saisir_ok_enregistreAvecEchelle2EtTracabilite() {
        NoteResponse r = service.saisir(ALI_ID, MATIERE_ID, form("12.5"));

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        Note saved = captor.getValue();
        assertEquals(new BigDecimal("12.50"), saved.getValeur());
        assertSame(ali, saved.getEnseignant());          // qui a saisi
        assertSame(amine, saved.getEtudiant());
        assertSame(algo, saved.getMatiere());
        assertNotNull(saved.getDateSaisie());
        assertNull(saved.getDateModification());
        assertEquals(new BigDecimal("12.50"), r.valeur());
    }

    // ---------- modification ----------

    private Note noteExistante() {
        Note n = new Note(new BigDecimal("10.00"), amine, algo, ali);
        when(noteRepository.findById(NOTE_ID)).thenReturn(Optional.of(n));
        return n;
    }

    @Test
    void modifier_ok_metAJourLaValeurEtDateModification() {
        Note n = noteExistante();
        service.modifier(ALI_ID, NOTE_ID, new BigDecimal("15.25"));
        assertEquals(new BigDecimal("15.25"), n.getValeur());
        assertNotNull(n.getDateModification());
    }

    @Test
    void modifier_apresCloture_estRefuse() {
        Note n = noteExistante();
        algo.cloturer();
        assertThrows(MatiereClotureeException.class, () -> service.modifier(ALI_ID, NOTE_ID, new BigDecimal("15")));
        assertEquals(new BigDecimal("10.00"), n.getValeur());
        assertNull(n.getDateModification());
    }

    @Test
    void modifier_enseignantNonAffecte_estRefuse() {
        Note n = noteExistante();
        assertThrows(AccesMatiereRefuseException.class, () -> service.modifier(SONIA_ID, NOTE_ID, new BigDecimal("15")));
        assertEquals(new BigDecimal("10.00"), n.getValeur());
    }

    @Test
    void modifier_noteSuperieureA20_estRejetee() {
        Note n = noteExistante();
        assertThrows(ValeurInvalideException.class, () -> service.modifier(ALI_ID, NOTE_ID, new BigDecimal("21")));
        assertEquals(new BigDecimal("10.00"), n.getValeur());
    }

    @Test
    void modifier_noteInconnue_donne404() {
        when(noteRepository.findById(NOTE_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.modifier(ALI_ID, NOTE_ID, new BigDecimal("15")));
    }

    // ---------- consultation, clôture, identité ----------

    @Test
    void notesDe_enseignantNonAffecte_estRefuse() {
        assertThrows(AccesMatiereRefuseException.class, () -> service.notesDe(SONIA_ID, MATIERE_ID));
        verify(noteRepository, never()).findByMatiere(any());
    }

    @Test
    void cloturer_bloqueEnsuiteLaSaisie() {
        assertTrue(service.cloturer(ALI_ID, MATIERE_ID).cloturee());
        assertThrows(MatiereClotureeException.class, () -> service.saisir(ALI_ID, MATIERE_ID, form("12")));
    }

    @Test
    void cloturer_enseignantNonAffecte_estRefuse() {
        assertThrows(AccesMatiereRefuseException.class, () -> service.cloturer(SONIA_ID, MATIERE_ID));
        assertFalse(algo.isCloturee());
    }

    @Test
    void profil_renvoieLeNomDeLEnseignantConnecte() {
        ali.setNom("Ben Ali");
        ali.setPrenom("Ali");
        ali.setEmail("ali.benali@fds.tn");
        when(enseignantRepository.findByEmail("ali.benali@fds.tn")).thenReturn(Optional.of(ali));

        var profil = service.profil("ali.benali@fds.tn");

        assertEquals("Ali", profil.prenom());
        assertEquals("Ben Ali", profil.nom());
    }

    @Test
    void idEnseignantPar_emailInconnu_donne404() {
        when(enseignantRepository.findByEmail("inconnu@fds.tn")).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.idEnseignantPar("inconnu@fds.tn"));
    }
}
