// Reflètent les DTO du backend (EnseignantResponse, MatiereResponse, ExamenResponse, ...).
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
}

export interface Groupe {
  id: number;
  nom: string;
}

export interface Examen {
  id: number;
  matiereId: number;
  matiereCode: string;
  matiereLibelle: string;
  session: string;
  date: string;
  cloturee: boolean;
  groupes: Groupe[];
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
  examenId: number;
  matiereId: number;
  matiereLibelle: string;
  etudiantId: number;
  etudiantNumInscription: string;
  etudiantNom: string;
  etudiantPrenom: string;
  enseignantId: number;
  dateSaisie: string;
  dateModification: string | null;
}
