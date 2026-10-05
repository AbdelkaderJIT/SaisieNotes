package tn.espacenote.notedemo.examensource;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.espacenote.notedemo.model.Enseignant;
import tn.espacenote.notedemo.model.Groupe;
import tn.espacenote.notedemo.model.Matiere;
import tn.espacenote.notedemo.repository.EnseignantRepository;
import tn.espacenote.notedemo.repository.GroupeRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Simule le système d'examens externe (celui de l'université) à partir des matières déjà affectées
// à chaque enseignant : aujourd'hui une génération déterministe, demain un vrai système qu'on ne
// contrôle pas. Ce contrôleur n'est PAS appelé par le frontend Angular : seul ExamenSourceClient
// l'appelle, en HTTP, exactement comme il appellerait une vraie adresse externe.
// GET, hors /api/** : déjà public via la règle de SecurityConfig qui sert les pages Angular.
// Un vrai système externe aurait sa propre authentification (clé d'API, mTLS...), ajoutée le jour où
// ce contrôleur est remplacé par un vrai appel sortant.
@RestController
@RequestMapping("/webservice/examens")
@RequiredArgsConstructor
public class ExamenSourceController {

    private static final LocalDate DEBUT = LocalDate.of(2025, 11, 10);

    private final EnseignantRepository enseignantRepository;
    private final GroupeRepository groupeRepository;

    @GetMapping
    public List<ExamenSourceDto> examens() {
        List<String> tousLesGroupes = groupeRepository.findAll().stream().map(Groupe::getNom).toList();
        List<ExamenSourceDto> resultat = new ArrayList<>();
        int i = 0;
        for (Enseignant enseignant : enseignantRepository.findAll()) {
            for (Matiere matiere : enseignant.getMatieres()) {
                List<String> groupes = groupesDe(matiere, enseignant, tousLesGroupes);
                resultat.add(new ExamenSourceDto(matiere.getCode(), enseignant.getEmail(), "DS1",
                        DEBUT.plusDays(i * 2L), groupes));
                resultat.add(new ExamenSourceDto(matiere.getCode(), enseignant.getEmail(), "Examen final",
                        DEBUT.plusDays(60 + i * 2L), groupes));
                i++;
            }
        }
        return resultat;
    }

    // DRT102 est partagée entre deux enseignants : chacun n'a QUE son propre groupe pour cette matière.
    // Toutes les autres matières sont données à toutes les classes.
    private List<String> groupesDe(Matiere matiere, Enseignant enseignant, List<String> tousLesGroupes) {
        if ("DRT102".equals(matiere.getCode()) && tousLesGroupes.size() >= 2) {
            boolean premier = "ali.benali@fds.tn".equals(enseignant.getEmail());
            return List.of(premier ? tousLesGroupes.get(0) : tousLesGroupes.get(1));
        }
        return tousLesGroupes;
    }
}
