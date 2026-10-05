import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Groupes } from './groupes';

registerLocaleData(localeFr);

const EXAMEN = {
  id: 1, matiereId: 1, matiereCode: 'INF101', matiereLibelle: 'Algorithmique', session: 'DS1',
  date: '2025-11-10', cloturee: false, groupes: [{ id: 5, nom: 'L2-A' }, { id: 6, nom: 'L2-B' }],
};

describe('Groupes', () => {
  let fixture: ComponentFixture<Groupes>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Groupes],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'fr' },
      ],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Groupes);
    fixture.componentRef.setInput('id', '1');
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  it('affiche l\'examen et ses groupes, chacun menant à ses notes', async () => {
    backend.expectOne('/api/examens/1').flush(EXAMEN);
    await fixture.whenStable();

    expect(page.querySelector('h1')?.textContent).toContain('Algorithmique');
    expect(page.querySelector('h1')?.textContent).toContain('DS1');

    const cartes = Array.from(page.querySelectorAll('.carte-groupe'));
    expect(cartes.map((c) => c.textContent?.trim())).toEqual(['L2-A', 'L2-B']);
    expect(cartes[0].getAttribute('href')).toBe('/examens/1/groupes/5/notes');
    expect(cartes[1].getAttribute('href')).toBe('/examens/1/groupes/6/notes');
  });

  it('signale un examen clôturé', async () => {
    backend.expectOne('/api/examens/1').flush({ ...EXAMEN, cloturee: true });
    await fixture.whenStable();

    expect(page.querySelector('.badge')?.textContent).toContain('Clôturé');
  });

  it('affiche le message du serveur en cas d\'erreur', async () => {
    backend.expectOne('/api/examens/1').flush(
      { message: 'Cet examen (Algorithmique (DS1)) ne vous appartient pas' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('ne vous appartient pas');
  });
});
