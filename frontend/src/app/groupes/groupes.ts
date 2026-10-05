import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { messageErreur } from '../core/erreurs';
import { Examen } from '../core/models';
import { NotesApi } from '../core/notes-api.service';

@Component({
  selector: 'app-groupes',
  imports: [RouterLink, DatePipe],
  templateUrl: './groupes.html',
  styleUrl: './groupes.css',
})
export class Groupes {
  private readonly api = inject(NotesApi);

  // Paramètre d'URL /examens/:id/groupes (lié par withComponentInputBinding)
  readonly id = input.required<string>();

  protected readonly examen = signal<Examen | null>(null);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  constructor() {
    effect(() => this.charger(Number(this.id())));
  }

  private charger(id: number): void {
    this.chargement.set(true);
    this.erreur.set(null);

    this.api.examen(id).subscribe({
      next: (examen) => {
        this.examen.set(examen);
        this.chargement.set(false);
      },
      error: (e) => {
        this.examen.set(null);
        this.erreur.set(messageErreur(e));
        this.chargement.set(false);
      },
    });
  }
}
