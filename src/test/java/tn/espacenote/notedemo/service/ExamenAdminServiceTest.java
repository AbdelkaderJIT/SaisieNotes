package tn.espacenote.notedemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.espacenote.notedemo.dto.ExamenAdminForm;
import tn.espacenote.notedemo.exception.ExamenPossedeDesNotesException;
import tn.espacenote.notedemo.exception.RessourceIntrouvableException;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Examen;
import tn.espacenote.notedemo.model.Groupe;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.ExamenRepository;
import tn.espacenote.notedemo.repository.GroupeRepository;
import tn.espacenote.notedemo.repository.MatiereRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Test unitaire du CRUD admin : repositories simulés (Mockito), aucune base de données.
@ExtendWith(MockitoExtension.class)
class ExamenAdminServiceTest {

    private static final Long MATIERE_ID = 1L;
    private static final Long ENSEIGNANT_ID = 2L;
    private static final Long GROUPE_ID = 3L;
    private static final Long EXAMEN_ID = 10L;

    @Mock EnseignantRepository enseignantRepository;
    @Mock EtudiantRepository etudiantRepository;
    @Mock MatiereRepository matiereRepository;
    @Mock GroupeRepository groupeRepository;
    @Mock ExamenRepository examenRepository;
    @Mock NoteRepository noteRepository;
    @InjectMocks ExamenAdminService service;

    private Matiere algo;
    private Enseignant ali;
    private Groupe l2a;

    @BeforeEach
    void fixtures() {
        algo = new Matiere();
        algo.setLibelle("Algorithmique");
        algo.setCode("INF101");

        ali = new Enseignant();
        ali.setId(ENSEIGNANT_ID);

        l2a = new Groupe();
        l2a.setId(GROUPE_ID);
        l2a.setNom("L2-A");

        lenient().when(matiereRepository.findById(MATIERE_ID)).thenReturn(Optional.of(algo));
        lenient().when(enseignantRepository.findById(ENSEIGNANT_ID)).thenReturn(Optional.of(ali));
        lenient().when(groupeRepository.findById(GROUPE_ID)).thenReturn(Optional.of(l2a));
        lenient().when(examenRepository.save(any(Examen.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ExamenAdminForm form() {
        return new ExamenAdminForm(MATIERE_ID, ENSEIGNANT_ID, "DS1", LocalDate.of(2026, 1, 15), List.of(GROUPE_ID));
    }

    // ---------- création ----------

    @Test
    void creer_ok_construitLExamenAvecSesGroupes() {
        var reponse = service.creer(form());

        ArgumentCaptor<Examen> captor = ArgumentCaptor.forClass(Examen.class);
        verify(examenRepository).save(captor.capture());
        Examen saved = captor.getValue();
        assertSame(algo, saved.getMatiere());
        assertSame(ali, saved.getEnseignant());
        assertEquals("DS1", saved.getSession());
        assertEquals(1, saved.getGroupes().size());
        assertFalse(saved.isCloturee());
        assertEquals("Algorithmique", reponse.matiereLibelle());
    }

    @Test
    void creer_matiereInconnue_donne404() {
        when(matiereRepository.findById(MATIERE_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.creer(form()));
        verify(examenRepository, never()).save(any());
    }

    @Test
    void creer_enseignantInconnu_donne404() {
        when(enseignantRepository.findById(ENSEIGNANT_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.creer(form()));
        verify(examenRepository, never()).save(any());
    }

    @Test
    void creer_groupeInconnu_donne404() {
        when(groupeRepository.findById(GROUPE_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.creer(form()));
        verify(examenRepository, never()).save(any());
    }

    // ---------- modification ----------

    @Test
    void modifier_ok_remplaceLesChamps() {
        Examen existant = new Examen(algo, ali, "DS1", LocalDate.of(2026, 1, 1));
        when(examenRepository.findById(EXAMEN_ID)).thenReturn(Optional.of(existant));

        Matiere autreMatiere = new Matiere();
        autreMatiere.setLibelle("Anglais");
        when(matiereRepository.findById(99L)).thenReturn(Optional.of(autreMatiere));
        var formModifie = new ExamenAdminForm(99L, ENSEIGNANT_ID, "Examen final",
                LocalDate.of(2026, 6, 1), List.of(GROUPE_ID));

        var reponse = service.modifier(EXAMEN_ID, formModifie);

        assertEquals("Anglais", reponse.matiereLibelle());
        assertEquals("Examen final", existant.getSession());
        assertEquals(LocalDate.of(2026, 6, 1), existant.getDate());
        assertEquals(1, existant.getGroupes().size());
    }

    @Test
    void modifier_examenInconnu_donne404() {
        when(examenRepository.findById(EXAMEN_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.modifier(EXAMEN_ID, form()));
    }

    // ---------- suppression ----------

    @Test
    void supprimer_sansNotes_supprime() {
        Examen existant = new Examen(algo, ali, "DS1", LocalDate.of(2026, 1, 1));
        when(examenRepository.findById(EXAMEN_ID)).thenReturn(Optional.of(existant));
        when(noteRepository.existsByExamen(existant)).thenReturn(false);

        service.supprimer(EXAMEN_ID);

        verify(examenRepository).delete(existant);
    }

    @Test
    void supprimer_avecNotes_estRefusee() {
        Examen existant = new Examen(algo, ali, "DS1", LocalDate.of(2026, 1, 1));
        when(examenRepository.findById(EXAMEN_ID)).thenReturn(Optional.of(existant));
        when(noteRepository.existsByExamen(existant)).thenReturn(true);

        assertThrows(ExamenPossedeDesNotesException.class, () -> service.supprimer(EXAMEN_ID));
        verify(examenRepository, never()).delete(any());
    }

    // ---------- consultation ----------

    @Test
    void obtenir_examenInconnu_donne404() {
        when(examenRepository.findById(EXAMEN_ID)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.obtenir(EXAMEN_ID));
    }

    @Test
    void lister_renvoieTousLesExamens() {
        Examen e1 = new Examen(algo, ali, "DS1", LocalDate.of(2026, 1, 1));
        when(examenRepository.findAllAvecGroupes()).thenReturn(List.of(e1));

        assertEquals(1, service.lister().size());
    }

    // ---------- listes de référence ----------

    @Test
    void etudiants_renvoieLeurGroupe() {
        Etudiant amine = new Etudiant();
        amine.setNom("Gharbi");
        amine.setPrenom("Amine");
        amine.setGroupe(l2a);
        when(etudiantRepository.findAll()).thenReturn(List.of(amine));

        var reponse = service.etudiants();

        assertEquals(1, reponse.size());
        assertEquals("L2-A", reponse.get(0).groupeNom());
    }

    @Test
    void enseignants_renvoieLaListe() {
        when(enseignantRepository.findAll()).thenReturn(List.of(ali));
        assertEquals(1, service.enseignants().size());
    }
}
