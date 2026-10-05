import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Etudiant, Examen, Groupe, Matiere, Note } from './models';

export interface NouvelleNote {
  etudiantId: number;
  valeur: number;
}

// Seul point d'accès à l'API : les composants n'écrivent jamais d'URL. L'intercepteur ajoute l'authentification.
@Injectable({ providedIn: 'root' })
export class NotesApi {
  private readonly http = inject(HttpClient);

  // Alimentent les filtres de la liste d'examens.
  matieres(): Observable<Matiere[]> {
    return this.http.get<Matiere[]>('/api/matieres');
  }

  groupes(): Observable<Groupe[]> {
    return this.http.get<Groupe[]>('/api/groupes');
  }

  examens(matiereId?: number, groupeId?: number): Observable<Examen[]> {
    let params = new HttpParams();
    if (matiereId != null) params = params.set('matiereId', matiereId);
    if (groupeId != null) params = params.set('groupeId', groupeId);
    return this.http.get<Examen[]>('/api/examens', { params });
  }

  examen(id: number): Observable<Examen> {
    return this.http.get<Examen>(`/api/examens/${id}`);
  }

  // Étudiants du groupe qui n'ont pas encore de note pour cet examen (liste déroulante de saisie).
  etudiantsSansNote(examenId: number, groupeId: number): Observable<Etudiant[]> {
    return this.http.get<Etudiant[]>(`/api/examens/${examenId}/groupes/${groupeId}/etudiants`);
  }

  notesDe(examenId: number, groupeId: number): Observable<Note[]> {
    return this.http.get<Note[]>(`/api/examens/${examenId}/groupes/${groupeId}/notes`);
  }

  saisir(examenId: number, groupeId: number, note: NouvelleNote): Observable<Note> {
    return this.http.post<Note>(`/api/examens/${examenId}/groupes/${groupeId}/notes`, note);
  }

  modifier(noteId: number, valeur: number): Observable<Note> {
    return this.http.put<Note>(`/api/notes/${noteId}`, { valeur });
  }

  cloturerExamen(examenId: number): Observable<Examen> {
    return this.http.post<Examen>(`/api/examens/${examenId}/cloturer`, null);
  }
}
