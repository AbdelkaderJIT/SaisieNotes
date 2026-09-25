import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./login/login').then((m) => m.Login) },
  {
    path: 'matieres',
    canActivate: [authGuard],
    loadComponent: () => import('./matieres/matieres').then((m) => m.Matieres),
  },
  { path: '', pathMatch: 'full', redirectTo: 'matieres' },
  { path: '**', redirectTo: 'matieres' },
];
