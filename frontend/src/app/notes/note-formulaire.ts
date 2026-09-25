import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { messageErreur } from '../core/erreurs';
import { Etudiant, Note } from '../core/models';
import { NotesApi } from '../core/notes-api.service';

// Formulaire de saisie (note = null) ou de modification (note fournie). Validation à deux niveaux :
// ici le format (mêmes règles que NoteForm côté serveur), puis les règles métier renvoyées par l'API.
@Component({
  selector: 'app-note-formulaire',
  imports: [ReactiveFormsModule],
  templateUrl: './note-formulaire.html',
  styleUrl: './note-formulaire.css',
})
export class NoteFormulaire {
  private readonly api = inject(NotesApi);

  readonly matiereId = input.required<number>();
  readonly etudiants = input<Etudiant[]>([]);
  readonly note = input<Note | null>(null);

  readonly enregistre = output<Note>();
  readonly annule = output<void>();

  protected readonly modification = computed(() => this.note() !== null);

  protected readonly form = new FormGroup({
    etudiantId: new FormControl<number | null>(null, Validators.required),
    valeur: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(20),
      Validators.pattern(/^\d{1,2}(\.\d{1,2})?$/),   // au plus 2 décimales
    ]),
  });

  protected readonly erreur = signal<string | null>(null);
  protected readonly enCours = signal(false);

  constructor() {
    // Réinitialise le formulaire quand on passe d'une saisie à une modification (ou d'une note à une autre)
    effect(() => {
      const note = this.note();
      untracked(() => this.initialiser(note));
    });
  }

  private initialiser(note: Note | null): void {
    this.erreur.set(null);
    this.form.reset({ etudiantId: note?.etudiantId ?? null, valeur: note?.valeur ?? null });
    if (note) {
      this.form.controls.etudiantId.disable();   // l'étudiant d'une note existante ne change pas
    } else {
      this.form.controls.etudiantId.enable();
    }
  }

  protected messageEtudiant(): string | null {
    const controle = this.form.controls.etudiantId;
    if (!controle.touched || !controle.errors) {
      return null;
    }
    return controle.errors['serveur'] ?? 'Choisissez un étudiant';
  }

  protected messageValeur(): string | null {
    const erreurs = this.form.controls.valeur.errors;
    if (!this.form.controls.valeur.touched || !erreurs) {
      return null;
    }
    if (erreurs['serveur']) return erreurs['serveur'];
    if (erreurs['required']) return 'La note est obligatoire';
    if (erreurs['min'] || erreurs['max']) return 'La note doit être comprise entre 0 et 20';
    return 'La note doit avoir au plus 2 décimales (ex. 12.75)';
  }

  protected soumettre(): void {
    this.erreur.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { etudiantId, valeur } = this.form.getRawValue();
    const note = this.note();
    const requete = note
      ? this.api.modifier(note.id, valeur!)
      : this.api.saisir(this.matiereId(), { etudiantId: etudiantId!, valeur: valeur! });

    this.enCours.set(true);
    requete.subscribe({
      next: (enregistree) => {
        this.enCours.set(false);
        this.enregistre.emit(enregistree);
      },
      error: (e: HttpErrorResponse) => {
        this.enCours.set(false);
        this.afficherErreur(e);
      },
    });
  }

  // Erreurs de champ du serveur (400) affichées sous le champ ; règles métier (403, 409, 422...) en message global.
  private afficherErreur(e: HttpErrorResponse): void {
    const champs = e.status === 400 ? (e.error?.champs as Record<string, string> | undefined) : undefined;
    if (!champs) {
      this.erreur.set(messageErreur(e));
      return;
    }
    for (const [nom, message] of Object.entries(champs)) {
      const controle = this.form.get(nom);
      if (controle) {
        controle.setErrors({ serveur: message });
        controle.markAsTouched();
      } else {
        this.erreur.set(message);
      }
    }
  }
}
