import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [guestGuard],   // un utilisateur déjà connecté est envoyé directement sur ses matières
    loadComponent: () => import('./accueil/accueil').then((m) => m.Accueil),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./login/login').then((m) => m.Login),
  },
  {
    path: 'matieres',
    canActivate: [authGuard],
    loadComponent: () => import('./matieres/matieres').then((m) => m.Matieres),
  },
  {
    path: 'matieres/:id/notes',
    canActivate: [authGuard],
    loadComponent: () => import('./notes/notes').then((m) => m.Notes),
  },
  { path: '**', redirectTo: '' },
];
