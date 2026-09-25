import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Notes } from './notes';

registerLocaleData(localeFr);

const ALGO = { id: 1, code: 'INF101', libelle: 'Algorithmique', semestre: 1, cloturee: false };

function note(id: number, nom: string, valeur: number, dateModification: string | null = null) {
  return {
    id, valeur, matiereId: 1, etudiantId: id, etudiantNumInscription: `202400${id}`,
    etudiantNom: nom, etudiantPrenom: 'Prénom', enseignantId: 1,
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
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function repondre(matiere: object, notes: object[], etudiants: object[] = []): Promise<void> {
    backend.expectOne('/api/matieres/1').flush(matiere);
    backend.expectOne('/api/matieres/1/notes').flush(notes);
    backend.expectOne('/api/matieres/1/etudiants').flush(etudiants);
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

  it('affiche la matière, la moyenne et les notes triées par nom', async () => {
    await repondre(ALGO, [note(2, 'Mansour', 12.75), note(1, 'Gharbi', 14.5, '2026-09-25T10:00:00')]);

    expect(page.querySelector('h1')?.textContent).toContain('Algorithmique');
    expect(page.textContent).toContain('2 notes saisies');
    expect(page.textContent).toContain('13,63');   // (14,5 + 12,75) / 2, format français

    const lignes = Array.from(page.querySelectorAll('tbody tr'));
    expect(lignes.length).toBe(2);
    expect(lignes[0].textContent).toContain('Gharbi');
    expect(lignes[1].textContent).toContain('Mansour');
  });

  it('affiche un tiret quand la note n\'a jamais été modifiée', async () => {
    await repondre(ALGO, [note(1, 'Gharbi', 14.5)]);

    const cellules = page.querySelectorAll('tbody tr td');
    expect(cellules[cellules.length - 2].textContent).toContain('—');   // avant-dernière : la dernière est le bouton
  });

  it('affiche un état vide quand il n\'y a aucune note', async () => {
    await repondre(ALGO, []);

    expect(page.textContent).toContain('Aucune note saisie');
    expect(page.querySelector('table')).toBeNull();
  });

  it('affiche le refus du serveur pour une matière non affectée (403)', async () => {
    backend.expectOne('/api/matieres/1').flush(
      { message: 'Vous n\'êtes pas affecté à la matière « Algorithmique »' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('pas affecté');
    expect(page.querySelector('table')).toBeNull();
    expect(bouton('Ajouter une note')).toBeUndefined();
  });

  // ---- matière clôturée ----

  it('matière clôturée : badge visible, plus aucun bouton d\'action', async () => {
    await repondre({ ...ALGO, cloturee: true }, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);

    expect(page.querySelector('.badge')?.textContent).toContain('Clôturée');
    expect(bouton('Ajouter une note')).toBeUndefined();
    expect(bouton('Clôturer la matière')).toBeUndefined();
    expect(bouton('Modifier')).toBeUndefined();
    expect(page.querySelectorAll('tbody button').length).toBe(0);
  });

  // ---- saisie ----

  it('la liste de saisie ne propose que les étudiants sans note', async () => {
    await repondre(ALGO, [note(1, 'Gharbi', 14.5)], [etudiant(1, 'Gharbi'), etudiant(2, 'Mansour')]);

    await cliquer('Ajouter une note');

    const options = Array.from(page.querySelectorAll('#etudiant option')).map((o) => o.textContent);
    expect(options.join('|')).toContain('Mansour');
    expect(options.join('|')).not.toContain('Gharbi');
  });

  it('désactive « Ajouter une note » quand tous les étudiants ont une note', async () => {
    await repondre(ALGO, [note(1, 'Gharbi', 14.5)], [etudiant(1, 'Gharbi')]);

    expect(bouton('Ajouter une note')?.disabled).toBe(true);
    expect(page.textContent).toContain('Tous les étudiants inscrits ont déjà une note');
  });

  it('ajoute la note enregistrée au tableau et confirme', async () => {
    await repondre(ALGO, [note(1, 'Gharbi', 14.5)], [etudiant(1, 'Gharbi'), etudiant(2, 'Mansour')]);
    await cliquer('Ajouter une note');

    const select = page.querySelector('#etudiant') as HTMLSelectElement;
    select.value = Array.from(select.options).find((o) => o.textContent?.includes('Mansour'))!.value;
    select.dispatchEvent(new Event('change'));
    await remplirValeur('12.75');

    const requete = backend.expectOne((r) => r.method === 'POST' && r.url === '/api/matieres/1/notes');
    expect(requete.request.body).toEqual({ etudiantId: 2, valeur: 12.75 });
    requete.flush(note(2, 'Mansour', 12.75));
    await fixture.whenStable();

    expect(page.querySelectorAll('tbody tr').length).toBe(2);
    expect(page.textContent).toContain('12,75');
    expect(page.textContent).toContain('2 notes saisies');
    expect(page.querySelector('[role="status"]')?.textContent).toContain('enregistrée');
    expect(page.querySelector('form')).toBeNull();   // formulaire refermé
  });

  // ---- modification ----

  it('modifie une note existante', async () => {
    await repondre(ALGO, [note(1, 'Gharbi', 14.5)], [etudiant(1, 'Gharbi')]);
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

  it('clôturer demande confirmation, puis verrouille la matière', async () => {
    await repondre(ALGO, [note(1, 'Gharbi', 14.5)], [etudiant(2, 'Mansour')]);

    await cliquer('Clôturer la matière');
    expect(page.textContent).toContain('Cette action est définitive');
    backend.expectNone((r) => r.method === 'POST');   // rien n'est envoyé avant confirmation

    await cliquer('Confirmer la clôture');
    backend.expectOne((r) => r.method === 'POST' && r.url === '/api/matieres/1/cloturer')
      .flush({ ...ALGO, cloturee: true });
    await fixture.whenStable();

    expect(page.querySelector('.badge')?.textContent).toContain('Clôturée');
    expect(bouton('Ajouter une note')).toBeUndefined();
    expect(page.querySelectorAll('tbody button').length).toBe(0);
    expect(page.querySelector('[role="status"]')?.textContent).toContain('clôturée');
  });

  it('annuler la clôture ne change rien', async () => {
    await repondre(ALGO, [], [etudiant(2, 'Mansour')]);

    await cliquer('Clôturer la matière');
    await cliquer('Annuler');

    backend.expectNone((r) => r.method === 'POST');
    expect(page.textContent).not.toContain('Cette action est définitive');
    expect(bouton('Ajouter une note')).toBeDefined();
  });

  it('affiche l\'erreur du serveur si la clôture échoue', async () => {
    await repondre(ALGO, [], [etudiant(2, 'Mansour')]);

    await cliquer('Clôturer la matière');
    await cliquer('Confirmer la clôture');
    backend.expectOne((r) => r.method === 'POST' && r.url === '/api/matieres/1/cloturer').flush(
      { message: 'Vous n\'êtes pas affecté à la matière « Algorithmique »' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('pas affecté');
    expect(page.querySelector('.badge')).toBeNull();
  });
});
