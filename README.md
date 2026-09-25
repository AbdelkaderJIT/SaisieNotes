# NoteDemo – Saisie des notes

API REST (Spring Boot) permettant à un enseignant de **saisir**, **modifier** et **consulter** les notes des matières qui lui sont affectées, avec validation à deux niveaux. Un front Angular pourra s'y brancher ultérieurement.

## Périmètre

| Fonctionnalité | Endpoint |
|---|---|
| Mon profil (sert à valider le login côté Angular) | `GET /api/me` |
| Lister mes matières | `GET /api/matieres` |
| Détail d'une matière | `GET /api/matieres/{id}` |
| Étudiants inscrits à une matière (liste déroulante de saisie) | `GET /api/matieres/{id}/etudiants` |
| Consulter les notes d'une matière | `GET /api/matieres/{id}/notes` |
| Saisir une note | `POST /api/matieres/{id}/notes` |
| Modifier une note | `PUT /api/notes/{id}` |
| Clôturer une matière | `POST /api/matieres/{id}/cloturer` |

Sections du cahier des charges couvertes : **2.3** (un enseignant n'accède qu'aux matières qui lui sont affectées) et **Annexe 7** (programmation modulaire en couches). *À compléter avec les autres sections du cahier des charges.*

## Prérequis
- Java 17
- MySQL 8 démarré sur `localhost:3306`, utilisateur `root` / mot de passe `root` (modifiable dans `src/main/resources/application.properties`). La base `notedemo` est créée automatiquement.

## Lancer
```
.\mvnw.cmd spring-boot:run
```
Au premier démarrage, les tables sont créées par Hibernate et `DataInitializer` insère les données de démonstration (uniquement si la base est vide). L'application écoute sur le port 8080.

Pour repartir de données propres :
```
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=create"
```

## Comptes de démonstration (HTTP Basic)

| Utilisateur | Mot de passe | Matières affectées (id) |
|---|---|---|
| `ali.benali@fds.tn` | `prof1` | 1 INF101, 2 INF102 |
| `sonia.trabelsi@fds.tn` | `prof2` | 2 INF102, 3 MAT101 (clôturée), 4 ANG101 |

Étudiants 1 à 6. L'étudiant 5 n'est pas inscrit à la matière 1 (sert à tester le refus).

## Tester avec Postman
Importer `docs/postman/NoteDemo.postman_collection.json` et exécuter les requêtes dans l'ordre (le code HTTP attendu figure dans le nom de chaque requête). Les requêtes 3, 7 et 12 modifient les données.

Exemple de saisie :
```
POST /api/matieres/1/notes        (Basic ali.benali@fds.tn / prof1)
{ "etudiantId": 3, "valeur": 11.5 }
```

## Règles métier et codes HTTP

| Règle | Code | Où |
|---|---|---|
| Non authentifié | 401 | Spring Security |
| Format invalide (champ manquant, > 20, > 2 décimales, texte) | 400 | `NoteForm` (`@Valid`), messages dans `messages.properties` |
| L'enseignant n'est pas affecté à la matière | 403 | `NoteService` |
| Matière ou note introuvable | 404 | `NoteService` |
| Matière clôturée (saisie et modification interdites) | 409 | `NoteService` |
| Note déjà existante pour (étudiant, matière) | 409 | `NoteService` + contrainte unique en base |
| Étudiant non inscrit à la matière | 422 | `NoteService` |
| Valeur hors de [0, 20] (revérifiée côté service) | 422 | `NoteService` |

Toutes les erreurs ont le même corps JSON : `{ "status", "message", "champs"?, "timestamp" }`.

## Architecture

```
tn.espacenote.notedemo
├── model/        Entités JPA : Enseignant, Matiere, Etudiant, Note
├── repository/   Interfaces Spring Data
├── dto/          NoteForm, NoteModificationForm (entrées) ; NoteResponse, MatiereResponse, ErreurResponse (sorties)
├── service/      NoteService : toutes les règles métier
├── exception/    NoteException et une sous-classe par règle
├── controller/   NoteController (fin) et GlobalExceptionHandler (exception -> HTTP)
└── config/       SecurityConfig, DataInitializer
```

Choix de conception :
- **Contrôleur fin, règles dans le service** : le service n'a aucune dépendance web et peut être réutilisé par n'importe quelle interface.
- **DTO en entrée et en sortie** : le client ne peut pas imposer l'enseignant ni les dates (pas de *mass assignment*) et les relations lazy des entités ne sont jamais sérialisées.
- **Deux niveaux de validation** : format (Bean Validation) puis règles métier (service). La contrainte unique `(etudiant_id, matiere_id)` en base protège aussi contre deux saisies simultanées.
- **`BigDecimal(4,2)`** pour les notes : notes sur 20 avec décimales, sans erreur d'arrondi.
- **L'enseignant vient de l'utilisateur authentifié**, jamais d'une valeur envoyée par le client.
- **Authentification sans état** (HTTP Basic, pas de session) ; comptes en mémoire pour la démonstration, hors du modèle métier.

## Tests
```
.\mvnw.cmd test "-Dtest=NoteServiceTest"      # backend
cd frontend; npx ng test --watch=false         # frontend (Vitest)
```
- **Backend** : 24 tests unitaires sur `NoteService` (Mockito, sans base de données) : bornes 0 et 20, valeur négative ou absente, enseignant non affecté, matière clôturée, étudiant non inscrit, doublon, modification, clôture. `NoteDemoApplicationTests` démarre tout le contexte et nécessite MySQL.
- **Frontend** : 42 tests (authentification et gardes, liste des matières, page des notes, formulaire de saisie et de modification, clôture).

## Diagrammes (PlantUML, dossier `docs/`)
`class-diagram.puml`, `use-case-diagram.puml`, `sequence-saisir-note.puml`.

## Front Angular (`frontend/`)
Application Angular 22 (composants standalone) placée dans le même projet ; Maven l'ignore.

Prérequis : Node.js 24 LTS. Lancer, dans deux terminaux :
```
.\mvnw.cmd spring-boot:run          # backend, port 8080 (ou le bouton Run d'IntelliJ)
cd frontend
npm install                          # une seule fois
npx ng serve                         # front, http://localhost:4200
```
`frontend/proxy.conf.json` redirige `/api/*` vers `http://localhost:8080` : le navigateur ne parle qu'au port 4200, donc aucune configuration CORS n'est nécessaire en développement.

Écrans : connexion, liste des matières, et page d'une matière (tableau des notes, moyenne, **ajout** et **modification** d'une note par formulaire, **clôture** avec confirmation). La liste de saisie ne propose que les étudiants inscrits qui n'ont pas encore de note ; les boutons d'action disparaissent quand la matière est clôturée. Le formulaire valide le format côté navigateur (0 à 20, 2 décimales), puis affiche les erreurs renvoyées par l'API (champ invalide, matière clôturée, doublon...).

L'écran de connexion (`login/`) valide les identifiants via `GET /api/me`. Ils sont gardés **en mémoire** par `AuthService` (jamais dans `localStorage`) et ajoutés à chaque appel `/api` par un intercepteur ; un rechargement de la page déconnecte, et un 401 renvoie vers `/login`. Le backend répond 401 **sans** en-tête `WWW-Authenticate`, sinon le navigateur ouvre sa propre fenêtre de connexion.

## Déploiement (à prévoir)
- Le front appelle des URL **relatives** (`/api/...`) : en production, un reverse proxy (ex. nginx) sert `frontend/dist/frontend/browser` et transmet `/api` au backend, comme le proxy de développement. Aucune configuration CORS n'est nécessaire.
- HTTP Basic n'est acceptable que derrière **HTTPS**.
- Remplacer les identifiants `root`/`root` et les mots de passe de démonstration.

## Évolutions prévues
- Pour ne plus conserver le mot de passe côté navigateur, remplacer HTTP Basic par un jeton JWT (seule `SecurityConfig` et `AuthService` changent).
- Stocker les enseignants et leur mot de passe haché en base.
