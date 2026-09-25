import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let naviguer: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
    naviguer = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(Login);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  async function remplirEtEnvoyer(email: string, motDePasse: string): Promise<void> {
    const champs = page.querySelectorAll('input');
    for (const [champ, valeur] of [[champs[0], email], [champs[1], motDePasse]] as const) {
      champ.value = valeur;
      champ.dispatchEvent(new Event('input'));
    }
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('n\'appelle pas le serveur si le formulaire est vide', async () => {
    await remplirEtEnvoyer('', '');

    backend.expectNone('/api/me');
    expect(page.textContent).toContain('Saisissez une adresse email valide');
    expect(page.textContent).toContain('Le mot de passe est obligatoire');
  });

  it('affiche une erreur si les identifiants sont refusés', async () => {
    await remplirEtEnvoyer('ali.benali@fds.tn', 'faux');

    backend.expectOne('/api/me').flush('', { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Email ou mot de passe incorrect');
    expect(naviguer).not.toHaveBeenCalled();
  });

  it('va sur /matieres après un login réussi', async () => {
    await remplirEtEnvoyer('ali.benali@fds.tn', 'prof1');

    backend.expectOne('/api/me').flush({ id: 1, nom: 'Ben Ali', prenom: 'Ali', email: 'ali.benali@fds.tn' });
    await fixture.whenStable();

    expect(naviguer).toHaveBeenCalledWith('/matieres', { replaceUrl: true });
  });
});
