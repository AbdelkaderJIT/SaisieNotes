import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

const ALI = { id: 1, nom: 'Ben Ali', prenom: 'Ali', email: 'ali.benali@fds.tn' };
const BASIC_ALI = 'Basic ' + btoa('ali.benali@fds.tn:prof1');

describe('AuthService + authInterceptor', () => {
  let auth: AuthService;
  let http: HttpClient;
  let backend: HttpTestingController;
  let naviguer: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    naviguer = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  function seConnecter(): void {
    auth.login('ali.benali@fds.tn', 'prof1').subscribe();
    backend.expectOne('/api/me').flush(ALI);
  }

  it('un login réussi envoie HTTP Basic et mémorise l\'utilisateur', () => {
    auth.login('ali.benali@fds.tn', 'prof1').subscribe();

    const requete = backend.expectOne('/api/me');
    expect(requete.request.headers.get('Authorization')).toBe(BASIC_ALI);
    requete.flush(ALI);

    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.user()?.prenom).toBe('Ali');
  });

  it('un login refusé (401) ne connecte pas et ne mémorise rien', () => {
    let statut = 0;
    auth.login('ali.benali@fds.tn', 'faux').subscribe({ error: (e) => (statut = e.status) });

    backend.expectOne('/api/me').flush('', { status: 401, statusText: 'Unauthorized' });

    expect(statut).toBe(401);
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.authorizationHeader()).toBeNull();
  });

  it('accepte les mots de passe avec accents', () => {
    auth.login('ali.benali@fds.tn', 'mötdepasse').subscribe();
    const requete = backend.expectOne('/api/me');
    expect(requete.request.headers.get('Authorization')).toMatch(/^Basic [A-Za-z0-9+/=]+$/);
    requete.flush(ALI);
  });

  it('signe les appels /api une fois connecté', () => {
    seConnecter();

    http.get('/api/matieres').subscribe();

    const requete = backend.expectOne('/api/matieres');
    expect(requete.request.headers.get('Authorization')).toBe(BASIC_ALI);
    requete.flush([]);
  });

  it('ne signe pas les appels hors /api', () => {
    seConnecter();

    http.get('/assets/logo.svg').subscribe();

    const requete = backend.expectOne('/assets/logo.svg');
    expect(requete.request.headers.has('Authorization')).toBe(false);
    requete.flush('');
  });

  it('ne signe rien tant qu\'on n\'est pas connecté', () => {
    http.get('/api/matieres').subscribe({ error: () => undefined });

    const requete = backend.expectOne('/api/matieres');
    expect(requete.request.headers.has('Authorization')).toBe(false);
    requete.flush('', { status: 401, statusText: 'Unauthorized' });
  });

  it('un 401 en cours de session déconnecte et renvoie vers /login', () => {
    seConnecter();

    http.get('/api/matieres').subscribe({ error: () => undefined });
    backend.expectOne('/api/matieres').flush('', { status: 401, statusText: 'Unauthorized' });

    expect(auth.isLoggedIn()).toBe(false);
    expect(naviguer).toHaveBeenCalledWith('/login');
  });

  it('logout efface l\'utilisateur et les identifiants', () => {
    seConnecter();

    auth.logout();

    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.authorizationHeader()).toBeNull();
    expect(naviguer).toHaveBeenCalledWith('/login');
  });
});
