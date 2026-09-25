import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

// Les pages protégées redirigent vers /login tant que l'utilisateur n'est pas connecté.
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/login']);
};

// Inverse : la page de connexion n'a pas de sens pour quelqu'un déjà connecté (bouton « Retour » du navigateur).
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? inject(Router).createUrlTree(['/matieres']) : true;
};
