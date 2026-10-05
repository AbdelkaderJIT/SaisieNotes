import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Accueil } from './accueil';

describe('Accueil', () => {
  let fixture: ComponentFixture<Accueil>;
  let page: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');

    await TestBed.configureTestingModule({
      imports: [Accueil],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Accueil);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  it('affiche l\'identité de l\'établissement', () => {
    const texte = page.textContent ?? '';
    expect(texte).toContain('République Tunisienne');
    expect(texte).toContain('Ministère de l’Enseignement Supérieur et de la Recherche Scientifique');
    expect(texte).toContain('Université de Sfax');
    expect(texte).toContain('Faculté de Droit de Sfax');
  });

  it('propose un bouton de connexion vers /login', () => {
    const lien = page.querySelector('a.bouton') as HTMLAnchorElement;
    expect(lien.textContent?.trim()).toBe('Se connecter');
    expect(lien.getAttribute('href')).toBe('/login');
  });

  it('décrit l\'image des armoiries pour les lecteurs d\'écran', () => {
    expect(page.querySelector('img')?.getAttribute('alt')).toContain('Armoiries');
  });

  it('propose un choix de thème, appliqué immédiatement et mémorisé', async () => {
    const boutons = Array.from(page.querySelectorAll('.theme-switch button')) as HTMLButtonElement[];
    expect(boutons.map((b) => b.textContent?.trim())).toEqual(['Clair', 'Sombre']);
    expect(boutons.some((b) => b.classList.contains('actif'))).toBe(false);   // aucun choix explicite au départ

    boutons.find((b) => b.textContent?.trim() === 'Sombre')!.click();
    await fixture.whenStable();

    expect(document.documentElement.getAttribute('data-theme')).toBe('sombre');
    expect(localStorage.getItem('notedemo-theme')).toBe('sombre');
    expect(boutons.find((b) => b.textContent?.trim() === 'Sombre')?.classList.contains('actif')).toBe(true);
  });
});
