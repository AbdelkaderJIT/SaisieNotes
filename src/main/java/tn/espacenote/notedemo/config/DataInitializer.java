package tn.espacenote.notedemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Données de démonstration (Faculté de Droit), insérées une seule fois (base vide) : enseignants,
// matières, groupes, étudiants, examens et quelques notes déjà saisies. En production, les examens
// sont créés par l'administration via l'API /api/admin (ExamenAdminController), jamais ici ; ce jeu de
// données ne sert qu'à avoir quelque chose à montrer au premier démarrage.
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final LocalDate DEBUT = LocalDate.of(2025, 11, 10);

    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final GroupeRepository groupeRepository;
    private final EtudiantRepository etudiantRepository;
    private final ExamenRepository examenRepository;
    private final NoteRepository noteRepository;

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

        // ---------- Examens : 2 sessions (DS1, Examen final) par matière affectée à chaque enseignant.
        // DRT102 est partagée : chaque enseignant n'y couvre QUE son propre groupe. Les autres matières
        // couvrent les deux groupes à la fois (démontre qu'un examen peut viser plusieurs groupes). ----------
        List<Groupe> tousLesGroupes = List.of(l2a, l2b);
        List<Examen> examens = new ArrayList<>();
        examensDe(ali, matieresAli, l2a, tousLesGroupes, examens);
        examensDe(sonia, matieresSonia, l2b, tousLesGroupes, examens);
        // Démo : le tout premier examen créé est déjà clôturé (tous ses étudiants sont notés).
        examens.get(0).cloturer();
        examenRepository.saveAll(examens);

        // ---------- Notes : toutes pour l'examen clôturé, une partie pour les autres ----------
        List<Note> notes = new ArrayList<>();
        for (int e = 0; e < examens.size(); e++) {
            Examen examen = examens.get(e);
            for (Groupe groupe : examen.getGroupes()) {
                List<Etudiant> inscrits = etudiantRepository.findByGroupeOrderByNomAscPrenomAsc(groupe);
                for (int s = 0; s < inscrits.size(); s++) {
                    if (!examen.isCloturee() && (s + e) % 3 == 0) {
                        continue;   // pas encore de note : la liste de saisie garde toujours des candidats
                    }
                    notes.add(new Note(valeur(s, e), inscrits.get(s), examen, examen.getEnseignant()));
                }
            }
        }
        noteRepository.saveAll(notes);

        log.info("Données de démonstration créées : {} matières, 2 groupes, {} étudiants, {} examens, {} notes ; "
                        + "enseignants [{} -> id {}, {} -> id {}]",
                toutes.size(), etudiants.size(), examens.size(), notes.size(),
                ali.getEmail(), ali.getId(), sonia.getEmail(), sonia.getId());
    }

    // DS1 et Examen final pour chaque matière de l'enseignant. DRT102 (partagée) ne couvre que le groupe
    // propre à cet enseignant ; les autres matières couvrent tous les groupes à la fois.
    private void examensDe(Enseignant enseignant, List<Matiere> matieres, Groupe groupePropre,
                           List<Groupe> tousLesGroupes, List<Examen> examens) {
        int i = examens.size();
        for (Matiere matiere : matieres) {
            boolean partagee = "DRT102".equals(matiere.getCode());
            List<Groupe> groupesDeLExamen = partagee ? List.of(groupePropre) : tousLesGroupes;
            Examen ds1 = new Examen(matiere, enseignant, "DS1", DEBUT.plusDays(i * 2L));
            Examen fin = new Examen(matiere, enseignant, "Examen final", DEBUT.plusDays(60 + i * 2L));
            ds1.getGroupes().addAll(groupesDeLExamen);
            fin.getGroupes().addAll(groupesDeLExamen);
            examens.add(ds1);
            examens.add(fin);
            i++;
        }
    }

    // Une note entre 6 et 17,75, par pas de 0,25, déterministe
    private BigDecimal valeur(int s, int e) {
        return BigDecimal.valueOf(6 + ((s * 7 + e * 3) % 12) + 0.25 * ((s + e) % 4)).setScale(2, RoundingMode.HALF_UP);
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
