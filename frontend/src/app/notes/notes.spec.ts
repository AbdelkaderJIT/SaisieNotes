import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Notes } from './notes';

registerLocaleData(localeFr);

const GROUPE_A = { id: 5, nom: 'L2-A' };
const GROUPE_B = { id: 6, nom: 'L2-B' };
const EXAMEN = {
  id: 1, matiereId: 1, matiereCode: 'INF101', matiereLibelle: 'Algorithmique', session: 'DS1',
  date: '2025-11-10', cloturee: false, groupes: [GROUPE_A, GROUPE_B],
};

function note(id: number, nom: string, valeur: number, dateModification: string | null = null) {
  return {
    id, valeur, examenId: 1, matiereId: 1, matiereLibelle: 'Algorithmique', etudiantId: id,
    etudiantNumInscription: `202400${id}`, etudiantNom: nom, etudiantPrenom: 'Prénom', enseignantId: 1,
    dateSaisie: '2026-09-24T16:46:00.400948', dateModification,
  };
}

function etudiant(id: number, nom: string) {
  return { id, numInscription: `202400${id}`, nom, prenom: 'Prénom' };
}

describe('Notes', () => {
  let fixture: ComponentFixture<Notes>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Notes],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'fr' },   // comme dans app.config.ts
      ],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Notes);
    fixture.componentRef.setInput('id', '1');
    fixture.componentRef.setInput('groupeId', '5');
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function repondre(examen: object, notes: object[], etudiants: object[] = []): Promise<void> {
    backend.expectOne('/api/examens/1').flush(examen);
    backend.expectOne('/api/examens/1/groupes/5/notes').flush(notes);
    backend.expectOne('/api/examens/1/groupes/5/etudiants').flush(etudiants);
    return fixture.whenStable();
  }

  function bouton(texte: string): HTMLButtonElement | undefined {
    return Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === texte);
  }

  async function cliquer(texte: string): Promise<void> {
    const b = bouton(texte);
    if (!b) throw new Error(`Bouton introuvable : ${texte}`);
    b.click();
    await fixture.whenStable();
  }

  async function remplirValeur(valeur: string): Promise<void> {
    const champ = page.querySelector('#valeur') as HTMLInputElement;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  // ---- consultation ----

  it('affiche l\'examen, le groupe et les notes triées par nom', async () => {
    await repondre(EXAMEN, [note(2, 'Mansour', 12.75), note(1, 'Gharbi', 14.5, '2026-09-25T10:00:00')]);

    expect(page.querySelector('h1')?.textContent).toContain('Algorithmique');
    expect(page.querySelector('h1')?.textContent).toContain('L2-A');
    expect(page.textContent).toContain('2 notes saisies');
    expect(page.textContent).toContain('14,50');          // format français
    expect(page.textContent).toContain('12,75');

    const lignes = Array.from(page.querySelectorAll('tbody tr'));
    expect(lignes.length).toBe(2);
    expect(lignes[0].textContent).toContain('Gharbi');
    expect(lignes[1].textContent).toContain('Mansour');
  });

  it('affiche un tiret quand la note n\'a jamais été modifiée', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)]);

    const cellules = page.querySelectorAll('tbody tr td');
    expect(cellules[cellules.length - 2].textContent).toContain('—');   // avant-dernière : la dernière est le bouton
  });

  it('affiche un état vide quand le groupe n\'a aucun étudiant', async () => {
    await repondre(EXAMEN, []);

    expect(page.textContent).toContain('Aucun étudiant dans ce groupe');
    expect(page.querySelector('table')).toBeNull();
  });

  it('affiche aussi les étudiants sans note, avec un tiret au lieu d\'une valeur', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);

    const lignes = Array.from(page.querySelectorAll('tbody tr'));
    expect(lignes.length).toBe(2);

    const ligneMansour = lignes.find((l) => l.textContent?.includes('Mansour'))!;
    expect(ligneMansour.querySelector('.num')?.textContent?.trim()).toBe('—');
    expect(ligneMansour.querySelector('button')?.textContent?.trim()).toBe('Ajouter');   // pas encore de note

    expect(page.textContent).toContain('2 étudiants');
    expect(page.textContent).toContain('1 note saisie');
  });

  it('depuis la ligne d\'un étudiant sans note, saisit sa note sans liste déroulante', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);

    const ajouter = Array.from(page.querySelectorAll('tbody button'))
      .find((b) => b.textContent?.trim() === 'Ajouter') as HTMLButtonElement;
    ajouter.click();
    await fixture.whenStable();

    expect(page.querySelector('#etudiant')).toBeNull();          // étudiant fixé, pas de choix
    expect(page.querySelector('.etudiant-fixe')?.textContent).toContain('Mansour');

    await remplirValeur('11');

    const requete = backend.expectOne((r) => r.method === 'POST' && r.url === '/api/examens/1/groupes/5/notes');
    expect(requete.request.body).toEqual({ etudiantId: 2, valeur: 11 });
  });

  it('affiche le refus du serveur pour un examen qui n\'est pas le sien (403)', async () => {
    backend.expectOne('/api/examens/1').flush(
      { message: 'Cet examen (Algorithmique (DS1)) ne vous appartient pas' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('ne vous appartient pas');
    expect(page.querySelector('table')).toBeNull();
    expect(bouton('Ajouter une note')).toBeUndefined();
  });

  // ---- examen clôturé ----

  it('examen clôturé : badge visible, plus aucun bouton d\'action', async () => {
    await repondre({ ...EXAMEN, cloturee: true }, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);

    expect(page.querySelector('.badge')?.textContent).toContain('Clôturé');
    expect(bouton('Ajouter une note')).toBeUndefined();
    expect(bouton('Clôturer l\'examen')).toBeUndefined();
    expect(bouton('Modifier')).toBeUndefined();
    expect(page.querySelectorAll('tbody button').length).toBe(0);
  });

  // ---- saisie ----

  it('la liste de saisie ne propose que les étudiants sans note (déjà filtrés par le serveur)', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);

    await cliquer('Ajouter une note');

    const options = Array.from(page.querySelectorAll('#etudiant option')).map((o) => o.textContent);
    expect(options.join('|')).toContain('Mansour');
    expect(options.join('|')).not.toContain('Gharbi');
  });

  it('désactive « Ajouter une note » quand tous les étudiants ont une note', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)], []);

    expect(bouton('Ajouter une note')?.disabled).toBe(true);
    expect(page.textContent).toContain('Tous vos étudiants ont déjà une note');
  });

  it('ajoute la note enregistrée au tableau, la retire de la liste de saisie, et confirme', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);
    await cliquer('Ajouter une note');

    const select = page.querySelector('#etudiant') as HTMLSelectElement;
    select.value = Array.from(select.options).find((o) => o.textContent?.includes('Mansour'))!.value;
    select.dispatchEvent(new Event('change'));
    await remplirValeur('12.75');

    const requete = backend.expectOne((r) => r.method === 'POST' && r.url === '/api/examens/1/groupes/5/notes');
    expect(requete.request.body).toEqual({ etudiantId: 2, valeur: 12.75 });
    requete.flush(note(2, 'Mansour', 12.75));
    await fixture.whenStable();

    expect(page.querySelectorAll('tbody tr').length).toBe(2);
    expect(page.textContent).toContain('12,75');
    expect(page.textContent).toContain('2 notes saisies');
    expect(page.querySelector('[role="status"]')?.textContent).toContain('enregistrée');
    expect(page.querySelector('form')).toBeNull();   // formulaire refermé

    // Mansour vient de recevoir une note : il ne doit plus être proposé dans la liste de saisie.
    await cliquer('Ajouter une note');
    const options = Array.from(page.querySelectorAll('#etudiant option')).map((o) => o.textContent);
    expect(options.join('|')).not.toContain('Mansour');
  });

  // ---- modification ----

  it('modifie une note existante', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)], []);
    await cliquer('Modifier');

    expect((page.querySelector('#valeur') as HTMLInputElement).value).toBe('14.5');
    await remplirValeur('16');

    const requete = backend.expectOne((r) => r.method === 'PUT' && r.url === '/api/notes/1');
    expect(requete.request.body).toEqual({ valeur: 16 });
    requete.flush(note(1, 'Gharbi', 16, '2026-09-25T10:00:00'));
    await fixture.whenStable();

    expect(page.querySelectorAll('tbody tr').length).toBe(1);
    expect(page.querySelector('tbody')?.textContent).toContain('16,00');
    expect(page.querySelector('tbody')?.textContent).toContain('25/09/2026');
    expect(page.querySelector('[role="status"]')?.textContent).toContain('modifiée');
  });

  // ---- clôture ----

  it('clôturer demande confirmation, puis verrouille l\'examen', async () => {
    await repondre(EXAMEN, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);

    await cliquer('Clôturer l\'examen');
    expect(page.textContent).toContain('Cette action est définitive');
    backend.expectNone((r) => r.method === 'POST');   // rien n'est envoyé avant confirmation

    await cliquer('Confirmer la clôture');
    backend.expectOne((r) => r.method === 'POST' && r.url === '/api/examens/1/cloturer')
      .flush({ ...EXAMEN, cloturee: true });
    await fixture.whenStable();

    expect(page.querySelector('.badge')?.textContent).toContain('Clôturé');
    expect(bouton('Ajouter une note')).toBeUndefined();
    expect(page.querySelectorAll('tbody button').length).toBe(0);
    expect(page.querySelector('[role="status"]')?.textContent).toContain('clôturé');
  });

  it('annuler la clôture ne change rien', async () => {
    await repondre(EXAMEN, [], [etudiant(2, 'Mansour')]);

    await cliquer('Clôturer l\'examen');
    await cliquer('Annuler');

    backend.expectNone((r) => r.method === 'POST');
    expect(page.textContent).not.toContain('Cette action est définitive');
    expect(bouton('Ajouter une note')).toBeDefined();
  });

  it('affiche l\'erreur du serveur si la clôture échoue', async () => {
    await repondre(EXAMEN, [], [etudiant(2, 'Mansour')]);

    await cliquer('Clôturer l\'examen');
    await cliquer('Confirmer la clôture');
    backend.expectOne((r) => r.method === 'POST' && r.url === '/api/examens/1/cloturer').flush(
      { message: 'Cet examen (Algorithmique (DS1)) ne vous appartient pas' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('ne vous appartient pas');
    expect(page.querySelector('.badge')).toBeNull();
  });
});
