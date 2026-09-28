package tn.espacenote.notedemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Etudiant;
import tn.espacenote.notedemo.model.Inscription;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.model.Note;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.EtudiantRepository;
import tn.espacenote.notedemo.repository.InscriptionRepository;
import tn.espacenote.notedemo.repository.MatiereRepository;
import tn.espacenote.notedemo.repository.NoteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Données de démonstration, insérées une seule fois (base vide) pour tester l'API tout de suite.
//
// Contenu (Faculté de Droit) : 2 enseignants qui ont chacun 10 matières, 16 étudiants, leurs inscriptions
// (un étudiant n'a qu'UN enseignant par matière) et des notes.
// Les ids sont les mêmes à chaque démarrage : tout est enregistré dans un ordre fixe (listes, pas Set.of).
// Les quatre premières matières, les six premiers étudiants et les quatre premières notes gardent leurs ids
// d'origine : la collection Postman et le README s'y réfèrent.
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final EtudiantRepository etudiantRepository;
    private final InscriptionRepository inscriptionRepository;
    private final NoteRepository noteRepository;

    @Override
    public void run(String... args) {
        if (enseignantRepository.count() > 0) {
            return;
        }

        // ---------- Matières ----------
        // ids 1 à 4 : celles de la première version de la démonstration
        Matiere obligations = matiere("DRT101", "Droit des obligations", 1);        // 1  Ali
        Matiere penal = matiere("DRT102", "Droit pénal général", 1);                // 2  PARTAGÉE (Ali et Sonia, étudiants différents)
        Matiere constitutionnel = matiere("DRT103", "Droit constitutionnel", 1);    // 3  Sonia, déjà clôturée
        constitutionnel.cloturer();
        Matiere anglais = matiere("LNG101", "Anglais juridique", 2);                // 4  Sonia

        // ids 5 à 12 : d'autres matières d'Ali (la première, DRT104, est clôturée pour démontrer la règle chez lui aussi)
        Matiere biens = matiere("DRT104", "Droit des biens", 1);
        biens.cloturer();
        List<Matiere> autresAli = List.of(biens,
                matiere("DRT201", "Droit commercial", 2),
                matiere("DRT202", "Procédure civile", 2),
                matiere("DRT203", "Droit administratif", 2),
                matiere("DRT301", "Droit du travail", 3),
                matiere("DRT302", "Droit international privé", 3),
                matiere("DRT303", "Droit des sociétés", 3),
                matiere("DRT304", "Droit de la famille", 3));

        // ids 13 à 19 : d'autres matières de Sonia
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

        // ---------- Étudiants (les six premiers sont ceux de la première version) ----------
        List<Etudiant> etudiants = List.of(
                etudiant("2024001", "Gharbi", "Amine"),       // 1
                etudiant("2024002", "Mansour", "Nour"),       // 2
                etudiant("2024003", "Jlassi", "Yassine"),     // 3
                etudiant("2024004", "Hamdi", "Sarra"),        // 4
                etudiant("2024005", "Bouzid", "Karim"),       // 5   pas inscrit en DRT101 : sert à tester le refus
                etudiant("2024006", "Kefi", "Ines"),          // 6
                etudiant("2024007", "Trabelsi", "Mariem"),
                etudiant("2024008", "Chaabane", "Omar"),
                etudiant("2024009", "Ben Salah", "Rania"),
                etudiant("2024010", "Ayadi", "Firas"),
                etudiant("2024011", "Zouari", "Hela"),
                etudiant("2024012", "Dridi", "Walid"),
                etudiant("2024013", "Sfaxi", "Lina"),
                etudiant("2024014", "Mekki", "Aymen"),
                etudiant("2024015", "Ghorbel", "Sirine"),
                etudiant("2024016", "Fourati", "Mehdi"));
        etudiantRepository.saveAll(etudiants);
        Etudiant amine = etudiants.get(0), nour = etudiants.get(1), yassine = etudiants.get(2),
                sarra = etudiants.get(3), karim = etudiants.get(4), ines = etudiants.get(5),
                mariem = etudiants.get(6), omar = etudiants.get(7), rania = etudiants.get(8), firas = etudiants.get(9);

        // ---------- Inscriptions : (étudiant, matière) -> l'enseignant qui l'a en charge ----------
        List<Inscription> inscriptions = new ArrayList<>(List.of(
                // DRT101 : Ali seul
                new Inscription(amine, obligations, ali), new Inscription(nour, obligations, ali),
                new Inscription(yassine, obligations, ali), new Inscription(sarra, obligations, ali),
                // DRT102, partagée : le groupe d'Ali et le groupe de Sonia (étudiants différents)
                new Inscription(amine, penal, ali), new Inscription(nour, penal, ali),
                new Inscription(mariem, penal, ali), new Inscription(omar, penal, ali),
                new Inscription(sarra, penal, sonia), new Inscription(karim, penal, sonia),
                new Inscription(ines, penal, sonia), new Inscription(rania, penal, sonia),
                new Inscription(firas, penal, sonia),
                // DRT103 (clôturée) et LNG101 : Sonia seule
                new Inscription(amine, constitutionnel, sonia), new Inscription(yassine, constitutionnel, sonia),
                new Inscription(sarra, constitutionnel, sonia), new Inscription(ines, constitutionnel, sonia),
                new Inscription(nour, anglais, sonia), new Inscription(sarra, anglais, sonia),
                new Inscription(karim, anglais, sonia), new Inscription(ines, anglais, sonia)));

        // Notes de départ (mêmes ids qu'avant) : 1 et 2 chez Ali, 3 dans la matière clôturée, 4 dans le groupe de Sonia
        List<Note> notes = new ArrayList<>(List.of(
                new Note(new BigDecimal("14.50"), amine, obligations, ali),
                new Note(new BigDecimal("12.75"), nour, obligations, ali),
                new Note(new BigDecimal("16.00"), amine, constitutionnel, sonia),
                new Note(new BigDecimal("11.00"), sarra, penal, sonia)));      // invisible pour Ali

        // Les autres matières : environ trois étudiants sur quatre y sont inscrits, et seulement une partie a déjà
        // une note (pour que la liste de saisie ait toujours des étudiants à proposer). Dans une matière clôturée,
        // tous les inscrits sont notés. Les valeurs sont calculées, donc identiques à chaque démarrage.
        remplir(autresAli, ali, etudiants, inscriptions, notes);
        remplir(autresSonia, sonia, etudiants, inscriptions, notes);

        inscriptionRepository.saveAll(inscriptions);
        noteRepository.saveAll(notes);

        log.info("Données de démonstration créées : {} matières, {} étudiants, {} inscriptions, {} notes ; "
                        + "enseignants [{} -> id {}, {} -> id {}]",
                toutes.size(), etudiants.size(), inscriptions.size(), notes.size(),
                ali.getEmail(), ali.getId(), sonia.getEmail(), sonia.getId());
    }

    // Inscrit une partie des étudiants à chaque matière de l'enseignant et note certains d'entre eux
    private void remplir(List<Matiere> matieres, Enseignant enseignant, List<Etudiant> etudiants,
                         List<Inscription> inscriptions, List<Note> notes) {
        for (int m = 0; m < matieres.size(); m++) {
            Matiere matiere = matieres.get(m);
            for (int s = 0; s < etudiants.size(); s++) {
                if ((s + m + 1) % 4 == 0) {
                    continue;                                   // cet étudiant n'a pas cette matière
                }
                inscriptions.add(new Inscription(etudiants.get(s), matiere, enseignant));
                if (matiere.isCloturee() || (s + 2 * m) % 3 == 0) {
                    notes.add(new Note(valeur(s, m), etudiants.get(s), matiere, enseignant));
                }
            }
        }
    }

    // Une note entre 6 et 17,75, par pas de 0,25, déterministe
    private BigDecimal valeur(int s, int m) {
        return BigDecimal.valueOf(6 + ((s * 7 + m * 3) % 12) + 0.25 * ((s + m) % 4)).setScale(2, RoundingMode.HALF_UP);
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

    private Etudiant etudiant(String numInscription, String nom, String prenom) {
        Etudiant e = new Etudiant();
        e.setNumInscription(numInscription);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setFiliere("Droit");
        e.setNiveau("L2");
        return e;
    }
}
