package tn.espacenote.notedemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Groupe;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.GroupeRepository;
import tn.espacenote.notedemo.repository.MatiereRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Données de référence (Faculté de Droit), insérées une seule fois (base vide) : enseignants, matières,
// groupes et étudiants. Les examens et les notes n'en font PAS partie : ils arrivent via
// ExamenIngestionRunner (le "webservice" d'examens), jamais construits ici à la main — c'est tout
// l'enjeu de la nouvelle règle métier (un examen n'est jamais ajouté par un formulaire).
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final GroupeRepository groupeRepository;
    private final EtudiantRepository etudiantRepository;

    @Override
    public void run(String... args) {
        if (enseignantRepository.count() > 0) {
            return;
        }

        // ---------- Matières ----------
        Matiere obligations = matiere("DRT101", "Droit des obligations", 1);
        Matiere penal = matiere("DRT102", "Droit pénal général", 1);              // partagée entre Ali et Sonia
        Matiere constitutionnel = matiere("DRT103", "Droit constitutionnel", 1);
        Matiere anglais = matiere("LNG101", "Anglais juridique", 2);

        List<Matiere> autresAli = List.of(
                matiere("DRT104", "Droit des biens", 1),
                matiere("DRT201", "Droit commercial", 2),
                matiere("DRT202", "Procédure civile", 2),
                matiere("DRT203", "Droit administratif", 2),
                matiere("DRT301", "Droit du travail", 3),
                matiere("DRT302", "Droit international privé", 3),
                matiere("DRT303", "Droit des sociétés", 3),
                matiere("DRT304", "Droit de la famille", 3));

        List<Matiere> autresSonia = List.of(
                matiere("DRT105", "Histoire du droit", 1),
                matiere("DRT106", "Institutions juridictionnelles", 1),
                matiere("DRT204", "Finances publiques", 2),
                matiere("DRT205", "Droit international public", 2),
                matiere("DRT305", "Droit fiscal", 3),
                matiere("DRT306", "Droit des assurances", 3),
                matiere("LNG201", "Méthodologie juridique", 2));

        List<Matiere> toutes = new ArrayList<>(List.of(obligations, penal, constitutionnel, anglais));
        toutes.addAll(autresAli);
        toutes.addAll(autresSonia);
        matiereRepository.saveAll(toutes);

        // ---------- Enseignants : 10 matières chacun (la matière partagée compte pour les deux) ----------
        List<Matiere> matieresAli = new ArrayList<>(List.of(obligations, penal));
        matieresAli.addAll(autresAli);
        List<Matiere> matieresSonia = new ArrayList<>(List.of(penal, constitutionnel, anglais));
        matieresSonia.addAll(autresSonia);

        Enseignant ali = enseignant("Ben Ali", "Ali", "ali.benali@fds.tn", Set.copyOf(matieresAli));
        Enseignant sonia = enseignant("Trabelsi", "Sonia", "sonia.trabelsi@fds.tn", Set.copyOf(matieresSonia));
        enseignantRepository.saveAll(List.of(ali, sonia));

        // ---------- Groupes ----------
        Groupe l2a = groupe("L2-A");
        Groupe l2b = groupe("L2-B");
        groupeRepository.saveAll(List.of(l2a, l2b));

        // ---------- Étudiants : des tailles de classe réalistes, pas un chiffre rond identique partout ----------
        int tailleL2A = 24;   // le reste (36) va à L2-B
        String[] noms = {
                "Gharbi", "Mansour", "Jlassi", "Hamdi", "Bouzid", "Kefi", "Trabelsi", "Chaabane", "Ben Salah", "Ayadi",
                "Zouari", "Dridi", "Sfaxi", "Mekki", "Ghorbel", "Fourati", "Khemiri", "Jebali", "Mrad", "Sassi",
                "Bouazizi", "Hammami", "Gharsallah", "Belhadj", "Chebbi", "Nasri", "Rekik", "Bouslama", "Chtioui",
                "Ferjani", "Kallel", "Maalej", "Mejri", "Ouali", "Sayadi", "Tlili", "Zaidi", "Bahri", "Chaouch",
                "Daoud", "Essid", "Guesmi", "Harzallah", "Issaoui", "Jendoubi", "Kchaou", "Lahmar", "Masmoudi",
                "Nouri", "Ouerghi", "Rebai", "Saidi", "Tabbabi", "Werfelli", "Yahyaoui", "Zribi", "Abidi", "Brahmi",
                "Chedly", "Derbali"};
        String[] prenoms = {
                "Amine", "Nour", "Yassine", "Sarra", "Karim", "Ines", "Mariem", "Omar", "Rania", "Firas",
                "Hela", "Walid", "Lina", "Aymen", "Sirine", "Mehdi", "Yassmine", "Bilel", "Sabrine", "Anis",
                "Emna", "Hamza", "Rim", "Chiheb", "Asma", "Mohamed", "Fatma", "Ahmed", "Khadija", "Seif",
                "Nadia", "Youssef", "Hind", "Wassim", "Amal", "Rayen", "Salma", "Marwen", "Dorra", "Hichem",
                "Olfa", "Nizar", "Rahma", "Sami", "Imen", "Ziad", "Meriem", "Houssem", "Syrine", "Khalil",
                "Nesrine", "Bassem", "Lobna", "Tarek", "Ghada", "Anouar", "Sawssen", "Marwa", "Oussama", "Hajer"};

        List<Etudiant> etudiants = new ArrayList<>();
        for (int i = 0; i < noms.length; i++) {
            Groupe groupe = i < tailleL2A ? l2a : l2b;
            etudiants.add(etudiant(String.format("2024%03d", i + 1), noms[i], prenoms[i], groupe));
        }
        etudiantRepository.saveAll(etudiants);

        log.info("Données de référence créées : {} matières, 2 groupes, {} étudiants ; "
                        + "enseignants [{} -> id {}, {} -> id {}]",
                toutes.size(), etudiants.size(), ali.getEmail(), ali.getId(), sonia.getEmail(), sonia.getId());
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

    private Groupe groupe(String nom) {
        Groupe g = new Groupe();
        g.setNom(nom);
        return g;
    }

    private Etudiant etudiant(String numInscription, String nom, String prenom, Groupe groupe) {
        Etudiant e = new Etudiant();
        e.setNumInscription(numInscription);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setFiliere("Droit");
        e.setNiveau("L2");
        e.setGroupe(groupe);
        return e;
    }
}
