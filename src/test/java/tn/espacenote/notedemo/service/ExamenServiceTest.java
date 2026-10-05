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
import tn.espacenote.notedemo.model.Examen;
import tn.espacenote.notedemo.model.Groupe;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.model.Note;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.ExamenRepository;
import tn.espacenote.notedemo.repository.GroupeRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Test unitaire des règles métier : repositories simulés (Mockito), aucune base de données.
@ExtendWith(MockitoExtension.class)
class ExamenServiceTest {

    private static final Long ALI_ID = 1L;
    private static final Long SONIA_ID = 2L;
    private static final Long EXAMEN_ID = 10L;
    private static final Long ETUDIANT_ID = 20L;
    private static final Long NOTE_ID = 30L;
    private static final Long GROUPE_A_ID = 40L;
    private static final Long GROUPE_B_ID = 41L;
    private static final Long GROUPE_INCONNU_ID = 99L;

    @Mock EnseignantRepository enseignantRepository;
    @Mock GroupeRepository groupeRepository;
    @Mock EtudiantRepository etudiantRepository;
    @Mock ExamenRepository examenRepository;
    @Mock NoteRepository noteRepository;
    @InjectMocks ExamenService service;

    private Enseignant ali;      // propriétaire de l'examen
    private Enseignant sonia;    // n'a pas cet examen
    private Matiere algo;
    private Groupe groupeA;
    private Groupe groupeB;
    private Examen examen;       // couvre A ET B : un examen peut viser plusieurs groupes à la fois
    private Etudiant amine;      // dans le groupe A

    @BeforeEach
    void fixtures() {
        algo = new Matiere();
        algo.setLibelle("Algorithmique");

        ali = new Enseignant();
        ali.setId(ALI_ID);
        sonia = new Enseignant();
        sonia.setId(SONIA_ID);

        groupeA = new Groupe();
        groupeA.setId(GROUPE_A_ID);
        groupeA.setNom("L2-A");
        groupeB = new Groupe();
        groupeB.setId(GROUPE_B_ID);
        groupeB.setNom("L2-B");

        examen = new Examen(algo, ali, "DS1", LocalDate.of(2025, 11, 10));
        examen.getGroupes().add(groupeA);
        examen.getGroupes().add(groupeB);

        amine = new Etudiant();
        amine.setNom("Gharbi");
        amine.setPrenom("Amine");
        amine.setGroupe(groupeA);

        lenient().when(enseignantRepository.findById(ALI_ID)).thenReturn(Optional.of(ali));
        lenient().when(enseignantRepository.findById(SONIA_ID)).thenReturn(Optional.of(sonia));
        lenient().when(examenRepository.findById(EXAMEN_ID)).thenReturn(Optional.of(examen));
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
        assertThrows(ValeurInvalideException.class,
                () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("20.01")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_noteNegative_estRejetee() {
        assertThrows(ValeurInvalideException.class,
                () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("-0.5")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_noteAbsente_estRejetee() {
        assertThrows(ValeurInvalideException.class, () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form(null)));
    }

    @Test
    void saisir_bornes0Et20_sontAcceptees() {
        assertEquals(0, service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("0")).valeur().compareTo(BigDecimal.ZERO));
        assertEquals(0, service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("20")).valeur()
                .compareTo(new BigDecimal("20")));
    }

    // ---------- saisie : autres règles ----------

    @Test
    void saisir_enseignantNonProprietaire_estRefuse() {
        assertThrows(ExamenAccesRefuseException.class,
                () -> service.saisir(SONIA_ID, EXAMEN_ID, GROUPE_A_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_examenCloture_estRefusee() {
        examen.cloturer();
        assertThrows(ExamenClotureException.class, () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_groupeHorsExamen_donne404() {
        assertThrows(RessourceIntrouvableException.class,
                () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_INCONNU_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    // L'examen couvre A et B, mais Amine est dans le groupe A : on ne peut pas le noter via le groupe B.
    @Test
    void saisir_etudiantHorsDuGroupeChoisi_estRefuse() {
        assertThrows(EtudiantHorsGroupeException.class,
                () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_B_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_noteDejaExistante_estRefusee() {
        when(noteRepository.existsByEtudiantAndExamen(amine, examen)).thenReturn(true);
        assertThrows(NoteDejaExistanteException.class,
                () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("12")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void saisir_etudiantInconnu_donne404() {
        when(etudiantRepository.findById(ETUDIANT_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class,
                () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("12")));
    }

    @Test
    void saisir_ok_enregistreAvecEchelle2EtTracabilite() {
        NoteResponse r = service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("12.5"));

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        Note saved = captor.getValue();
        assertEquals(new BigDecimal("12.50"), saved.getValeur());
        assertSame(ali, saved.getEnseignant());          // qui a saisi
        assertSame(amine, saved.getEtudiant());
        assertSame(examen, saved.getExamen());
        assertNotNull(saved.getDateSaisie());
        assertNull(saved.getDateModification());
        assertEquals(new BigDecimal("12.50"), r.valeur());
    }

    // ---------- modification ----------

    private Note noteExistante() {
        Note n = new Note(new BigDecimal("10.00"), amine, examen, ali);
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
        examen.cloturer();
        assertThrows(ExamenClotureException.class, () -> service.modifier(ALI_ID, NOTE_ID, new BigDecimal("15")));
        assertEquals(new BigDecimal("10.00"), n.getValeur());
        assertNull(n.getDateModification());
    }

    @Test
    void modifier_enseignantNonProprietaire_estRefuse() {
        Note n = noteExistante();
        assertThrows(ExamenAccesRefuseException.class, () -> service.modifier(SONIA_ID, NOTE_ID, new BigDecimal("15")));
        assertEquals(new BigDecimal("10.00"), n.getValeur());
    }

    @Test
    void modifier_noteSuperieureA20_estRejetee() {
        noteExistante();
        assertThrows(ValeurInvalideException.class, () -> service.modifier(ALI_ID, NOTE_ID, new BigDecimal("21")));
    }

    @Test
    void modifier_noteInconnue_donne404() {
        when(noteRepository.findById(NOTE_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.modifier(ALI_ID, NOTE_ID, new BigDecimal("15")));
    }

    // ---------- consultation, groupes, clôture, identité ----------

    @Test
    void examenDe_nonProprietaire_estRefuse() {
        assertThrows(ExamenAccesRefuseException.class, () -> service.examenDe(SONIA_ID, EXAMEN_ID));
    }

    @Test
    void examenDe_proprietaire_renvoieLExamenAvecSesGroupes() {
        var reponse = service.examenDe(ALI_ID, EXAMEN_ID);
        assertEquals("Algorithmique", reponse.matiereLibelle());
        assertEquals(2, reponse.groupes().size());
    }

    @Test
    void groupesDe_nonProprietaire_estRefuse() {
        assertThrows(ExamenAccesRefuseException.class, () -> service.groupesDe(SONIA_ID, EXAMEN_ID));
    }

    @Test
    void etudiantsSansNote_exclutLesEtudiantsDejaNotes() {
        Etudiant nour = new Etudiant();
        nour.setNom("Mansour");
        nour.setPrenom("Nour");
        nour.setGroupe(groupeA);

        when(etudiantRepository.findByGroupeOrderByNomAscPrenomAsc(groupeA)).thenReturn(List.of(amine, nour));
        when(noteRepository.existsByEtudiantAndExamen(amine, examen)).thenReturn(true);
        when(noteRepository.existsByEtudiantAndExamen(nour, examen)).thenReturn(false);

        var etudiants = service.etudiantsSansNote(ALI_ID, EXAMEN_ID, GROUPE_A_ID);

        assertEquals(1, etudiants.size());
        assertEquals("Nour", etudiants.get(0).prenom());
    }

    @Test
    void notesDe_nonProprietaire_estRefuse() {
        assertThrows(ExamenAccesRefuseException.class, () -> service.notesDe(SONIA_ID, EXAMEN_ID, GROUPE_A_ID));
        verify(noteRepository, never()).findVisiblesPar(any(), any());
    }

    @Test
    void notesDe_renvoieLesNotesDuGroupe() {
        Note note = new Note(new BigDecimal("14.50"), amine, examen, ali);
        when(noteRepository.findVisiblesPar(examen, groupeA)).thenReturn(List.of(note));

        var notes = service.notesDe(ALI_ID, EXAMEN_ID, GROUPE_A_ID);

        assertEquals(1, notes.size());
        assertEquals(new BigDecimal("14.50"), notes.get(0).valeur());
    }

    @Test
    void matieresDe_renvoieLesMatieresDeLEnseignant() {
        ali.getMatieres().add(algo);
        var matieres = service.matieresDe(ALI_ID);
        assertEquals(1, matieres.size());
        assertEquals("Algorithmique", matieres.get(0).libelle());
    }

    @Test
    void cloturer_bloqueEnsuiteLaSaisie() {
        assertTrue(service.cloturer(ALI_ID, EXAMEN_ID).cloturee());
        assertThrows(ExamenClotureException.class, () -> service.saisir(ALI_ID, EXAMEN_ID, GROUPE_A_ID, form("12")));
    }

    @Test
    void cloturer_nonProprietaire_estRefuse() {
        assertThrows(ExamenAccesRefuseException.class, () -> service.cloturer(SONIA_ID, EXAMEN_ID));
        assertFalse(examen.isCloturee());
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
