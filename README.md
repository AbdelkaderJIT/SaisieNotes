# NoteDemo – Saisie des notes

API REST (Spring Boot) permettant à un enseignant de **saisir**, **modifier** et **consulter** les notes des matières qui lui sont affectées, avec validation à deux niveaux. Un front Angular pourra s'y brancher ultérieurement.

## Périmètre

| Fonctionnalité | Endpoint |
|---|---|
| Mon profil (sert à valider le login côté Angular) | `GET /api/me` |
| Lister mes matières | `GET /api/matieres` |
| Détail d'une matière | `GET /api/matieres/{id}` |
| Mes étudiants dans une matière (liste déroulante de saisie) | `GET /api/matieres/{id}/etudiants` |
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

| Utilisateur | Mot de passe | Matières (id) |
|---|---|---|
| `ali.benali@fds.tn` | `prof1` | 10 matières : 1 DRT101, 2 DRT102 (partagée), 5 DRT104 (clôturée), 6 à 12 (DRT201 à DRT304) |
| `sonia.trabelsi@fds.tn` | `prof2` | 10 matières : 2 DRT102 (partagée), 3 DRT103 (clôturée), 4 LNG101, 13 à 19 (DRT105 à LNG201) |

Données de démonstration de la Faculté de Droit : 19 matières (droit des obligations, droit pénal, droit constitutionnel, droit administratif, procédure civile, etc.), 16 étudiants (ids 1 à 16), plus de 200 inscriptions et environ 70 notes. Seule une partie des étudiants a déjà une note, pour que la liste de saisie propose toujours quelqu'un. L'étudiant 5 n'est pas inscrit à la matière 1 (sert à tester le refus). Les ids des quatre premières matières, des six premiers étudiants et des quatre premières notes sont ceux utilisés par la collection Postman.

### Règle : un étudiant a un seul enseignant par matière
Plusieurs enseignants peuvent enseigner la même matière, mais **à des étudiants différents**. Chaque couple (étudiant, matière) est rattaché à un seul enseignant (entité `Inscription`, contrainte unique `(etudiant, matiere)`). Un enseignant ne voit, ne saisit et ne modifie que les notes de **ses** étudiants.

Exemple avec la matière 2 (DRT102, droit pénal général), partagée : Ali suit Gharbi, Mansour, Trabelsi et Chaabane ; Sonia suit Hamdi, Bouzid, Kefi, Ben Salah et Ayadi. Ali ne voit ni les étudiants ni les notes du groupe de Sonia, et inversement.

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
| L'étudiant est suivi par un autre enseignant dans cette matière | 403 | `NoteService` (via `Inscription`) |
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
- **Backend** : 28 tests unitaires sur `NoteService` (Mockito, sans base de données) : bornes 0 et 20, valeur négative ou absente, enseignant non affecté, étudiant suivi par un collègue, matière clôturée, étudiant non inscrit, doublon, modification, clôture. `NoteDemoApplicationTests` démarre tout le contexte et nécessite MySQL.
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

Écrans : connexion, liste des matières, et page d'une matière (tableau des notes, **ajout** et **modification** d'une note par formulaire, **clôture** avec confirmation). La liste de saisie ne propose que les étudiants de l'enseignant qui n'ont pas encore de note ; les boutons d'action disparaissent quand la matière est clôturée. Le formulaire valide le format côté navigateur (0 à 20, 2 décimales), puis affiche les erreurs renvoyées par l'API (champ invalide, matière clôturée, doublon...).

L'écran de connexion (`login/`) valide les identifiants via `GET /api/me`. Ils sont gardés **en mémoire** par `AuthService` (jamais dans `localStorage`) et ajoutés à chaque appel `/api` par un intercepteur ; un rechargement de la page déconnecte, et un 401 renvoie vers `/login`. Le backend répond 401 **sans** en-tête `WWW-Authenticate`, sinon le navigateur ouvre sa propre fenêtre de connexion.

## Déploiement gratuit : Render (application) + Aiven (base MySQL)

**Principe.** Un seul conteneur Docker contient tout : Spring Boot sert l'API (`/api/...`) **et** l'application Angular (mêmes URLs relatives qu'en développement, donc aucune configuration CORS). La base MySQL est hébergée chez Aiven, hors Docker. Le `Dockerfile` (à la racine) compile Angular, puis Spring Boot avec Angular à l'intérieur du jar, puis produit une petite image Java.

```
Navigateur ──HTTPS──► Render : 1 conteneur (Spring Boot = API + pages Angular) ──JDBC/SSL──► Aiven MySQL
```

### Variables d'environnement

| Variable | Rôle | Valeur par défaut (local) |
|---|---|---|
| `DB_URL` | URL JDBC de la base | `jdbc:mysql://localhost:3306/notedemo?createDatabaseIfNotExist=true` |
| `DB_USER` | utilisateur MySQL | `root` |
| `DB_PASSWORD` | mot de passe MySQL | `root` |
| `DEMO_ALI_PASSWORD` | mot de passe du compte `ali.benali@fds.tn` | `prof1` |
| `DEMO_SONIA_PASSWORD` | mot de passe du compte `sonia.trabelsi@fds.tn` | `prof2` |
| `SHOW_SQL` | afficher le SQL dans les logs (désactivé dans l'image Docker) | `true` en local |
| `DB_POOL_SIZE` | taille du pool de connexions | `5` |
| `PORT` | port d'écoute (**fixé par Render**, ne pas le définir) | `8080` |

Aucun secret n'est dans le dépôt : en production, tout vient de ces variables.

### Étape 1 : la base sur Aiven
1. Créer un compte sur aiven.io, puis un service **MySQL** avec le plan **gratuit**, dans une région européenne.
2. Quand il est démarré, relever sur la page du service : **hôte**, **port**, **utilisateur** (`avnadmin`) et **mot de passe**. La base par défaut s'appelle `defaultdb`.
3. L'URL à utiliser : `jdbc:mysql://HOTE:PORT/defaultdb?sslMode=REQUIRED` (attention : Aiven affiche `ssl-mode`, mais le pilote Java attend `sslMode`).
4. Aiven impose une clé primaire sur chaque table : c'est le cas de toutes les tables de l'application (y compris la table `enseigne`, grâce aux `Set` JPA).

### Étape 2 : le dépôt GitHub
Pousser le projet sur le dépôt (public). Vérifier qu'aucun mot de passe de production n'est commité.

### Étape 3 : le service sur Render
Deux façons, au choix :
- **Avec le fichier `render.yaml` (plus rapide)** : **New > Blueprint**, choisir le dépôt. Le service (Docker, plan gratuit) est préconfiguré ; Render demande seulement `DB_URL` et `DB_PASSWORD`, qui ne sont volontairement pas dans le dépôt public. Vérifier la région proposée (`frankfurt`) : elle doit être proche de celle d'Aiven.
- **À la main**, comme ci-dessous.

1. Créer un compte sur render.com avec GitHub, puis **New > Web Service** et choisir le dépôt. Render détecte le `Dockerfile`.
2. Type d'instance : **Free**. Région : la plus proche de celle d'Aiven (par exemple Francfort).
3. Ajouter les variables d'environnement : `DB_URL`, `DB_USER`, `DB_PASSWORD` (et, si on veut d'autres mots de passe de démonstration, `DEMO_ALI_PASSWORD` et `DEMO_SONIA_PASSWORD`).
4. Lancer le déploiement. Le premier build prend plusieurs minutes (compilation d'Angular puis de Java). L'adresse ressemble à `https://nom-du-service.onrender.com`.
5. Au premier démarrage, Hibernate crée les tables et `DataInitializer` insère les données de démonstration.

En cas d'échec, lire l'onglet **Logs** de Render. Causes fréquentes : paramètre SSL de l'URL (`sslMode=REQUIRED`), mémoire insuffisante, mauvais identifiants.

### Limites du plan gratuit (à connaître avant de montrer l'application)
- **Render endort le service après 15 minutes sans visite** ; le réveil prend environ une minute ou plus (Spring Boot sur un petit processeur). Ouvrir l'adresse quelques minutes avant une démonstration.
- **Aiven peut éteindre une base gratuite inactive** (avec un e-mail de prévenance) ; on la rallume depuis la console Aiven, puis on redémarre le service Render.
- Les vérifications sont faites sur des offres qui changent souvent : relire les pages des tarifs avant de compter dessus.

### Remettre les données de démonstration à zéro
Fermer une matière est **définitif**. Pour repartir des données de départ :
1. Se connecter à la base Aiven avec MySQL Workbench (hôte, port, `avnadmin`, mot de passe, SSL activé).
2. Exécuter `docs/reset-demo.sql` (supprime les tables de l'application).
3. Sur Render : **Manual Deploy > Restart service**. Les tables sont recréées et les données réinsérées.

### À essayer pendant la démonstration
- Se connecter avec `ali.benali@fds.tn` puis `sonia.trabelsi@fds.tn` : chacun ne voit que ses matières ; dans la matière partagée DRT102, chacun ne voit que **ses** étudiants.
- Saisir une note, puis saisir `25` : le message d'erreur s'affiche.
- Modifier une note ; ouvrir une matière clôturée (DRT104 pour Ali, DRT103 pour Sonia) : aucune action possible.
- Essayer d'ouvrir par l'adresse la matière d'un collègue : accès refusé.

### Tester l'image Docker chez soi (facultatif, nécessite Docker Desktop)
```
docker build -t saisie-notes .
docker run -p 8080:8080 -e DB_URL="jdbc:mysql://host.docker.internal:3306/notedemo" -e DB_USER=root -e DB_PASSWORD=root saisie-notes
```
`host.docker.internal` désigne le MySQL installé sur le PC depuis l'intérieur du conteneur.

### Pour un déploiement sur un serveur (plus tard)
La même image fonctionne sur n'importe quel serveur avec Docker (par exemple avec un fichier `docker-compose.yml` ajoutant un conteneur MySQL et un reverse proxy comme nginx ou Caddy pour le HTTPS). HTTP Basic n'est acceptable que derrière **HTTPS** ; en production réelle, il faudrait remplacer les comptes de démonstration par des utilisateurs en base et des mots de passe hachés.

## Évolutions prévues
- Pour ne plus conserver le mot de passe côté navigateur, remplacer HTTP Basic par un jeton JWT (seule `SecurityConfig` et `AuthService` changent).
- Stocker les enseignants et leur mot de passe haché en base.
