import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { messageErreur } from '../core/erreurs';
import { Etudiant, Matiere, Note } from '../core/models';
import { NotesApi } from '../core/notes-api.service';
import { NoteFormulaire } from './note-formulaire';

// note = null : formulaire de saisie ; sinon : modification de cette note
interface FormulaireOuvert {
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

  // Paramètre d'URL /matieres/:id/notes (lié par withComponentInputBinding)
  readonly id = input.required<string>();

  protected readonly matiere = signal<Matiere | null>(null);
  protected readonly notes = signal<Note[]>([]);
  protected readonly etudiants = signal<Etudiant[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  protected readonly formulaire = signal<FormulaireOuvert | null>(null);
  protected readonly succes = signal<string | null>(null);
  protected readonly erreurAction = signal<string | null>(null);
  protected readonly clotureDemandee = signal(false);

  protected readonly moyenne = computed(() => {
    const notes = this.notes();
    return notes.length === 0 ? null : notes.reduce((somme, n) => somme + n.valeur, 0) / notes.length;
  });

  // Une seule note par étudiant et par matière : la liste de saisie ne propose que ceux qui n'en ont pas
  protected readonly etudiantsSansNote = computed(() => {
    const notes = this.notes();
    return this.etudiants().filter((e) => !notes.some((n) => n.etudiantId === e.id));
  });

  constructor() {
    effect(() => this.charger(Number(this.id())));
  }

  private charger(id: number): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.formulaire.set(null);
    this.succes.set(null);
    this.erreurAction.set(null);
    this.clotureDemandee.set(false);

    forkJoin({
      matiere: this.api.matiere(id),
      notes: this.api.notes(id),
      etudiants: this.api.etudiants(id),
    }).subscribe({
      next: ({ matiere, notes, etudiants }) => {
        this.matiere.set(matiere);
        this.notes.set(this.trier(notes));
        this.etudiants.set(etudiants);
        this.chargement.set(false);
      },
      error: (e) => {
        this.matiere.set(null);
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
    this.formulaire.set({ note: null });
  }

  protected modifier(note: Note): void {
    this.nouveauMessage();
    this.clotureDemandee.set(false);
    this.formulaire.set({ note });
  }

  protected fermerFormulaire(): void {
    this.formulaire.set(null);
  }

  protected onEnregistre(note: Note): void {
    const modifiee = this.notes().some((n) => n.id === note.id);
    this.notes.update((notes) => this.trier([...notes.filter((n) => n.id !== note.id), note]));
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
    this.api.cloturer(Number(this.id())).subscribe({
      next: (matiere) => {
        this.matiere.set(matiere);
        this.clotureDemandee.set(false);
        this.succes.set('Matière clôturée : plus aucune saisie ni modification n\'est possible');
      },
      error: (e) => {
        this.clotureDemandee.set(false);
        this.erreurAction.set(messageErreur(e));
      },
    });
  }
}
