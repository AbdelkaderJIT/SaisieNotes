import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Examens } from './examens';

registerLocaleData(localeFr);

const MATIERE_ALGO = { id: 1, code: 'INF101', libelle: 'Algorithmique', semestre: 1 };
const MATIERE_BD = { id: 2, code: 'INF102', libelle: 'Bases de données', semestre: 1 };
const GROUPE_A = { id: 5, nom: 'L2-A' };
const GROUPE_B = { id: 6, nom: 'L2-B' };

function examen(
  id: number,
  matiere: typeof MATIERE_ALGO,
  session: string,
  groupes: typeof GROUPE_A[],
  cloturee = false,
) {
  return {
    id, matiereId: matiere.id, matiereCode: matiere.code, matiereLibelle: matiere.libelle,
    session, date: '2025-11-10', cloturee, groupes,
  };
}

describe('Examens', () => {
  let fixture: ComponentFixture<Examens>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Examens],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'fr' },
      ],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Examens);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function repondre(matieres: object[], groupes: object[], examens: object[]): Promise<void> {
    backend.expectOne('/api/matieres').flush(matieres);
    backend.expectOne('/api/groupes').flush(groupes);
    backend.expectOne('/api/examens').flush(examens);
    return fixture.whenStable();
  }

  it('affiche un message de chargement puis les examens', async () => {
    expect(page.textContent).toContain('Chargement');

    await repondre(
      [MATIERE_ALGO],
      [GROUPE_A],
      [examen(1, MATIERE_ALGO, 'DS1', [GROUPE_A])],
    );

    const lignes = Array.from(page.querySelectorAll('tbody tr'));
    expect(lignes.length).toBe(1);
    expect(lignes[0].textContent).toContain('Algorithmique');
    expect(lignes[0].querySelector('a')?.getAttribute('href')).toBe('/examens/1/groupes');
    expect(page.textContent).not.toContain('Chargement');
  });

  it('signale les examens clôturés', async () => {
    await repondre([MATIERE_ALGO], [GROUPE_A], [examen(1, MATIERE_ALGO, 'DS1', [GROUPE_A], true)]);

    expect(page.querySelector('.badge')?.textContent).toContain('Clôturé');
  });

  it('affiche un message quand aucun examen n\'est disponible', async () => {
    await repondre([], [], []);

    expect(page.textContent).toContain('Aucun examen');
  });

  it('filtre la liste par matière', async () => {
    await repondre(
      [MATIERE_ALGO, MATIERE_BD],
      [GROUPE_A],
      [examen(1, MATIERE_ALGO, 'DS1', [GROUPE_A]), examen(2, MATIERE_BD, 'DS1', [GROUPE_A])],
    );
    expect(page.querySelectorAll('tbody tr').length).toBe(2);

    const select = page.querySelector('#filtre-matiere') as HTMLSelectElement;
    select.value = String(MATIERE_BD.id);
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    const lignes = page.querySelectorAll('tbody tr');
    expect(lignes.length).toBe(1);
    expect(lignes[0].textContent).toContain('Bases de données');
  });

  it('filtre la liste par groupe', async () => {
    await repondre(
      [MATIERE_ALGO],
      [GROUPE_A, GROUPE_B],
      [examen(1, MATIERE_ALGO, 'DS1', [GROUPE_A]), examen(2, MATIERE_ALGO, 'DS2', [GROUPE_B])],
    );

    const select = page.querySelector('#filtre-groupe') as HTMLSelectElement;
    select.value = String(GROUPE_B.id);
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    const lignes = page.querySelectorAll('tbody tr');
    expect(lignes.length).toBe(1);
    expect(lignes[0].textContent).toContain('DS2');
  });

  it('filtre la liste par mot-clé, sur la matière, la session ou le groupe', async () => {
    await repondre(
      [MATIERE_ALGO, MATIERE_BD],
      [GROUPE_A, GROUPE_B],
      [examen(1, MATIERE_ALGO, 'DS1', [GROUPE_A]), examen(2, MATIERE_BD, 'Examen final', [GROUPE_B])],
    );
    expect(page.querySelectorAll('tbody tr').length).toBe(2);

    const champ = page.querySelector('#recherche') as HTMLInputElement;
    champ.value = 'final';
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    let lignes = page.querySelectorAll('tbody tr');
    expect(lignes.length).toBe(1);
    expect(lignes[0].textContent).toContain('Bases de données');

    champ.value = 'l2-a';
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    lignes = page.querySelectorAll('tbody tr');
    expect(lignes.length).toBe(1);
    expect(lignes[0].textContent).toContain('Algorithmique');
  });

  it('affiche le message du serveur en cas d\'erreur', async () => {
    backend.expectOne('/api/matieres').flush({ message: 'Enseignant introuvable' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Enseignant introuvable');
  });
});
