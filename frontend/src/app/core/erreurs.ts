import { HttpErrorResponse } from '@angular/common/http';

// Message à afficher pour une erreur HTTP : celui du backend (déjà en français) s'il existe.
export function messageErreur(erreur: unknown): string {
  if (erreur instanceof HttpErrorResponse) {
    if (erreur.status === 0) {
      return 'Serveur injoignable, réessayez dans un instant';
    }
    const message = erreur.error?.message;
    if (typeof message === 'string') {
      return message;
    }
  }
  return 'Une erreur inattendue est survenue';
}
