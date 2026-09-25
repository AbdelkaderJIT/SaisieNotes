import { Component, inject } from '@angular/core';
import { AuthService } from '../core/auth.service';

// Page d'accueil après connexion. Provisoire : la liste des matières arrive à l'étape suivante.
@Component({
  selector: 'app-matieres',
  template: `
    <main class="page">
      <h1>Mes matières</h1>
      <p>Bienvenue {{ auth.user()?.prenom }} ! La liste de vos matières arrive à l'étape suivante.</p>
    </main>
  `,
  styles: `
    .page { max-width: 60rem; margin: 0 auto; padding: 1.5rem 1rem; }
  `,
})
export class Matieres {
  protected readonly auth = inject(AuthService);
}
