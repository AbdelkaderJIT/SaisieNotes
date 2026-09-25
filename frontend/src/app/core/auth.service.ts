import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { Enseignant } from './models';

// En-tête HTTP Basic. Passe par UTF-8 : btoa seul refuse les caractères hors Latin-1 (accents).
function basic(email: string, password: string): string {
  const octets = new TextEncoder().encode(`${email}:${password}`);
  return 'Basic ' + btoa(String.fromCharCode(...octets));
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // Les identifiants ne vivent qu'en mémoire (jamais dans localStorage) : un rechargement de
  // la page déconnecte, et un script tiers ne peut pas les relire dans le stockage du navigateur.
  private authorization: string | null = null;

  private readonly utilisateur = signal<Enseignant | null>(null);
  readonly user = this.utilisateur.asReadonly();
  readonly isLoggedIn = computed(() => this.utilisateur() !== null);

  // Valide les identifiants en appelant /api/me ; ils ne sont mémorisés que si le serveur les accepte.
  login(email: string, password: string): Observable<Enseignant> {
    const authorization = basic(email.trim(), password);
    return this.http
      .get<Enseignant>('/api/me', { headers: { Authorization: authorization } })
      .pipe(
        tap((user) => {
          this.authorization = authorization;
          this.utilisateur.set(user);
        }),
      );
  }

  logout(): void {
    this.authorization = null;
    this.utilisateur.set(null);
    void this.router.navigateByUrl('/login');
  }

  // Lu par l'intercepteur pour signer chaque requête /api.
  authorizationHeader(): string | null {
    return this.authorization;
  }
}
