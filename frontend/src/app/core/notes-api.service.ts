import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Etudiant, Matiere, Note } from './models';

export interface NouvelleNote {
  etudiantId: number;
  valeur: number;
}

// Seul point d'accès à l'API : les composants n'écrivent jamais d'URL. L'intercepteur ajoute l'authentification.
@Injectable({ providedIn: 'root' })
export class NotesApi {
  private readonly http = inject(HttpClient);

  matieres(): Observable<Matiere[]> {
    return this.http.get<Matiere[]>('/api/matieres');
  }

  matiere(id: number): Observable<Matiere> {
    return this.http.get<Matiere>(`/api/matieres/${id}`);
  }

  etudiants(matiereId: number): Observable<Etudiant[]> {
    return this.http.get<Etudiant[]>(`/api/matieres/${matiereId}/etudiants`);
  }

  notes(matiereId: number): Observable<Note[]> {
    return this.http.get<Note[]>(`/api/matieres/${matiereId}/notes`);
  }

  saisir(matiereId: number, note: NouvelleNote): Observable<Note> {
    return this.http.post<Note>(`/api/matieres/${matiereId}/notes`, note);
  }

  modifier(noteId: number, valeur: number): Observable<Note> {
    return this.http.put<Note>(`/api/notes/${noteId}`, { valeur });
  }

  cloturer(matiereId: number): Observable<Matiere> {
    return this.http.post<Matiere>(`/api/matieres/${matiereId}/cloturer`, null);
  }
}
