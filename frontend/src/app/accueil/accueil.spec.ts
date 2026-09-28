import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Accueil } from './accueil';

describe('Accueil', () => {
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Accueil],
      providers: [provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(Accueil);
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
});
