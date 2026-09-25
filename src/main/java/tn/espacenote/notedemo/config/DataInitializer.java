package tn.espacenote.notedemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.model.Note;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.MatiereRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

// Données de démonstration, insérées une seule fois (base vide) pour tester l'API tout de suite.
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final EtudiantRepository etudiantRepository;
    private final NoteRepository noteRepository;

    @Override
    public void run(String... args) {
        if (enseignantRepository.count() > 0) {
            return;
        }

        // Matières (MAT101 est déjà clôturée pour démontrer la règle de clôture)
        Matiere algo = matiere("INF101", "Algorithmique", 1);
        Matiere bd = matiere("INF102", "Bases de données", 1);
        Matiere analyse = matiere("MAT101", "Analyse", 1);
        analyse.cloturer();
        Matiere anglais = matiere("ANG101", "Anglais technique", 2);
        // Listes ordonnées (pas Set.of) : les ids sont ainsi les mêmes à chaque démarrage
        matiereRepository.saveAll(List.of(algo, bd, analyse, anglais));

        // Enseignants et leurs affectations ("enseigne")
        Enseignant ali = enseignant("Ben Ali", "Ali", "ali.benali@fds.tn", Set.of(algo, bd));
        Enseignant sonia = enseignant("Trabelsi", "Sonia", "sonia.trabelsi@fds.tn", Set.of(bd, analyse, anglais));
        enseignantRepository.saveAll(List.of(ali, sonia));

        // Étudiants et leurs inscriptions ("inscrit")
        // Karim n'est pas inscrit à INF101 : sert à tester le refus "étudiant non inscrit"
        Etudiant amine = etudiant("2024001", "Gharbi", "Amine", Set.of(algo, bd, analyse));
        Etudiant nour = etudiant("2024002", "Mansour", "Nour", Set.of(algo, bd, anglais));
        Etudiant yassine = etudiant("2024003", "Jlassi", "Yassine", Set.of(algo, analyse));
        Etudiant sarra = etudiant("2024004", "Hamdi", "Sarra", Set.of(algo, bd, analyse, anglais));
        Etudiant karim = etudiant("2024005", "Bouzid", "Karim", Set.of(bd, anglais));
        Etudiant ines = etudiant("2024006", "Kefi", "Ines", Set.of(bd, analyse, anglais));
        etudiantRepository.saveAll(List.of(amine, nour, yassine, sarra, karim, ines));

        // Notes existantes
        noteRepository.save(new Note(new BigDecimal("14.50"), amine, algo, ali));
        noteRepository.save(new Note(new BigDecimal("12.75"), nour, algo, ali));
        noteRepository.save(new Note(new BigDecimal("16.00"), amine, analyse, sonia));   // matière clôturée

        log.info("Données de démonstration créées : enseignants [{} -> id {}, {} -> id {}]",
                ali.getEmail(), ali.getId(), sonia.getEmail(), sonia.getId());
    }

    private Matiere matiere(String code, String libelle, int semestre) {
        Matiere m = new Matiere();
        m.setCode(code);
        m.setLibelle(libelle);
        m.setSemestre(semestre);
        return m;
    }

    private Enseignant enseignant(String nom, String prenom, String email, Set<Matiere> matieres) {
        Enseignant e = new Enseignant();
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setEmail(email);
        e.getMatieres().addAll(matieres);
        return e;
    }

    private Etudiant etudiant(String numInscription, String nom, String prenom, Set<Matiere> matieres) {
        Etudiant e = new Etudiant();
        e.setNumInscription(numInscription);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setFiliere("Informatique");
        e.setNiveau("L2");
        e.getMatieres().addAll(matieres);
        return e;
    }
}
