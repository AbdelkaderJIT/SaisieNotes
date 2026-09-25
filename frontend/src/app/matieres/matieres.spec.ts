import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Matieres } from './matieres';

describe('Matieres', () => {
  let fixture: ComponentFixture<Matieres>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Matieres],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Matieres);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  it('affiche un message de chargement puis les matières triées par code', async () => {
    expect(page.textContent).toContain('Chargement');

    backend.expectOne('/api/matieres').flush([
      { id: 2, code: 'INF102', libelle: 'Bases de données', semestre: 1, cloturee: false },
      { id: 1, code: 'INF101', libelle: 'Algorithmique', semestre: 1, cloturee: false },
    ]);
    await fixture.whenStable();

    const cartes = Array.from(page.querySelectorAll('.carte-matiere'));
    expect(cartes.map((c) => c.querySelector('.code')?.textContent)).toEqual(['INF101', 'INF102']);
    expect(cartes[0].getAttribute('href')).toBe('/matieres/1/notes');
    expect(page.textContent).not.toContain('Chargement');
  });

  it('signale les matières clôturées', async () => {
    backend.expectOne('/api/matieres').flush([
      { id: 3, code: 'MAT101', libelle: 'Analyse', semestre: 1, cloturee: true },
      { id: 1, code: 'INF101', libelle: 'Algorithmique', semestre: 1, cloturee: false },
    ]);
    await fixture.whenStable();

    const badges = page.querySelectorAll('.badge');
    expect(badges.length).toBe(1);
    expect(badges[0].textContent).toContain('Clôturée');
  });

  it('affiche un message quand aucune matière n\'est affectée', async () => {
    backend.expectOne('/api/matieres').flush([]);
    await fixture.whenStable();

    expect(page.textContent).toContain('Aucune matière');
  });

  it('affiche le message du serveur en cas d\'erreur', async () => {
    backend.expectOne('/api/matieres').flush({ message: 'Enseignant introuvable' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Enseignant introuvable');
  });

  it('affiche un message clair si le serveur est injoignable', async () => {
    backend.expectOne('/api/matieres').error(new ProgressEvent('error'), { status: 0 });
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('injoignable');
  });
});
