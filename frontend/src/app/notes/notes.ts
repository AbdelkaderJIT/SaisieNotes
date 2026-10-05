import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { messageErreur } from '../core/erreurs';
import { Etudiant, Examen, Note } from '../core/models';
import { NotesApi } from '../core/notes-api.service';
import { NoteFormulaire } from './note-formulaire';

// note = null : formulaire de saisie ; sinon : modification de cette note
interface FormulaireOuvert {
  note: Note | null;
  etudiant: Etudiant | null;
}

// Une ligne du tableau : soit un étudiant déjà noté (note renseignée), soit sans note encore (note = null).
// Union de notes() et etudiants() (le serveur ne renvoie dans etudiants() que ceux sans note) : chaque
// étudiant du groupe apparaît exactement une fois.
interface LigneRoster {
  etudiantId: number;
  numInscription: string;
  nom: string;
  prenom: string;
  note: Note | null;
}

@Component({
  selector: 'app-notes',
  imports: [RouterLink, DatePipe, DecimalPipe, NoteFormulaire],
  templateUrl: './notes.html',
  styleUrl: './notes.css',
})
export class Notes {
  private readonly api = inject(NotesApi);

  // Paramètres d'URL /examens/:id/groupes/:groupeId/notes (liés par withComponentInputBinding)
  readonly id = input.required<string>();
  readonly groupeId = input.required<string>();

  protected readonly examen = signal<Examen | null>(null);
  protected readonly notes = signal<Note[]>([]);
  protected readonly etudiants = signal<Etudiant[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  protected readonly formulaire = signal<FormulaireOuvert | null>(null);
  protected readonly succes = signal<string | null>(null);
  protected readonly erreurAction = signal<string | null>(null);
  protected readonly clotureDemandee = signal(false);

  protected readonly groupeIdNum = computed(() => Number(this.groupeId()));
  protected readonly groupeNom = computed(
    () => this.examen()?.groupes.find((g) => g.id === this.groupeIdNum())?.nom ?? '',
  );

  // Tout le groupe, noté ou non : affiché dans le tableau même si "il n'a pas encore de note" doit rester visible.
  protected readonly roster = computed<LigneRoster[]>(() => {
    const notees: LigneRoster[] = this.notes().map((n) => ({
      etudiantId: n.etudiantId,
      numInscription: n.etudiantNumInscription,
      nom: n.etudiantNom,
      prenom: n.etudiantPrenom,
      note: n,
    }));
    const nonNotees: LigneRoster[] = this.etudiants().map((e) => ({
      etudiantId: e.id,
      numInscription: e.numInscription,
      nom: e.nom,
      prenom: e.prenom,
      note: null,
    }));
    return [...notees, ...nonNotees].sort((a, b) => a.nom.localeCompare(b.nom));
  });

  constructor() {
    effect(() => this.charger(Number(this.id()), Number(this.groupeId())));
  }

  private charger(examenId: number, groupeId: number): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.formulaire.set(null);
    this.succes.set(null);
    this.erreurAction.set(null);
    this.clotureDemandee.set(false);

    forkJoin({
      examen: this.api.examen(examenId),
      notes: this.api.notesDe(examenId, groupeId),
      etudiants: this.api.etudiantsSansNote(examenId, groupeId),
    }).subscribe({
      next: ({ examen, notes, etudiants }) => {
        this.examen.set(examen);
        this.notes.set(this.trier(notes));
        this.etudiants.set(etudiants);
        this.chargement.set(false);
      },
      error: (e) => {
        this.examen.set(null);
        this.erreur.set(messageErreur(e));
        this.chargement.set(false);
      },
    });
  }

  private trier(notes: Note[]): Note[] {
    return [...notes].sort((a, b) => a.etudiantNom.localeCompare(b.etudiantNom));
  }

  private nouveauMessage(): void {
    this.succes.set(null);
    this.erreurAction.set(null);
  }

  // ---- saisie et modification ----

  protected ajouter(): void {
    this.nouveauMessage();
    this.clotureDemandee.set(false);
    this.formulaire.set({ note: null, etudiant: null });
  }

  // Depuis la ligne d'un étudiant sans note : saisie directe pour lui
  protected ajouterPour(ligne: LigneRoster): void {
    this.nouveauMessage();
    this.clotureDemandee.set(false);
    this.formulaire.set({
      note: null,
      etudiant: { id: ligne.etudiantId, numInscription: ligne.numInscription, nom: ligne.nom, prenom: ligne.prenom },
    });
  }

  protected modifier(note: Note): void {
    this.nouveauMessage();
    this.clotureDemandee.set(false);
    this.formulaire.set({ note, etudiant: null });
  }

  protected fermerFormulaire(): void {
    this.formulaire.set(null);
  }

  protected onEnregistre(note: Note): void {
    const modifiee = this.notes().some((n) => n.id === note.id);
    this.notes.update((notes) => this.trier([...notes.filter((n) => n.id !== note.id), note]));
    if (!modifiee) {
      // La liste de saisie ne propose que les étudiants sans note : celui-ci vient d'en recevoir une.
      this.etudiants.update((etudiants) => etudiants.filter((e) => e.id !== note.etudiantId));
    }
    this.formulaire.set(null);
    this.succes.set(`Note de ${note.etudiantPrenom} ${note.etudiantNom} ${modifiee ? 'modifiée' : 'enregistrée'}`);
  }

  // ---- clôture ----

  protected demanderCloture(): void {
    this.nouveauMessage();
    this.formulaire.set(null);
    this.clotureDemandee.set(true);
  }

  protected annulerCloture(): void {
    this.clotureDemandee.set(false);
  }

  protected confirmerCloture(): void {
    this.api.cloturerExamen(Number(this.id())).subscribe({
      next: (examen) => {
        this.examen.set(examen);
        this.clotureDemandee.set(false);
        this.succes.set('Examen clôturé : plus aucune saisie ni modification n\'est possible');
      },
      error: (e) => {
        this.clotureDemandee.set(false);
        this.erreurAction.set(messageErreur(e));
      },
    });
  }
}
