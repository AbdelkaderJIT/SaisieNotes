import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Etudiant, Note } from '../core/models';
import { NoteFormulaire } from './note-formulaire';

const ETUDIANTS: Etudiant[] = [
  { id: 2, numInscription: '2024002', nom: 'Mansour', prenom: 'Nour' },
  { id: 3, numInscription: '2024003', nom: 'Jlassi', prenom: 'Yassine' },
];

const NOTE: Note = {
  id: 7, valeur: 14.5, matiereId: 1, etudiantId: 2, etudiantNumInscription: '2024002',
  etudiantNom: 'Mansour', etudiantPrenom: 'Nour', enseignantId: 1,
  dateSaisie: '2026-09-24T16:46:00', dateModification: null,
};

describe('NoteFormulaire', () => {
  let fixture: ComponentFixture<NoteFormulaire>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoteFormulaire],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(NoteFormulaire);
    fixture.componentRef.setInput('matiereId', 1);
    fixture.componentRef.setInput('etudiants', ETUDIANTS);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  async function remplir(etudiantNom: string | null, valeur: string): Promise<void> {
    if (etudiantNom) {
      const select = page.querySelector('#etudiant') as HTMLSelectElement;
      select.value = Array.from(select.options).find((o) => o.textContent?.includes(etudiantNom))!.value;
      select.dispatchEvent(new Event('change'));
    }
    const champ = page.querySelector('#valeur') as HTMLInputElement;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function envoyer(): Promise<void> {
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('saisie : un formulaire vide n\'appelle pas le serveur et explique quoi corriger', async () => {
    await envoyer();

    backend.expectNone(() => true);
    expect(page.textContent).toContain('Choisissez un étudiant');
    expect(page.textContent).toContain('La note est obligatoire');
  });

  it('saisie : refuse une note hors de 0..20 sans appeler le serveur', async () => {
    await remplir('Mansour', '21');
    await envoyer();

    backend.expectNone(() => true);
    expect(page.textContent).toContain('comprise entre 0 et 20');
  });

  it('saisie : refuse plus de 2 décimales sans appeler le serveur', async () => {
    await remplir('Mansour', '12.555');
    await envoyer();

    backend.expectNone(() => true);
    expect(page.textContent).toContain('au plus 2 décimales');
  });

  it('saisie : envoie étudiant et note, puis émet la note créée', async () => {
    const enregistre = vi.fn();
    fixture.componentInstance.enregistre.subscribe(enregistre);

    await remplir('Jlassi', '11.5');
    await envoyer();

    const requete = backend.expectOne((r) => r.method === 'POST' && r.url === '/api/matieres/1/notes');
    expect(requete.request.body).toEqual({ etudiantId: 3, valeur: 11.5 });
    requete.flush({ ...NOTE, id: 8, etudiantId: 3, valeur: 11.5 });

    expect(enregistre).toHaveBeenCalledWith(expect.objectContaining({ id: 8, valeur: 11.5 }));
  });

  it('saisie : accepte les bornes 0 et 20', async () => {
    await remplir('Mansour', '20');
    await envoyer();

    const requete = backend.expectOne((r) => r.method === 'POST');
    expect(requete.request.body.valeur).toBe(20);
    requete.flush(NOTE);
  });

  it('affiche sous le champ l\'erreur de validation renvoyée par le serveur (400)', async () => {
    await remplir('Mansour', '12');
    await envoyer();

    backend.expectOne((r) => r.method === 'POST').flush(
      { status: 400, message: 'Données invalides', champs: { valeur: 'La note doit être inférieure ou égale à 20' } },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(page.querySelector('.champ-erreur')?.textContent).toContain('inférieure ou égale à 20');
    expect(page.querySelector('[role="alert"]')).toBeNull();
  });

  it('affiche en message global une règle métier refusée (409)', async () => {
    await remplir('Mansour', '12');
    await envoyer();

    backend.expectOne((r) => r.method === 'POST').flush(
      { status: 409, message: 'Une note existe déjà pour Nour Mansour en « Algorithmique »' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Une note existe déjà');
  });

  it('modification : préremplit la note, cache la liste d\'étudiants et envoie un PUT', async () => {
    const enregistre = vi.fn();
    fixture.componentInstance.enregistre.subscribe(enregistre);
    fixture.componentRef.setInput('note', NOTE);
    await fixture.whenStable();

    expect(page.querySelector('#etudiant')).toBeNull();
    expect(page.textContent).toContain('Nour Mansour');
    expect((page.querySelector('#valeur') as HTMLInputElement).value).toBe('14.5');

    await remplir(null, '15.25');
    await envoyer();

    const requete = backend.expectOne((r) => r.method === 'PUT' && r.url === '/api/notes/7');
    expect(requete.request.body).toEqual({ valeur: 15.25 });
    requete.flush({ ...NOTE, valeur: 15.25, dateModification: '2026-09-25T10:00:00' });

    expect(enregistre).toHaveBeenCalledWith(expect.objectContaining({ valeur: 15.25 }));
  });

  it('Annuler émet l\'événement sans appeler le serveur', async () => {
    const annule = vi.fn();
    fixture.componentInstance.annule.subscribe(annule);

    (Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Annuler') as HTMLButtonElement).click();

    expect(annule).toHaveBeenCalled();
  });
});
