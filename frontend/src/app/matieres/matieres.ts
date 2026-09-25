import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { messageErreur } from '../core/erreurs';
import { Matiere } from '../core/models';
import { NotesApi } from '../core/notes-api.service';

@Component({
  selector: 'app-matieres',
  imports: [RouterLink],
  templateUrl: './matieres.html',
  styleUrl: './matieres.css',
})
export class Matieres {
  private readonly api = inject(NotesApi);

  protected readonly matieres = signal<Matiere[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  constructor() {
    this.api.matieres().subscribe({
      next: (matieres) => {
        this.matieres.set([...matieres].sort((a, b) => a.code.localeCompare(b.code)));
        this.chargement.set(false);
      },
      error: (e) => {
        this.erreur.set(messageErreur(e));
        this.chargement.set(false);
      },
    });
  }
}
