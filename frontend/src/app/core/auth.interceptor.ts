import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// Signe chaque appel /api avec les identifiants en mémoire, et renvoie vers /login si le serveur
// répond 401 (mot de passe devenu invalide, etc.).
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const authorization = auth.authorizationHeader();

  // Pas connecté, hors API, ou en-tête déjà fourni (cas du login) : on ne touche à rien.
  if (!authorization || !req.url.startsWith('/api') || req.headers.has('Authorization')) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: authorization } })).pipe(
    catchError((erreur) => {
      if (erreur.status === 401) {
        auth.logout();
      }
      return throwError(() => erreur);
    }),
  );
};
