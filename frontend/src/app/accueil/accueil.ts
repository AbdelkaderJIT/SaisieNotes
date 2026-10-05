import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Theme, ThemeService } from '../core/theme.service';

// Page publique affichée avant la connexion : identité de l'établissement et accès à l'espace enseignants.
@Component({
  selector: 'app-accueil',
  imports: [RouterLink],
  templateUrl: './accueil.html',
  styleUrl: './accueil.css',
})
export class Accueil {
  private readonly themeService = inject(ThemeService);

  protected readonly theme = this.themeService.theme;

  protected choisirTheme(theme: Theme): void {
    this.themeService.choisir(theme);
  }
}
