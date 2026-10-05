import { Injectable, signal } from '@angular/core';

export type Theme = 'systeme' | 'clair' | 'sombre';

const CLE_STOCKAGE = 'notedemo-theme';

// Préférence de thème, persistée dans localStorage et appliquée via l'attribut data-theme sur <html>
// (voir styles.css). 'systeme' suit prefers-color-scheme ; 'clair'/'sombre' le forcent explicitement.
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly preference = signal<Theme>(this.lire());
  readonly theme = this.preference.asReadonly();

  constructor() {
    this.appliquer(this.preference());
  }

  choisir(theme: Theme): void {
    this.preference.set(theme);
    this.appliquer(theme);
    try {
      localStorage.setItem(CLE_STOCKAGE, theme);
    } catch {
      // stockage indisponible (navigation privée...) : le thème reste actif pour la session en cours
    }
  }

  private lire(): Theme {
    try {
      const stocke = localStorage.getItem(CLE_STOCKAGE);
      if (stocke === 'clair' || stocke === 'sombre' || stocke === 'systeme') {
        return stocke;
      }
    } catch {
      // ignore
    }
    return 'systeme';
  }

  private appliquer(theme: Theme): void {
    if (theme === 'systeme') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.setAttribute('data-theme', theme);
    }
  }
}
