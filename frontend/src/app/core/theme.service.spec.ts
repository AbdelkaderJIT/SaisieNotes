import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    TestBed.configureTestingModule({});
  });

  it('démarre sur "systeme" sans préférence enregistrée, sans attribut sur <html>', () => {
    const theme = TestBed.inject(ThemeService);
    expect(theme.theme()).toBe('systeme');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
  });

  it('choisir("sombre") pose data-theme et le mémorise', () => {
    const theme = TestBed.inject(ThemeService);
    theme.choisir('sombre');

    expect(theme.theme()).toBe('sombre');
    expect(document.documentElement.getAttribute('data-theme')).toBe('sombre');
    expect(localStorage.getItem('notedemo-theme')).toBe('sombre');
  });

  it('choisir("systeme") retire l\'attribut', () => {
    const theme = TestBed.inject(ThemeService);
    theme.choisir('clair');
    theme.choisir('systeme');

    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
  });

  it('relit la préférence mémorisée au démarrage', () => {
    localStorage.setItem('notedemo-theme', 'clair');

    const theme = TestBed.inject(ThemeService);

    expect(theme.theme()).toBe('clair');
    expect(document.documentElement.getAttribute('data-theme')).toBe('clair');
  });
});
