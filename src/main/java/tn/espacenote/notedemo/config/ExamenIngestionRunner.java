package tn.espacenote.notedemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tn.espacenote.notedemo.examensource.ExamenSourceClient;
import tn.espacenote.notedemo.examensource.ExamenSourceDto;
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
import tn.espacenote.notedemo.repository.MatiereRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// Récupère les examens auprès du webservice (ExamenSourceClient) et les enregistre : c'est la SEULE
// façon dont un examen entre en base, jamais un formulaire. Tourne une fois (base vide), après
// DataInitializer (qui doit d'abord avoir créé enseignants, matières et groupes).
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ExamenIngestionRunner implements CommandLineRunner {

    private final ExamenSourceClient examenSourceClient;
    private final MatiereRepository matiereRepository;
    private final EnseignantRepository enseignantRepository;
    private final GroupeRepository groupeRepository;
    private final EtudiantRepository etudiantRepository;
    private final ExamenRepository examenRepository;
    private final NoteRepository noteRepository;

    @Override
    public void run(String... args) {
        if (examenRepository.count() > 0) {
            return;
        }

        List<Examen> examens = new ArrayList<>();
        for (ExamenSourceDto dto : examenSourceClient.recuperer()) {
            Matiere matiere = matiereRepository.findByCode(dto.matiereCode())
                    .orElseThrow(() -> new IllegalStateException("Matière inconnue : " + dto.matiereCode()));
            Enseignant enseignant = enseignantRepository.findByEmail(dto.enseignantEmail())
                    .orElseThrow(() -> new IllegalStateException("Enseignant inconnu : " + dto.enseignantEmail()));
            Examen examen = new Examen(matiere, enseignant, dto.session(), dto.date());
            for (String nom : dto.groupeNoms()) {
                examen.getGroupes().add(groupeRepository.findByNom(nom)
                        .orElseThrow(() -> new IllegalStateException("Groupe inconnu : " + nom)));
            }
            examens.add(examen);
        }

        // Démo : le tout premier examen reçu est déjà clôturé (tous ses étudiants sont notés).
        // Fait AVANT saveAll : saveAll persiste immédiatement (pas de transaction englobante ici),
        // modifier l'entité après coup ne serait jamais réécrit en base.
        if (!examens.isEmpty()) {
            examens.get(0).cloturer();
        }
        examenRepository.saveAll(examens);

        List<Note> notes = new ArrayList<>();
        for (int e = 0; e < examens.size(); e++) {
            Examen examen = examens.get(e);
            for (Groupe groupe : examen.getGroupes()) {
                List<Etudiant> etudiants = etudiantRepository.findByGroupeOrderByNomAscPrenomAsc(groupe);
                for (int s = 0; s < etudiants.size(); s++) {
                    if (!examen.isCloturee() && (s + e) % 3 == 0) {
                        continue;   // pas encore de note : la liste de saisie garde toujours des candidats
                    }
                    notes.add(new Note(valeur(s, e), etudiants.get(s), examen, examen.getEnseignant()));
                }
            }
        }
        noteRepository.saveAll(notes);

        log.info("Examens reçus du webservice et enregistrés : {} examens, {} notes", examens.size(), notes.size());
    }

    // Une note entre 6 et 17,75, par pas de 0,25, déterministe
    private BigDecimal valeur(int s, int e) {
        return BigDecimal.valueOf(6 + ((s * 7 + e * 3) % 12) + 0.25 * ((s + e) % 4)).setScale(2, RoundingMode.HALF_UP);
    }
}
