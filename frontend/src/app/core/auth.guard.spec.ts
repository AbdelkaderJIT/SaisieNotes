import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('guards', () => {
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  function executer(guard: typeof authGuard) {
    return TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
  }

  function seConnecter(): void {
    auth.login('ali.benali@fds.tn', 'prof1').subscribe();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/me')
      .flush({ id: 1, nom: 'Ben Ali', prenom: 'Ali', email: 'ali.benali@fds.tn' });
  }

  it('authGuard : renvoie vers /login si non connecté', () => {
    const resultat = executer(authGuard) as UrlTree;
    expect(router.serializeUrl(resultat)).toBe('/login');
  });

  it('authGuard : laisse passer un utilisateur connecté', () => {
    seConnecter();
    expect(executer(authGuard)).toBe(true);
  });

  it('guestGuard : laisse voir /login à un visiteur non connecté', () => {
    expect(executer(guestGuard)).toBe(true);
  });

  it('guestGuard : renvoie un utilisateur déjà connecté vers /matieres', () => {
    seConnecter();
    const resultat = executer(guestGuard) as UrlTree;
    expect(router.serializeUrl(resultat)).toBe('/matieres');
  });
});
