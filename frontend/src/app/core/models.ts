// Reflètent les DTO du backend (EnseignantResponse, MatiereResponse, NoteResponse, ...).
export interface Enseignant {
  id: number;
  nom: string;
  prenom: string;
  email: string;
}

export interface Matiere {
  id: number;
  code: string;
  libelle: string;
  semestre: number;
  cloturee: boolean;
}

export interface Etudiant {
  id: number;
  numInscription: string;
  nom: string;
  prenom: string;
}

export interface Note {
  id: number;
  valeur: number;
  matiereId: number;
  etudiantId: number;
  etudiantNumInscription: string;
  etudiantNom: string;
  etudiantPrenom: string;
  enseignantId: number;
  dateSaisie: string;
  dateModification: string | null;
}
