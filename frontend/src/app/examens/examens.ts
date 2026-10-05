import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { messageErreur } from '../core/erreurs';
import { Examen, Groupe, Matiere } from '../core/models';
import { NotesApi } from '../core/notes-api.service';

@Component({
  selector: 'app-examens',
  imports: [RouterLink, DatePipe],
  templateUrl: './examens.html',
  styleUrl: './examens.css',
})
export class Examens {
  private readonly api = inject(NotesApi);

  protected readonly matieres = signal<Matiere[]>([]);
  protected readonly groupes = signal<Groupe[]>([]);
  protected readonly examens = signal<Examen[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);

  // Filtres : appliqués côté client, la liste tient largement en mémoire.
  protected readonly recherche = signal('');
  protected readonly matiereChoisie = signal<number | null>(null);
  protected readonly groupeChoisi = signal<number | null>(null);

  protected readonly examensFiltres = computed(() => {
    const matiereId = this.matiereChoisie();
    const groupeId = this.groupeChoisi();
    const mots = this.recherche().trim().toLowerCase();

    return this.examens().filter((e) => {
      if (matiereId !== null && e.matiereId !== matiereId) return false;
      if (groupeId !== null && !e.groupes.some((g) => g.id === groupeId)) return false;
      if (mots === '') return true;
      const texte = [e.matiereCode, e.matiereLibelle, e.session, ...e.groupes.map((g) => g.nom)]
        .join(' ')
        .toLowerCase();
      return texte.includes(mots);
    });
  });

  constructor() {
    forkJoin({
      matieres: this.api.matieres(),
      groupes: this.api.groupes(),
      examens: this.api.examens(),
    }).subscribe({
      next: ({ matieres, groupes, examens }) => {
        this.matieres.set([...matieres].sort((a, b) => a.code.localeCompare(b.code)));
        this.groupes.set([...groupes].sort((a, b) => a.nom.localeCompare(b.nom)));
        this.examens.set(examens);
        this.chargement.set(false);
      },
      error: (e) => {
        this.erreur.set(messageErreur(e));
        this.chargement.set(false);
      },
    });
  }

  protected rechercher(mots: string): void {
    this.recherche.set(mots);
  }

  protected filtrerParMatiere(id: string): void {
    this.matiereChoisie.set(id === '' ? null : Number(id));
  }

  protected filtrerParGroupe(id: string): void {
    this.groupeChoisi.set(id === '' ? null : Number(id));
  }
}
