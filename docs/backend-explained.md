# The backend and the frontend, explained line by line

This document explains every file of the application: what each line does, which framework feature it uses, and **why it was written that way**. **Part 1** covers the backend (Spring Boot); **Part 2** covers the frontend (Angular). Code identifiers stay in French (they come from the specification); the explanations are in English.

**How to read it.** Each file has (1) its code, (2) a line-by-line commentary, (3) a "Why" section for the design decisions. Sections follow the order a request travels through the application.

## Contents

### Part 1 — Backend (Spring Boot)

0. [The big picture: the journey of one request](#0-the-big-picture)
1. [Project setup: `pom.xml`, `application.properties`, the main class](#1-project-setup)
2. [Lombok in one page](#2-lombok)
3. [Model layer: the JPA entities](#3-model-layer)
4. [Repository layer](#4-repository-layer)
5. [DTOs, Bean Validation, and `messages.properties`](#5-dtos-and-validation)
6. [The exception classes](#6-exceptions)
7. [The service: where the business rules live](#7-the-service)
8. [The controller](#8-the-controller)
9. [The global exception handler](#9-the-global-exception-handler)
10. [Security](#10-security)
11. [Seed data: `DataInitializer`](#11-seed-data)
12. [Unit tests](#12-unit-tests)
13. [Design decisions recap, known limitations, likely interview questions](#13-recap)

### Part 2 — Frontend (Angular)

14. [The frontend's big picture](#14-the-frontends-big-picture)
15. [Project setup and tooling](#15-project-setup-and-tooling)
16. [The Angular concepts used here](#16-angular-concepts)
17. [Bootstrapping, configuration and routing](#17-bootstrapping-configuration-and-routing)
18. [The `core/` folder: models, errors, API service](#18-core)
19. [Authentication: service, interceptor, guards](#19-authentication)
20. [The application shell](#20-the-application-shell)
21. [The home page (`accueil`)](#21-the-home-page)
22. [The login page](#22-the-login-page)
23. [The matières page](#23-the-matieres-page)
24. [The notes page](#24-the-notes-page)
25. [The note form component](#25-the-note-form-component)
26. [Styling](#26-styling)
27. [Frontend tests](#27-frontend-tests)
28. [Frontend ↔ backend contract, recap, limitations, interview questions](#28-recap-of-the-frontend)

---

# PART 1 — BACKEND

---

## 0. The big picture

The app is built in **layers**. Each layer has one job and only talks to the layer below it:

```
Client (Postman / Angular)
   │  HTTP + JSON
   ▼
Spring Security filter chain     config/SecurityConfig        "Who are you?"  (401 if unknown)
   ▼
Controller                       controller/NoteController    "Which URL? Is the JSON well-formed?"
   ▼
Service                          service/NoteService          "Is this allowed? (business rules)"
   ▼
Repository                       repository/*                 "Read / write the database"
   ▼
Entities (model)                 model/*                      Java objects mapped to MySQL tables
   ▼
MySQL
```

And the reverse path for errors: any rule violation is a Java **exception** thrown in the service; `GlobalExceptionHandler` catches it and turns it into an HTTP status + JSON body.

**One request, step by step** — `POST /api/matieres/1/notes` with body `{"etudiantId": 3, "valeur": 11.5}`:

1. Tomcat (embedded web server) receives the HTTP request.
2. The **security filter chain** reads the `Authorization: Basic ...` header, checks the password, and stores an `Authentication` object for this request. No/wrong credentials → `401`, and the request stops here.
3. Spring MVC finds the controller method whose mapping matches (`@PostMapping("/matieres/{matiereId}/notes")` under `/api`).
4. Spring converts the JSON body into a `NoteForm` object and runs **Bean Validation** (`@Valid`). Bad format → `400`.
5. The controller calls `noteService.saisir(...)`.
6. The service opens a **transaction**, loads entities through repositories, checks the five business rules, saves the `Note`, and returns a `NoteResponse`. A violated rule throws an exception → transaction rolled back.
7. Spring converts the returned `NoteResponse` into JSON, status `201`.
8. If an exception was thrown anywhere, `GlobalExceptionHandler` produces the error response instead.

---

## 1. Project setup

### 1.1 `pom.xml`

Maven's project file. It declares **what libraries the project needs** and how to build it.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```
- **`spring-boot-starter-parent`** is a special parent POM. It gives us: (a) the *dependency management* (a curated list of library versions that work together, so we never write versions for Spring libraries), (b) sensible plugin defaults (Java compiler settings, including `-parameters`, which lets Spring read Java parameter names such as `matiereId` in `@PathVariable Long matiereId`), (c) the packaging plugin.
- `4.1.1` is the Spring Boot version; it decides the versions of Spring Framework, Hibernate, Tomcat, Jackson, Spring Security, etc.

```xml
<groupId>tn.espacenote</groupId>
<artifactId>NoteDemo</artifactId>
<version>0.0.1-SNAPSHOT</version>
```
Our project's identity. `groupId` matches the Java package root `tn.espacenote`. `SNAPSHOT` means "work in progress".

```xml
<properties><java.version>17</java.version></properties>
```
Tells the compiler to target Java 17. This is why we can use `record`, `Stream.toList()`, and text features from Java 17, but **not** newer features such as pattern-matching `switch` (that's why `GlobalExceptionHandler` uses a `Map`).

**Dependencies** (each `spring-boot-starter-*` is a bundle of several libraries + auto-configuration):

| Dependency | What it brings | Where we use it |
|---|---|---|
| `spring-boot-starter-webmvc` | Spring MVC + embedded Tomcat + Jackson (JSON) | `@RestController`, JSON in/out |
| `spring-boot-starter-data-jpa` | JPA API + Hibernate + Spring Data JPA | entities, repositories, `@Transactional` |
| `spring-boot-starter-security` | Spring Security | `SecurityConfig`, login |
| `spring-boot-starter-validation` | Bean Validation (Jakarta Validation + Hibernate Validator) | `@NotNull`, `@DecimalMax`, `@Valid` |
| `mysql-connector-j` (`runtime`) | The JDBC driver for MySQL | needed to run, not to compile → `runtime` scope |
| `lombok` (`optional`) | Compile-time code generator | `@Getter`, `@RequiredArgsConstructor`… |
| `spring-boot-devtools` (`runtime`, `optional`) | Automatic restart on code change in the IDE | development only; `optional` keeps it out of the packaged app's dependencies |
| `*-test` starters (`test`) | JUnit 5, Mockito, Spring test support | tests only |

**The `maven-compiler-plugin` block** at the bottom sets `annotationProcessorPaths` to Lombok for both `default-compile` and `default-testCompile`. Lombok works by hooking into the compiler as an *annotation processor* that writes extra bytecode (getters, constructors…). Recent Maven/JDK versions no longer discover processors automatically from the classpath, so it must be listed explicitly.

### 1.2 `application.properties`

```properties
spring.application.name=NoteDemo
```
Just a name (appears in logs).

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/notedemo?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```
- The JDBC URL: `jdbc:mysql://host:port/databaseName`. Spring Boot reads these `spring.datasource.*` keys and creates a **connection pool** (HikariCP) automatically — we never write connection code.
- `createDatabaseIfNotExist=true` is a **MySQL driver parameter**: if the `notedemo` database doesn't exist, the driver creates it. Convenient for a demo.
- `driver-class-name` is redundant (Spring deduces it from the URL) but harmless and explicit.

```properties
spring.jpa.hibernate.ddl-auto=update
```
Hibernate compares the entities to the database at startup:
- `update`: **creates missing tables/columns**, never drops anything (what we use for development);
- `create`: drops and recreates all tables at each start (we used it to reset the demo data);
- `validate` / `none`: for production, where a migration tool (Flyway/Liquibase) manages the schema.

```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.show-sql=true
```
The *dialect* tells Hibernate which SQL flavour to generate (recent Hibernate versions detect it automatically, so this line is optional). `show-sql=true` prints every SQL statement to the console — extremely useful for **seeing what Hibernate actually does** (and for spotting extra queries).

### 1.3 `NoteDemoApplication.java`

```java
@SpringBootApplication
public class NoteDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NoteDemoApplication.class, args);
    }
}
```
- `main` is the normal Java entry point.
- `SpringApplication.run(...)` creates the **Spring application context** (the container holding all our "beans"), starts the embedded Tomcat on port 8080, and runs `CommandLineRunner`s such as `DataInitializer`.
- `@SpringBootApplication` is a shortcut for **three annotations**:
  - `@Configuration` — this class may define beans;
  - `@EnableAutoConfiguration` — Spring Boot looks at the classpath and configures things automatically (sees Hibernate + MySQL driver → sets up a DataSource and an EntityManager; sees Spring MVC → sets up the web layer; etc.);
  - `@ComponentScan` — scans **this package and all sub-packages** for classes annotated `@Component`, `@Service`, `@RestController`, `@Configuration`… and registers them. **This is why the main class must sit in the root package** `tn.espacenote.notedemo`: `model`, `service`, `controller`… are all below it.

---

## 2. Lombok

Lombok removes boilerplate by generating code at compile time. Annotations used in this project:

| Annotation | Generates | Why we need it |
|---|---|---|
| `@Getter` | a `getX()` for every field (`isX()` for `boolean`) | JPA and our code read fields; DTO mapping (`n.getValeur()`) |
| `@Setter` | a `setX(...)` for every field | used on `Enseignant`, `Matiere`, `Etudiant` (seed data), form DTOs (Jackson fills them) |
| `@NoArgsConstructor` | `public X() {}` | **JPA requires a no-arg constructor**: Hibernate creates entities by reflection, then fills the fields. Form DTOs also need it so Jackson can instantiate them |
| `@RequiredArgsConstructor` | a constructor taking every `final` field | **constructor injection** for Spring beans (see below) |
| `@Slf4j` | `private static final Logger log = ...` | logging in `DataInitializer` |

**We deliberately do not use `@Data`.** `@Data` also generates `equals`, `hashCode`, and `toString` from *all* fields. On entities with two-way relations (`Enseignant` ↔ `Matiere`) that causes infinite recursion (A's hashCode calls B's, which calls A's…) and accidental lazy loading. Explicit `@Getter`/`@Setter` avoids that.

**Constructor injection with `@RequiredArgsConstructor`:** for a class like

```java
@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    ...
}
```
Lombok generates `public NoteService(NoteRepository noteRepository, ...)`. When a class has a single constructor, Spring uses it and passes the matching beans in — no `@Autowired` needed. This is preferred over field injection because dependencies are `final` (can't be null or swapped), and the class can be tested by simply calling `new NoteService(mockA, mockB, ...)`.

---

## 3. Model layer

Entities are Java classes that **map to database tables**. JPA (Jakarta Persistence) is the standard; Hibernate is the implementation.

### The relationships (from the class diagram)

```
Enseignant ──< enseigne >── Matiere                      (many-to-many: which matières a teacher teaches)

Inscription ──► Etudiant      (many-to-one)              ┐  one row = "this student, in this matière,
Inscription ──► Matiere       (many-to-one)              │   is taught by this teacher"
Inscription ──► Enseignant    (many-to-one)              ┘   unique (etudiant, matiere)

Note ──► Etudiant      (many-to-one)
Note ──► Matiere       (many-to-one)
Note ──► Enseignant    (many-to-one)                      who entered it (traceability)
```
Tables created: `enseignant`, `matiere`, `etudiant`, `inscription`, `note`, plus the join table `enseigne`.

**The business rule behind `Inscription`:** several teachers can teach the *same* matière, but to *different* students. A student therefore has **exactly one teacher per matière**, and only that teacher may see, enter or edit the student's grade. An earlier version modelled enrollment as a plain student ↔ matière many-to-many, which lost the "who teaches this student" information: any teacher of a matière could see every student and edit every note. `Inscription` fixes that.

### 3.1 `Enseignant`

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Enseignant {
```
- `@Entity` marks the class as a table-backed entity (table name defaults to `enseignant`).

```java
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
```
- `@Id` = primary key.
- `@GeneratedValue(IDENTITY)` = the **database** generates the value (`AUTO_INCREMENT` in MySQL). The `id` is `null` until the row is inserted; after `save(...)` Hibernate fills it in.
- `Long` (not `long`): a not-yet-saved entity has *no* id, and `null` expresses that.

```java
    @Column(nullable = false)
    private String nom;
    @Column(nullable = false)
    private String prenom;
    @Column(nullable = false, unique = true)
    private String email;
```
- `@Column(nullable = false)` → `NOT NULL` in the DDL. It's a **database-level guarantee**, independent of any Java check.
- `unique = true` on `email` → a unique index. Two teachers can't share an email; important because the email is the **login identity** (`findByEmail`).

```java
    // "enseigne" : matières affectées par l'administration (section 2.3)
    @ManyToMany
    @JoinTable(name = "enseigne",
            joinColumns = @JoinColumn(name = "enseignant_id"),
            inverseJoinColumns = @JoinColumn(name = "matiere_id"))
    private Set<Matiere> matieres = new HashSet<>();
```
- `@ManyToMany`: one teacher teaches many matières; one matière can have many teachers.
- A many-to-many can't be stored in either table, so it needs a **join table**. `@JoinTable(name = "enseigne", ...)` names it and its two foreign-key columns: `joinColumns` = the column pointing to *this* entity (`enseignant_id`), `inverseJoinColumns` = the column pointing to the *other* entity (`matiere_id`).
- **`Enseignant` is the *owning side*** of this relationship: it defines the join table. Changes to `enseignant.getMatieres()` are what Hibernate writes to `enseigne`.
- `Set` rather than `List`: a teacher can't teach the same matière twice, so a set matches the meaning; and with Hibernate, a `List` (a "bag") on many-to-many makes Hibernate delete and re-insert **all** rows of the join table on any change, and can't be fetched together with a second bag. `Set` avoids both problems.
- `= new HashSet<>()` so the field is never `null`.

```java
    public boolean enseigne(Matiere matiere) {
        return matieres.contains(matiere);
    }
```
A tiny domain method that answers **"is this teacher assigned to this matière?"** — it implements section 2.3 of the spec. Putting it on the entity keeps the service readable: `enseignant.enseigne(matiere)`.
*Technical note:* `contains` uses `equals`. We didn't override `equals`, so it's identity-based. Inside one transaction (persistence context) Hibernate guarantees a single Java object per database row, so identity comparison is correct here. (See limitations, §13.)

### 3.2 `Matiere`

```java
    @Column(nullable = false, unique = true)
    private String code;         // e.g. INF101
    @Column(nullable = false)
    private String libelle;      // e.g. Algorithmique
    private int semestre;
```
- `code` is unique (business identifier). `semestre` is a primitive `int` → `NOT NULL` column automatically (a primitive can't be null).

```java
    // Une fois clôturée, aucune note ne peut être ajoutée ni modifiée
    private boolean cloturee = false;
```
The **closure flag**. Default `false`. Lombok generates `isCloturee()` (for `boolean` fields the getter is `isX`, not `getX`).

```java
    @ManyToMany(mappedBy = "matieres")
    private Set<Enseignant> enseignants = new HashSet<>();
```
- `mappedBy = "matieres"` marks the **inverse side**: "the relationship is defined by the field named `matieres` in the other entity (`Enseignant`); don't create another join table". The inverse side is a read-only *view* of the same table.
- Why have it at all? It lets us write the query `findByEnseignantsContaining(enseignant)` ("which matières does this teacher have?") directly on `Matiere`.

```java
    public void cloturer() { this.cloturee = true; }
```
A domain method instead of `setCloturee(false)`: the only transition allowed is **open → closed**; nothing can re-open a matière. (`@Setter` still exists on the class, which is a compromise for the seed data; the service only ever calls `cloturer()`.)

### 3.3 `Etudiant`

A plain entity: `numInscription` (unique — the student's registration number), `nom`, `prenom`, `filiere`, `niveau`. It has **no list of matières**: which matières a student follows, and with which teacher, is stored in `Inscription` (next section). Keeping that link out of `Etudiant` avoids a second way to say the same thing.

### 3.3b `Inscription` — "this student, in this matière, is taught by this teacher"

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_inscription_etudiant_matiere",
        columnNames = {"etudiant_id", "matiere_id"}))
@Getter
@NoArgsConstructor
public class Inscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "etudiant_id")   private Etudiant etudiant;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "matiere_id")    private Matiere matiere;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "enseignant_id") private Enseignant enseignant;

    public Inscription(Etudiant etudiant, Matiere matiere, Enseignant enseignant) {
        if (!enseignant.enseigne(matiere)) {
            throw new IllegalArgumentException("L'enseignant n'enseigne pas la matière « " + matiere.getLibelle() + " »");
        }
        ...
    }
}
```
- It's an **association entity**: a many-to-many between `Etudiant` and `Matiere` that carries an extra piece of data (the teacher). A plain `@ManyToMany` + `@JoinTable` can't hold extra columns, so the join becomes an entity of its own with three `@ManyToOne` links (same pattern as `Note`).
- **`unique (etudiant_id, matiere_id)`** is the heart of the rule: a student appears **once per matière**, hence with **one teacher**. The database enforces it even if the code has a bug.
- The **constructor checks an invariant**: a student can only be entrusted to a teacher who actually teaches that matière (`enseignant.enseigne(matiere)`, i.e. the `enseigne` assignments). You cannot build an inconsistent `Inscription`.
- No setters (only `@Getter`): once created, the assignment doesn't change; reassigning a student would be an explicit administrative operation (not part of this scope).

### 3.4 `Note` — the most important entity

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_note_etudiant_matiere",
        columnNames = {"etudiant_id", "matiere_id"}))
@Getter
@NoArgsConstructor
public class Note {
```
- `@Table(uniqueConstraints = ...)` creates a **composite unique constraint** on `(etudiant_id, matiere_id)`: the database itself refuses a second note for the same student and matière. This is the specification's `{unique (etudiant, matiere)}` rule enforced at the lowest level — even if someone bypasses the service, or two requests race.
- Only `@Getter`, **no `@Setter`**: a `Note` can only be changed through its constructor and `modifier(...)`, so its invariants can't be bypassed by accident.

```java
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal valeur;
```
- **`BigDecimal`, not `double`**: `double` is binary floating point and can't represent 12.75 or 0.1 exactly in every case; grades must be exact. `BigDecimal` is exact decimal arithmetic.
- `precision = 4, scale = 2` → SQL `DECIMAL(4,2)`: 4 digits in total, 2 after the decimal point → range `-99.99…99.99`, which comfortably holds `0.00…20.00`.

```java
    @Column(nullable = false)
    private LocalDateTime dateSaisie;
    private LocalDateTime dateModification;      // null until the first modification
```
`LocalDateTime` (modern `java.time`) maps to a SQL `DATETIME`. `dateModification` is nullable on purpose: `null` = "never modified" (the UI shows "—").

```java
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id")
    private Etudiant etudiant;
```
(the same for `matiere` and `enseignant`)
- `@ManyToOne`: many notes point to one student. The `note` table gets a **foreign-key column** `etudiant_id`.
- `optional = false` → the FK column is `NOT NULL`; a note **must** have a student, a matière, and an author. The `enseignant` link is the **traceability**: it records who entered the grade.
- `fetch = LAZY`: don't load the `Etudiant` row when loading a `Note`; Hibernate puts a lightweight **proxy** and loads the real row only when you call something on it. (`@ManyToOne` defaults to `EAGER`, which tends to trigger cascades of unnecessary queries, so we choose `LAZY` explicitly.) Consequence: navigating `note.getEtudiant().getNom()` must happen **inside a transaction/session** — this is why the service builds the DTOs, not the controller.

```java
    public Note(BigDecimal valeur, Etudiant etudiant, Matiere matiere, Enseignant enseignant) {
        this.valeur = valeur; ... 
        this.dateSaisie = LocalDateTime.now();
    }
```
The only way to create a note: all required data at once, and `dateSaisie` set automatically → you can't build an incomplete note.

```java
    public void modifier(BigDecimal v) {
        this.valeur = v;
        this.dateModification = LocalDateTime.now();
    }
```
The only way to change a note: the value **and** its modification timestamp always change together. (This method is on the UML class diagram.)

---

## 4. Repository layer

```java
public interface NoteRepository extends JpaRepository<Note, Long> {
    boolean existsByEtudiantAndMatiere(Etudiant etudiant, Matiere matiere);

    @Query("""
            select n from Note n join fetch n.etudiant
            where n.matiere = :matiere
              and exists (select 1 from Inscription i
                          where i.etudiant = n.etudiant
                            and i.matiere = n.matiere
                            and i.enseignant = :enseignant)
            """)
    List<Note> findVisiblesPar(@Param("matiere") Matiere matiere, @Param("enseignant") Enseignant enseignant);
}
```
- We write **only interfaces**. At startup Spring Data creates the implementation (a runtime proxy) for each.
- `JpaRepository<Note, Long>` = "a repository for `Note` entities whose id type is `Long`". It provides, for free: `save`, `findById` (returns `Optional`), `findAll`, `deleteById`, `count`, `existsById`, and more. Each of those runs inside a transaction.
- **Derived query methods**: Spring Data *parses the method name* and generates the query:
  - `existsByEtudiantAndMatiere(etudiant, matiere)` → `select ... from note where etudiant_id = ? and matiere_id = ?` returning a boolean.
  The words `findBy`, `existsBy`, `And`, `Containing`, `OrderBy…Asc` are Spring Data's *keywords*; the property names (`Etudiant`, `Matiere`) must match fields of the entity. A typo in a name makes the **application fail at startup** (not at runtime), which is a nice safety net.
- **`@Query` with JPQL** when a method name would be unreadable or the query needs a sub-query. JPQL is like SQL but written against **entities and fields** (`Note n`, `n.etudiant`), not tables and columns. `findVisiblesPar` reads: "the notes of this matière **whose student is assigned to this teacher** in an `Inscription`". The `exists (select 1 ...)` sub-query is what makes two teachers of the same matière blind to each other's notes.
- **`join fetch n.etudiant`** loads each note *together with* its student in one SQL statement. Without it, `NoteResponse.from(note)` would touch the lazy `etudiant` and Hibernate would fire one extra `SELECT` per student (the "N+1" problem). `@Param("matiere")` binds the method parameter to the `:matiere` placeholder.

The other repositories:

| Method | Generated query (meaning) | Used for |
|---|---|---|
| `EnseignantRepository.findByEmail(String)` → `Optional<Enseignant>` | `where email = ?` | mapping the logged-in user to an `Enseignant` |
| `MatiereRepository.findByEnseignantsContaining(Enseignant)` | matières whose `enseignants` collection contains this teacher (`exists` on the join table) | "a teacher only sees their own matières" |
| `InscriptionRepository.findByEtudiantAndMatiere(Etudiant, Matiere)` → `Optional<Inscription>` | `where etudiant_id = ? and matiere_id = ?` (at most one row, thanks to the unique constraint) | "who teaches this student in this matière?" |
| `InscriptionRepository.etudiantsDe(Matiere, Enseignant)` (`@Query`) | `select i.etudiant from Inscription i where i.matiere = :matiere and i.enseignant = :enseignant order by nom, prenom` | the students dropdown: **only this teacher's students** |
| `EtudiantRepository` | (only the inherited `findById`) | loading the student named in a request |

`Optional<T>` is used when "not found" is a normal outcome; the service decides what that means (`orElseThrow(...)`).

**Why a repository layer at all?** The service talks to an interface, so it doesn't care about SQL, and the tests replace it with a Mockito mock.

---

## 5. DTOs and validation

**DTO = Data Transfer Object**: a small class shaped exactly like what the API receives or sends. We never expose entities directly, for three reasons:
1. **Security (mass assignment):** if the form bound to `Note`, a client could send `"enseignant": {...}` or `"dateSaisie": ...` and set fields it must not control.
2. **Lazy loading / cycles:** serializing an entity to JSON walks its relations (`Matiere.enseignants → Enseignant.matieres → …`): infinite loops or `LazyInitializationException`.
3. **Stability:** the API shape can stay stable when the database model changes.

### 5.1 Input DTOs

```java
@Getter @Setter @NoArgsConstructor
public class NoteForm {
    @NotNull
    private Long etudiantId;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("20.00")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal valeur;
}
```
- Jackson builds a `NoteForm` from the JSON: it uses the no-arg constructor, then the setters (hence `@NoArgsConstructor` + `@Setter`).
- **Bean Validation annotations** state the rules *declaratively*:
  - `@NotNull` → must be present (`null` or a missing JSON property fails);
  - `@DecimalMin("0.00")` / `@DecimalMax("20.00")` → inclusive bounds (0 and 20 are valid);
  - `@Digits(integer = 2, fraction = 2)` → at most 2 digits before and 2 after the decimal point (`12.75` ✔, `12.755` ✘, `123` ✘).
- They only *run* when the controller parameter carries **`@Valid`**.
- Note that `NoteForm` has **only** `etudiantId` and `valeur`. The matière comes from the URL, the teacher from the login, the dates from the service.

`NoteModificationForm` has only `valeur` (same annotations): when editing, the student and the matière are fixed by the existing note.

### 5.2 `messages.properties` — French error messages

```properties
NotNull.noteForm.valeur=La note est obligatoire
DecimalMax.noteForm.valeur=La note doit être inférieure ou égale à 20
...
```
Key format: `AnnotationName.objectName.fieldName`. When a validation fails, Spring builds a `FieldError` carrying a list of candidate codes from most to least specific — `DecimalMax.noteForm.valeur`, `DecimalMax.valeur`, `DecimalMax.java.math.BigDecimal`, `DecimalMax` — and looks each up in `messages.properties` (Spring Boot auto-configures a `MessageSource` reading `messages*.properties`). `noteForm` is the *decapitalized class name* of the `@RequestBody` parameter type. The point: **wording lives in one file**, not in annotations, so the messages are easy to change or translate.

### 5.3 Output DTOs — Java `record`s

```java
public record MatiereResponse(Long id, String code, String libelle, int semestre, boolean cloturee) {
    public static MatiereResponse from(Matiere m) {
        return new MatiereResponse(m.getId(), m.getCode(), m.getLibelle(), m.getSemestre(), m.isCloturee());
    }
}
```
- A `record` (Java 16+) is an **immutable data carrier**: the compiler generates the constructor, accessors (`id()`, `code()`…), `equals`, `hashCode`, `toString`. Perfect for a read-only response.
- Jackson serializes a record to `{"id":1,"code":"INF101",...}`.
- The **static factory `from(entity)`** keeps the entity→DTO mapping next to the DTO; the service uses it as a method reference: `.map(MatiereResponse::from)`.

`NoteResponse` is deliberately **flat**:

```java
public record NoteResponse(Long id, BigDecimal valeur, Long matiereId, Long etudiantId,
        String etudiantNumInscription, String etudiantNom, String etudiantPrenom,
        Long enseignantId, LocalDateTime dateSaisie, LocalDateTime dateModification) { ... }
```
It contains the student's name directly, so the frontend gets everything to draw a table row with **one** request and never needs to follow relations. `BigDecimal` is serialized as a JSON number (`14.50`) and `LocalDateTime` as an ISO string (`"2026-09-24T16:46:00.4"`).

`EnseignantResponse` and `EtudiantResponse` follow the same pattern.

### 5.4 `ErreurResponse` — the shape of every error

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErreurResponse(int status, String message, Map<String, String> champs, LocalDateTime timestamp) {
    public static ErreurResponse of(int status, String message) { ... champs = null ... }
    public static ErreurResponse of(int status, String message, Map<String, String> champs) { ... }
}
```
- One uniform error format for the whole API: `{status, message, champs?, timestamp}`, so a client handles errors in one place.
- `@JsonInclude(NON_NULL)` omits `champs` when it's `null`; only validation errors have per-field messages (`{"valeur": "La note est obligatoire"}`).

---

## 6. Exceptions

```java
public abstract class NoteException extends RuntimeException {
    protected NoteException(String message) { super(message); }
}
```
- **Base class** of all business-rule violations, so a single `@ExceptionHandler(NoteException.class)` can catch them all.
- Extends **`RuntimeException` (unchecked)**: callers aren't forced to write `try/catch` or `throws`, and — crucial — **Spring's `@Transactional` rolls back on unchecked exceptions by default**. A violated rule therefore automatically cancels any partial database change.
- `abstract` + `protected` constructor: it only makes sense to throw one of the specific subclasses.

Each subclass = one rule = one **French message aimed at the user**, and (in `GlobalExceptionHandler`) one HTTP status:

| Exception | Rule | Status | Why this status |
|---|---|---|---|
| `RessourceIntrouvableException` | unknown id/email | 404 | resource doesn't exist |
| `AccesMatiereRefuseException` | teacher not assigned to the matière (spec 2.3) | 403 | authenticated but not allowed |
| `EtudiantAutreEnseignantException` | the student is enrolled in this matière but taught by **another** teacher | 403 | not allowed: it's a colleague's student |
| `MatiereClotureeException` | matière closed | 409 | conflicts with the current state of the resource |
| `NoteDejaExistanteException` | note exists for (student, matière) | 409 | conflicts with existing state |
| `EtudiantNonInscritException` | student not enrolled | 422 | request is well-formed but semantically invalid |
| `ValeurInvalideException` | value outside 0–20 (business level) | 422 | same |

`RessourceIntrouvableException` has **two constructors** (`(String, Long id)` and `(String, String identifiant)`) because we look things up both by numeric id and by email.

*Why put the status in the handler rather than in the exception?* So exceptions and the service stay **independent of HTTP**: the same service could be called from a batch job or a test with no web concept in sight.

---

## 7. The service

`NoteService` is the heart of the application: **all business rules live here**.

```java
@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {
```
- `@Service`: registers the class as a Spring bean (a specialised `@Component`); it documents the layer's role.
- `@RequiredArgsConstructor`: constructor injection of the four `final` repositories (see §2).
- `@Transactional` **at class level**: every public method runs **inside a database transaction**. Spring wraps the bean in a proxy: it opens the transaction before the method, commits when it returns, and rolls back if a `RuntimeException` escapes.
  A transaction gives **atomicity** (all changes or none), a single **persistence context** (one Java object per DB row while it runs), and keeps the lazy proxies usable.

```java
    private static final BigDecimal MAX = new BigDecimal("20");
```
The upper bound as a constant. **`new BigDecimal("20")` with a String**, never `new BigDecimal(20.0)` (the `double` constructor carries binary imprecision).

### 7.1 Read methods

```java
    @Transactional(readOnly = true)
    public Long idEnseignantPar(String email) {
        return enseignantRepository.findByEmail(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant", email))
                .getId();
    }
```
- `readOnly = true` overrides the class-level setting: a hint that nothing will be written (Hibernate skips dirty-checking and flushing; the database can optimise).
- `Optional.orElseThrow(supplier)` unwraps the value or throws our exception. **Bridges authentication and the domain:** the login username is an email; this finds the matching `Enseignant` and returns its id.

`profil(String email)` — same lookup, but `.map(EnseignantResponse::from)` returns the DTO (used by `GET /api/me`, which the Angular login calls to check the credentials and display the name).

```java
    public List<MatiereResponse> matieresDe(Long enseignantId) {
        Enseignant enseignant = enseignant(enseignantId);
        return matiereRepository.findByEnseignantsContaining(enseignant).stream()
                .map(MatiereResponse::from)
                .toList();
    }
```
Loads the teacher, queries only *their* matières (rule 2.3 applied **at the query level**: other matières are never even read), converts each entity to a DTO. `.stream().map(...).toList()` (Java 16+) returns an unmodifiable `List`.

`matiereDe`, `etudiantsDe`, `notesDe` all begin the same way:

```java
        Matiere matiere = matiere(matiereId);
        Enseignant enseignant = enseignant(enseignantId);
        verifierAffectation(enseignant, matiere);
```
**Every operation on a matière first checks that the caller teaches it.** Because the check is repeated in each method, no path forgets it.

Then the two methods that return lists filter **by teacher**, not just by matière:

```java
    // etudiantsDe
    return inscriptionRepository.etudiantsDe(matiere, enseignant).stream().map(EtudiantResponse::from).toList();
    // notesDe
    return noteRepository.findVisiblesPar(matiere, enseignant).stream().map(NoteResponse::from).toList();
```
This is the rule "two teachers of the same matière teach different students": in INF102, Ali gets only *his* students and *their* notes, Sonia only hers. The filtering happens **in the SQL query** (see `InscriptionRepository.etudiantsDe` and `NoteRepository.findVisiblesPar`), so other teachers' data is never even loaded into memory.

*Important design point:* the entity→DTO conversion (`NoteResponse.from(note)` touches `note.getEtudiant().getNom()`, a lazy proxy) happens **inside the service method**, while the transaction/session is still open.

### 7.2 `saisir` — entering a grade

```java
    public NoteResponse saisir(Long enseignantId, Long matiereId, NoteForm form) {
        Enseignant enseignant = enseignant(enseignantId);
        Matiere matiere = matiere(matiereId);

        verifierAffectation(enseignant, matiere);          // 1. enseignant affecté
        verifierNonCloturee(matiere);                      // 2. matière non clôturée

        Etudiant etudiant = etudiantRepository.findById(form.getEtudiantId())
                .orElseThrow(() -> new RessourceIntrouvableException("Étudiant", form.getEtudiantId()));
        verifierEtudiantDeLEnseignant(enseignant, matiere, etudiant);   // 3. inscrit, et suivi par CET enseignant

        verifierValeur(form.getValeur());                  // 4. 0 <= valeur <= 20

        if (noteRepository.existsByEtudiantAndMatiere(etudiant, matiere)) {   // 5. unicité
            throw new NoteDejaExistanteException(nomComplet(etudiant), matiere.getLibelle());
        }

        Note note = new Note(normaliser(form.getValeur()), etudiant, matiere, enseignant);
        return NoteResponse.from(noteRepository.save(note));
    }
```
The five rules run in this order **on purpose** (it mirrors the sequence diagram):
1. **Authorization first.** A teacher without access must learn *nothing* — not even whether a student id exists.
2. **Closed matière** — cheap, state-based, and independent of the input.
3. **The student is enrolled, and is *this teacher's* student** — needs the student loaded; see the helper below.
4. **Value range** — re-checked here although `@Valid` already did (see "Why" below).
5. **Uniqueness** — the only rule needing an extra query, so it goes last; the database unique constraint still backs it up.

Then `new Note(normaliser(...), ...)` builds the entity, and `noteRepository.save(note)` inserts it (with `IDENTITY` ids, the `INSERT` runs immediately and the returned entity has its `id`). The method returns the DTO. If any `throw` happened, nothing was saved, and the transaction is rolled back anyway.

### 7.3 `modifier` — editing a grade

```java
        Note note = noteRepository.findById(noteId).orElseThrow(...);
        Matiere matiere = note.getMatiere();
        Enseignant enseignant = enseignant(enseignantId);

        verifierAffectation(enseignant, matiere);
        verifierNonCloturee(matiere);
        verifierEtudiantDeLEnseignant(enseignant, matiere, note.getEtudiant());   // pas la note d'un collègue
        verifierValeur(valeur);

        note.modifier(normaliser(valeur));   // met à jour dateModification ; flush au commit
        return NoteResponse.from(note);
```
Notice there is **no `save(...)` call**. This is Hibernate's **dirty checking**: `note` is a *managed* entity (loaded inside this transaction). When the method returns and the transaction commits, Hibernate compares the entity to its original snapshot, sees `valeur` and `dateModification` changed, and issues the `UPDATE` itself. (Same for `matiere.cloturer()` in `cloturer(...)`.) The `note.getMatiere()` here is a lazy proxy loaded on demand — fine because we're still inside the transaction.

**The ownership check in `modifier`** is what stops two teachers of the same matière from editing each other's notes: only the teacher the student is assigned to (through `Inscription`) may change the grade. That is why "two teachers overwriting the same note" cannot happen any more — a given note has exactly one teacher allowed to write to it.

### 7.3b The ownership helper

```java
    private void verifierEtudiantDeLEnseignant(Enseignant enseignant, Matiere matiere, Etudiant etudiant) {
        Inscription inscription = inscriptionRepository.findByEtudiantAndMatiere(etudiant, matiere)
                .orElseThrow(() -> new EtudiantNonInscritException(nomComplet(etudiant), matiere.getLibelle()));
        if (!inscription.getEnseignant().getId().equals(enseignant.getId())) {
            throw new EtudiantAutreEnseignantException(nomComplet(etudiant), matiere.getLibelle());
        }
    }
```
- Looks up the single `Inscription` for (student, matière). **None** → the student isn't enrolled in that matière → `EtudiantNonInscritException` (422).
- One exists but its teacher is someone else → `EtudiantAutreEnseignantException` (403): "this student is followed by another teacher".
- Teachers are compared **by id** (`getId().equals(...)`), which is robust whatever the persistence state of the objects (real entities or Hibernate proxies).
- Used by both `saisir` and `modifier`, so the rule has a single implementation.

### 7.4 `cloturer`

Checks the assignment, calls `matiere.cloturer()`, returns the DTO. **Idempotent**: closing an already-closed matière just sets `true` again, no error.

### 7.5 The rule helpers

```java
    private void verifierValeur(BigDecimal valeur) {
        if (valeur == null || valeur.signum() < 0 || valeur.compareTo(MAX) > 0) {
            throw new ValeurInvalideException();
        }
    }
```
- `signum() < 0` = negative (`signum` returns -1, 0, or 1).
- **`compareTo`, not `equals`:** `BigDecimal.equals` also compares *scale* — `new BigDecimal("20.0").equals(new BigDecimal("20.00"))` is `false`. `compareTo` compares the numeric value.

```java
    private BigDecimal normaliser(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }
```
Forces 2 decimals (`12.5` → `12.50`) to match the `DECIMAL(4,2)` column and keep the stored value consistent. (`@Digits` already rejected more than 2 decimals, so no rounding actually happens; `HALF_UP` is the required rounding argument.)

`enseignant(id)` and `matiere(id)` are small private helpers doing `findById(...).orElseThrow(RessourceIntrouvableException)` so the code above doesn't repeat itself.

### Why the service is designed this way
- **No web dependency:** it takes ids and a DTO and returns DTOs; it doesn't know about HTTP, `Authentication`, or JSON. That makes it testable with plain unit tests and reusable.
- **Ids instead of entities as parameters:** an entity passed from outside would be *detached* (its lazy collections unusable). Reloading inside the transaction guarantees a consistent state.
- **Defence in depth on the value:** `@Valid` protects the HTTP entry point; the service re-checks because it can be called from elsewhere (tests, other controllers, future batch imports). The rule "grade between 0 and 20" is too important to trust to one layer.

---

## 8. The controller

```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;
```
- `@RestController` = `@Controller` + `@ResponseBody`: the value a method returns is **serialised to JSON** and becomes the response body (no view/template is involved).
- `@RequestMapping("/api")`: a URL prefix for every method in the class. (`/api/...` is also what our security rules and the Angular dev proxy target.)
- The controller depends **only on the service**, never on repositories.

```java
    @GetMapping("/matieres/{matiereId}")
    public MatiereResponse matiere(Authentication auth, @PathVariable Long matiereId) {
        return noteService.matiereDe(enseignantId(auth), matiereId);
    }
```
- `@GetMapping(...)`: maps `GET /api/matieres/{matiereId}`. Similarly `@PostMapping`, `@PutMapping`.
- `{matiereId}` is a **path variable**; `@PathVariable Long matiereId` binds it, converting the text to `Long` (with the parameter name known thanks to the `-parameters` flag). `/api/matieres/abc/notes` can't convert → `MethodArgumentTypeMismatchException` → our handler answers `400`.
- **`Authentication auth`**: Spring MVC injects the **currently logged-in user** (the object the security filter stored for this request). `auth.getName()` is the username — the teacher's email.

```java
    private Long enseignantId(Authentication auth) {
        return noteService.idEnseignantPar(auth.getName());
    }
```
**The teacher's identity comes from the verified login, never from the request.** (Earlier we used an `X-Enseignant-Id` header for testing: any client could claim to be any teacher. Now it can't.)

```java
    @PostMapping("/matieres/{matiereId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse saisir(Authentication auth,
                               @PathVariable Long matiereId,
                               @Valid @RequestBody NoteForm form) {
        return noteService.saisir(enseignantId(auth), matiereId, form);
    }
```
- `@RequestBody` → Spring converts the JSON body to a `NoteForm` (Jackson).
- **`@Valid`** → runs the Bean Validation annotations of `NoteForm` *before* the method body executes. On failure Spring throws `MethodArgumentNotValidException` and the body never runs.
- `@ResponseStatus(CREATED)` → status `201` instead of the default `200` (REST convention for "created").

Endpoint table:

| Method + URL | Purpose | Success |
|---|---|---|
| `GET /api/me` | logged-in teacher's profile | 200 |
| `GET /api/matieres` | my matières | 200 |
| `GET /api/matieres/{id}` | one matière | 200 |
| `GET /api/matieres/{id}/etudiants` | **my** students in this matière (not a colleague's) | 200 |
| `GET /api/matieres/{id}/notes` | notes of a matière | 200 |
| `POST /api/matieres/{id}/notes` | enter a note | 201 |
| `PUT /api/notes/{id}` | modify a note (`PUT` = replace the value) | 200 |
| `POST /api/matieres/{id}/cloturer` | close a matière (an *action*, hence `POST`) | 200 |

**"Thin controller":** it receives, checks the *format*, delegates, returns. Every rule lives in the service. Two benefits: rules are unit-testable without HTTP, and a second interface could reuse them.

---

## 9. The global exception handler

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;
```
- `@RestControllerAdvice` (= `@ControllerAdvice` + `@ResponseBody`): a class whose `@ExceptionHandler` methods apply to **all controllers**. When an exception escapes a controller method, Spring picks the handler with the **most specific matching exception type**.
- `MessageSource` is the Spring bean that reads `messages.properties` (auto-configured, injected through the constructor).

```java
    private static final Map<Class<? extends NoteException>, HttpStatus> STATUTS = Map.of(
            RessourceIntrouvableException.class, HttpStatus.NOT_FOUND,
            AccesMatiereRefuseException.class, HttpStatus.FORBIDDEN, ... );

    @ExceptionHandler(NoteException.class)
    public ResponseEntity<ErreurResponse> regleMetier(NoteException e) {
        return reponse(STATUTS.getOrDefault(e.getClass(), HttpStatus.BAD_REQUEST), e.getMessage());
    }
```
- One handler for the whole hierarchy; the **status is looked up in a `Map` by the exception's exact class**, defaulting to 400. (A pattern-matching `switch` would be more elegant, but it isn't available in Java 17 without preview flags.)
- `ResponseEntity<T>` lets us control **status + body** explicitly.

```java
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurResponse> validation(MethodArgumentNotValidException e) {
        Map<String, String> champs = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                champs.putIfAbsent(fe.getField(),
                        messageSource.getMessage(fe, LocaleContextHolder.getLocale())));
        return ResponseEntity.badRequest().body(ErreurResponse.of(400, "Données invalides", champs));
    }
```
- Thrown when `@Valid` fails. `getBindingResult().getFieldErrors()` lists each violated field.
- `messageSource.getMessage(fe, locale)` resolves the French text from `messages.properties` (see §5.2); `LocaleContextHolder.getLocale()` is the request's locale (from `Accept-Language`); our single `messages.properties` serves all locales.
- `LinkedHashMap` keeps field order; `putIfAbsent` keeps **one message per field** (a field can break several rules at once).

The other handlers:
- `HttpMessageNotReadableException` → `400`: malformed JSON, or a value that can't convert (`"valeur": "abc"`).
- `MethodArgumentTypeMismatchException` → `400`: bad path variable (`/matieres/abc`).
- `DataIntegrityViolationException` → `409`: Spring translates the database error into this exception. It's the **safety net**: two simultaneous requests could both pass `existsByEtudiantAndMatiere`; the `unique` constraint rejects the second, and this handler turns the crash into a clean `409`.

```java
    private ResponseEntity<ErreurResponse> reponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErreurResponse.of(status.value(), message));
    }
```
A helper so every handler builds the same body.

**Why centralise?** Controllers and services stay free of `try/catch`; the API answers errors in a single, consistent format.

---

## 10. Security

Spring Security works as a chain of **servlet filters** that run *before* the controller.

```java
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
```
- `@Configuration` + `@Bean`: we define beans ourselves. A method annotated `@Bean` returns an object Spring will manage.
- Defining a `SecurityFilterChain` bean is the modern way to configure security (the old `WebSecurityConfigurerAdapter` no longer exists). `HttpSecurity` is a builder Spring hands us.

```java
        http.csrf(AbstractHttpConfigurer::disable)
```
**CSRF** (cross-site request forgery) is an attack that abuses *browser cookies* sent automatically. Our API uses **no cookie or session** (each request carries its own credentials), so CSRF doesn't apply, and leaving it on would just block our `POST`/`PUT` requests.

```java
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
`STATELESS`: never create an HTTP session. Every request must authenticate itself. (Good for scaling and for a token/Basic API.)

```java
            .authorizeHttpRequests(a -> a
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll())
```
Rules are evaluated **in order; the first match wins**:
1. `OPTIONS` on anything: allowed — browsers send these "preflight" requests for cross-origin calls; preparing for a frontend on another origin.
2. anything under `/api/`: must be **authenticated**.
3. everything else: `denyAll()` — **deny by default**, so a forgotten URL is never accidentally public.

```java
            .httpBasic(b -> b.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
```
- `httpBasic`: enables **HTTP Basic authentication**. A filter (`BasicAuthenticationFilter`) reads `Authorization: Basic base64(username:password)`, decodes it, and hands it to the authentication manager.
- The **entry point** decides what happens when authentication is missing or wrong. By default Spring answers `401` **with a `WWW-Authenticate: Basic` header**, which makes browsers open their own native login popup — on top of our Angular login page. `HttpStatusEntryPoint(UNAUTHORIZED)` returns a plain `401` without that header.
- `http.build()` produces the configured chain.

```java
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
```
Passwords are never compared as plain text. **BCrypt** is a deliberately slow, salted hash: `encode("prof1")` gives a different string each time (random salt inside), and `matches(raw, hash)` verifies. Spring Security requires a `PasswordEncoder` bean.

```java
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("ali.benali@fds.tn").password(encoder.encode("prof1")).roles("ENSEIGNANT").build(),
                User.withUsername("sonia.trabelsi@fds.tn").password(encoder.encode("prof2")).roles("ENSEIGNANT").build());
    }
```
- A `UserDetailsService` is the component that **finds a user by username** for authentication. `InMemoryUserDetailsManager` keeps users in memory — enough for a demo.
- The passwords are hashed at startup; `roles("ENSEIGNANT")` grants the authority `ROLE_ENSEIGNANT` (not used for authorization yet: every authenticated user may call `/api/**`; the *data-level* rules are in the service).
- **The username is the teacher's email**, which is how the logged-in user is linked to the `Enseignant` row (`idEnseignantPar`). The `Enseignant` entity has **no password field** on purpose: authentication is kept out of the domain model (and out of the UML diagram).
- Declaring our own `UserDetailsService` bean also switches off Spring Boot's default "generated password" user.

---

## 11. Seed data

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
```
- `@Component`: a generic Spring bean. `CommandLineRunner` is a Spring Boot interface whose `run(...)` is called **once, after the context has started**.
- `@Slf4j` gives the `log` object.

```java
        if (enseignantRepository.count() > 0) { return; }
```
**Idempotence guard:** seed only into an empty database, so restarts don't duplicate data.

The rest builds the demo data in dependency order — **matières → enseignants (with their `enseigne` assignments) → étudiants → inscriptions (student + matière + teacher) → notes** — because each step references saved entities. The private helpers `matiere(...)`, `enseignant(...)`, `etudiant(...)` just avoid repeating setter calls. `analyse.cloturer()` closes MAT101 so the closure rule is visible from the start; student Karim is not enrolled in INF101 so the "not enrolled" rule can be demonstrated.

**The seed is a Faculty of Law sample:** 19 matières (law subjects, `DRT...` codes), **10 per teacher**, 16 students, about 200 inscriptions and 70 notes. The first four matières, the first six students and the first four notes keep the ids used by the Postman collection; the rest is generated in a loop (`remplir`) with a fixed rule, so the ids and values are the same at every start. In a closed matière every enrolled student has a grade; elsewhere only some do, so the "add a note" dropdown always has someone to offer.

**DRT102 (droit pénal général) is deliberately shared:** Ali follows Gharbi, Mansour, Trabelsi and Chaabane there, Sonia follows Hamdi, Bouzid, Kefi, Ben Salah and Ayadi (and a note for Hamdi exists). This lets you demonstrate that each teacher sees only their own group. `new Inscription(student, matière, teacher)` runs its constructor check, so the seed itself can't contain a teacher assigned to a matière they don't teach.

Two details worth knowing:
- We save with **`List.of(...)`, not `Set.of(...)`**: `Set.of` iterates in an unspecified order, so ids came out different at each start. A list keeps insertion order → ids are stable → Postman/documentation stay valid.
- The class isn't `@Transactional`: each `saveAll` is its own transaction (Spring Data repository methods are transactional). That works here because the many-to-many collections only need the referenced entities' **ids** to write the join-table rows.

`log.info("... {} ...", value)` uses `{}` placeholders (SLF4J): the message is only built if the log level is enabled.

---

## 12. Unit tests

`NoteServiceTest` tests the rules **without a database or Spring**:

```java
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {
    @Mock EnseignantRepository enseignantRepository; ...
    @InjectMocks NoteService service;
```
- `@ExtendWith(MockitoExtension.class)`: JUnit 5 + Mockito integration.
- `@Mock`: a fake repository whose behaviour we script. `@InjectMocks`: builds the real `NoteService` and passes the mocks to its (Lombok-generated) constructor.
- `lenient().when(repo.findById(ID)).thenReturn(Optional.of(x))` scripts an answer; `lenient()` because Mockito's strict mode fails a test that defines an answer it never uses.
- `assertThrows(SomeException.class, () -> service.saisir(...))` checks the rule triggers; `verify(noteRepository, never()).save(any())` checks that **nothing was saved** when a rule failed; `ArgumentCaptor` grabs the `Note` passed to `save` to check its fields (scale 2, teacher recorded, `dateSaisie` set).
- The fixtures (`ali` teaches `algo`; `sonia` doesn't; `amine` is enrolled and assigned to Ali via an `Inscription`) let each test change **one** condition (`algo.cloturer()`, an empty inscription lookup, an inscription owned by a colleague) and assert the matching exception.
- New for the "one teacher per student" rule: a colleague's student is refused on `saisir` **and** on `modifier` (the note is left untouched), the lists only return the caller's data, and an `Inscription` can't be built for a teacher who doesn't teach the matière.

Result: 28 tests, running in about 2 seconds.

---

## 13. Recap

### 13.1 The decisions, in one table

| Decision | Reason |
|---|---|
| Layered architecture (controller → service → repository) | separation of concerns; testability |
| Rules only in the service | one place to read/test/change; controllers stay thin |
| DTOs in and out, never entities | no mass assignment, no lazy-loading/cycle problems, stable API |
| Two levels of validation (format, then business) | fast feedback for bad input + real rules that need the database |
| Rule also enforced by the database (unique constraint) | last line of defence against races and bypasses |
| Custom unchecked exceptions + one global handler | clean code, automatic transaction rollback, uniform errors |
| `BigDecimal(4,2)` for grades | exact decimals |
| `LAZY` relations + DTO mapping inside the transaction | avoids loading whole object graphs |
| Identity from the authenticated user | a client can't impersonate another teacher |
| Stateless HTTP Basic + BCrypt | simple, works with Postman and Angular, no session/CSRF surface |
| `Note` has no setters | the object can't be put in an inconsistent state |
| `Inscription` entity (student + matière + teacher, unique on student+matière) | a student has exactly one teacher per matière; teachers of the same matière can't see or edit each other's students and grades |

### 13.2 Known limitations (be honest about these in the interview)

1. **`equals`/`hashCode` are not overridden on entities.** `enseignant.enseigne(matiere)` relies on identity, which is correct *inside one transaction* (Hibernate returns one object per row). It would break if entities from different sessions were compared. A stricter version would base `equals` on the business key (`code`, `email`).
2. **Extra queries with lazy loading.** The notes list uses `join fetch` so it doesn't run one `SELECT` per student. Any *new* list endpoint that maps entities with lazy relations should do the same; `spring.jpa.show-sql=true` shows what Hibernate really executes.
3. **HTTP Basic re-checks the BCrypt hash on every request** (deliberately slow). Fine for a demo; a token (JWT) would avoid it.
4. **In-memory users and demo passwords** — production needs users and hashed passwords in the database, and `ddl-auto=update` replaced by migrations. The database credentials in `application.properties` (`root`/`root`) must move to environment variables.
5. **No optimistic locking** (`@Version`). Two *different* teachers can no longer overwrite each other's note, because a note has exactly one teacher allowed to write to it (`Inscription`). What remains is the *same* teacher editing a note from two browser tabs: the last save wins silently. A `@Version` field would detect that.
6. **Small information leaks in the error codes.** `PUT /api/notes/{id}` answers `403` for a note the caller may not touch but `404` for an id that doesn't exist, and the message for a colleague's student says the student is followed by "another teacher". Acceptable here (it makes the demo self-explanatory); a stricter API would answer `404` in both cases.
7. **Student reassignment isn't implemented.** `Inscription` is created once (seed data); there is no endpoint to move a student to another teacher. Notes already entered keep working for the *current* teacher of the inscription, because visibility is computed from `Inscription`, not from who originally typed the note (`Note.enseignant`, kept for traceability).
8. **No administration side yet.** Creating matières, assignments (`enseigne`) and inscriptions is done by the seed data only.
9. The `open-in-view` default of Spring Boot (session kept open during the request) is left as is; we don't rely on it because the DTOs are built inside the service.

### 13.3 Questions you should be ready for

- **Why a service layer? Why not put the rules in the controller?** Testability, reuse, thin controllers.
- **Why DTOs?** Mass assignment, lazy loading/cycles, API stability.
- **Why `BigDecimal`?** Exact decimal arithmetic; `double` can't represent all decimal values exactly.
- **What does `@Transactional` do, and why does `modifier` not call `save`?** Transaction + dirty checking.
- **What is `LAZY` and what can go wrong?** Proxies loaded on demand; `LazyInitializationException` outside a session.
- **Difference between 400, 403, 404, 409, 422 here?** Format / not allowed / doesn't exist / conflicts with state / semantically invalid.
- **Why validate the range twice?** Defence in depth; the service can be called from elsewhere.
- **Where is the "one note per student and matière" rule enforced?** Three places: the `existsBy...` check (friendly message), the database unique constraint (guarantee), and the `DataIntegrityViolationException` handler (turns a race into a clean 409).
- **Two teachers teach the same matière: how do you keep their students and grades separate?** Through the `Inscription` entity (student, matière, teacher) with a unique constraint on (student, matière): a student has exactly one teacher per matière. The lists are filtered by teacher in the query, and `saisir`/`modifier` call `verifierEtudiantDeLEnseignant`, which returns 403 for a colleague's student.
- **Why is `Inscription` an entity and not a `@ManyToMany`?** A plain many-to-many can't carry the extra data (which teacher). The join has become an entity with its own columns.
- **Why `join fetch` in `findVisiblesPar`?** To load each note with its student in one query instead of one query per student (N+1).
- **Why disable CSRF?** No cookies/session → no CSRF risk.
- **How does the API know who the teacher is?** The `Authorization` header → Spring Security → `Authentication` → email → `Enseignant`.
- **`@Component` vs `@Service` vs `@RestController` vs `@Configuration`?** All are beans found by component scanning; the stereotypes document the role (`@RestController` adds JSON handling, `@Configuration` allows `@Bean` methods).

---

# PART 2 — FRONTEND

---

## 14. The frontend's big picture

The frontend is a **single-page application (SPA)** written with **Angular 22** and **TypeScript**:

- The browser downloads `index.html` and a JavaScript bundle **once**. After that, moving between pages does **not** reload the page: Angular's router swaps components in and out, and the data comes from the backend as **JSON** over `/api/...`.
- The frontend contains **no business rules**. It shows data, collects input, checks the *format* of that input for a fast answer, and displays the backend's verdicts. The backend stays the authority (a client can always be bypassed with Postman).

### Folder map

```
frontend/
├── package.json  angular.json  proxy.conf.json  tsconfig*.json      tooling and configuration
├── public/armoiries.png                       static file, copied as-is to the site root
└── src/
    ├── index.html  main.ts  styles.css        the HTML shell, the entry point, global styles
    └── app/
        ├── app.ts / app.html / app.css        the shell: top bar + <router-outlet>
        ├── app.config.ts                      global providers (router, HTTP, locale)
        ├── app.routes.ts                      URL -> page table
        ├── core/                              shared by every page
        │     ├── models.ts                    TypeScript types mirroring the backend DTOs
        │     ├── erreurs.ts                   HTTP error -> French message
        │     ├── notes-api.service.ts         the only place that knows the API URLs
        │     ├── auth.service.ts              login state, credentials in memory
        │     ├── auth.interceptor.ts          adds the Authorization header to every /api call
        │     └── auth.guard.ts                route protection (authGuard, guestGuard)
        ├── accueil/   login/   matieres/   notes/     one folder per page
        └── notes/note-formulaire.*            the add / edit form (a child component)
```
Each folder holds a component as three files (`.ts` logic, `.html` template, `.css` styles) plus a `.spec.ts` test.

### The journey of a click

Example: the teacher clicks **Enregistrer** in the "Ajouter une note" form.

1. The browser fires the form's `submit` event → Angular calls `soumettre()` in `NoteFormulaire`.
2. `soumettre()` checks the format (client-side validators). If invalid → shows messages and stops.
3. Otherwise it calls `NotesApi.saisir(...)`, which uses `HttpClient` to send `POST /api/matieres/1/notes`.
4. The **interceptor** adds `Authorization: Basic ...` to the request.
5. In development, the **dev-server proxy** forwards `/api` to Spring Boot on port 8080 (Part 1 §0 shows what happens there).
6. The JSON response arrives. The Observable emits; the component emits `enregistre` to its parent, which updates a **signal** holding the notes.
7. Angular sees that a signal read by the template changed and **re-renders** just what depends on it: the new row appears.
8. If the backend answered with an error, the same chain ends in `afficherErreur(...)` instead: the message shows under the field or above the buttons.

**Parallel with the backend layers:** components ≈ controllers (thin: they receive events and delegate), `NotesApi` ≈ the client-side counterpart of `NoteController`, `models.ts` ≈ the DTOs.

---

## 15. Project setup and tooling

### 15.1 Node.js, npm, Angular CLI
- **Node.js** runs the *development tools* (compiler, dev server, test runner). The finished app is plain static files; **Node is not needed in production** (nginx serves them).
- **npm** downloads the libraries listed in `package.json` into `node_modules/` (never committed to git).
- **Angular CLI** (`ng`) is the command-line tool: `ng serve` (dev server with live reload), `ng build` (production bundle), `ng test` (unit tests). We call it as `npx ng ...` so no global install is needed.

### 15.2 `package.json`

```json
"scripts": { "ng": "ng", "start": "ng serve", "build": "ng build", "watch": "...", "test": "ng test" },
"packageManager": "npm@11.19.0",
```
Shortcuts (`npm start` = `ng serve`) and the npm version the project was created with.

| Dependency | What it is |
|---|---|
| `@angular/core` | the framework core: components, signals, dependency injection |
| `@angular/common` | `HttpClient`, pipes (`date`, `number`), `@if`/`@for` support |
| `@angular/compiler` | compiles templates |
| `@angular/forms` | form handling (we use *reactive forms*) |
| `@angular/platform-browser` | runs Angular in a browser (`bootstrapApplication`) |
| `@angular/router` | URL ↔ component navigation |
| `rxjs` | **Observables**: streams of asynchronous values; `HttpClient` returns them |
| `tslib` | small TypeScript runtime helpers |

`devDependencies` (build/test only): `@angular/build` (the builder, based on esbuild/Vite: very fast), `@angular/cli`, `@angular/compiler-cli` (ahead-of-time compiler), `typescript`, `vitest` (test runner), `jsdom` (a fake browser DOM so tests run in Node), `prettier` (formatter). The `^22.2.0` notation means "22.2.0 or any newer 22.x".

Note: there is **no `zone.js`**. Older Angular apps depended on it to detect when to refresh the screen; this project is **zoneless** (see §16).

### 15.3 `angular.json` — how the app is built, served, tested

```json
"build": { "builder": "@angular/build:application",
  "options": { "browser": "src/main.ts", "tsConfig": "tsconfig.app.json",
               "assets": [ { "glob": "**/*", "input": "public" } ],
               "styles": [ "src/styles.css" ] },
```
- `builder`: which tool builds the app. `browser`: the entry point. `tsConfig`: TypeScript settings for the app.
- `assets`: copy everything in `public/` to the root of the built site, so `public/armoiries.png` is served at `/armoiries.png` (that is why the home page uses `src="armoiries.png"`).
- `styles`: the **global** stylesheet.

```json
"production": { "budgets": [ { "type": "initial", "maximumWarning": "500kB", "maximumError": "1MB" }, ... ],
                "outputHashing": "all" },
"development": { "optimization": false, "extractLicenses": false, "sourceMap": true }
```
- **production** build: minified/optimised; **budgets** make the build warn/fail if the first download grows past 500 kB / 1 MB (a guard against bloat); `outputHashing: "all"` puts a content hash in file names (`main-56IEHOOY.js`) so browsers cache aggressively yet always fetch a new file when the code changes. `"defaultConfiguration": "production"` for `ng build`.
- **development**: no optimisation, source maps (debug the original TypeScript in the browser).

```json
"serve": { "builder": "@angular/build:dev-server",
           "options": { "proxyConfig": "proxy.conf.json" }, "defaultConfiguration": "development" },
"test":  { "builder": "@angular/build:unit-test" }
```
The dev server (port 4200) uses `proxy.conf.json`; `ng test` uses the Vitest-based unit-test builder.

### 15.4 `proxy.conf.json`

```json
{ "/api": { "target": "http://localhost:8080", "secure": false } }
```
Every request whose path starts with `/api` is forwarded by the dev server to Spring Boot. Why:
- The browser only ever talks to `localhost:4200` — **the same origin** for the page and the API, so the browser's **CORS** restrictions never apply and Spring needs no CORS configuration.
- It **mimics production**, where nginx will serve the Angular files and forward `/api` to the backend in exactly the same way.
- `secure: false` means "don't insist on a valid HTTPS certificate for the target" (ours is plain HTTP).

### 15.5 `tsconfig.json`
Settings worth knowing: `target: ES2022` (modern JavaScript output); `noImplicitReturns` (a function must return on every path); `noPropertyAccessFromIndexSignature` (for dictionary-like objects you must write `erreurs['serveur']`, not `erreurs.serveur` — visible in `note-formulaire.ts`); `experimentalDecorators` (needed for `@Component`); `isolatedModules`; and Angular checks such as `strictInjectionParameters`.

### 15.6 `index.html` and `main.ts`

```html
<html lang="fr"> ... <title>Faculté de Droit de Sfax – Saisie des notes</title>
<base href="/"> <meta name="viewport" content="width=device-width, initial-scale=1"> ...
<body><app-root></app-root></body>
```
- `lang="fr"` helps screen readers and browser translation; `<title>` is the tab title.
- **`<base href="/">`** is required by the router: it builds all URLs relative to it.
- The `viewport` meta tag makes the page render at device width on phones (responsive layout depends on it).
- `<app-root>` is the **mount point**: Angular replaces it with the `App` component.

```ts
bootstrapApplication(App, appConfig).catch((err) => console.error(err));
```
`main.ts` starts the application with `App` as the root component and `appConfig` as the global configuration. (There is no `NgModule`: see "standalone components" below.)

---

## 16. Angular concepts

A quick glossary of everything the code uses, each with where it appears.

| Concept | Meaning | Where |
|---|---|---|
| **Component** | a class + HTML template + CSS = one piece of UI, used through its `selector` (e.g. `<app-note-formulaire>`) | every page |
| **Standalone component** | a component that declares its own dependencies in `imports: [...]` (no `NgModule`); default in modern Angular | all components |
| **Template** | the HTML with Angular syntax: `{{ }}` (interpolation), `[prop]` (bind a value in), `(event)` (listen), `@if`/`@for` | `*.html` |
| **Signal** | a value holder that **notifies** whoever reads it: `const n = signal(0); n()` reads, `n.set(1)` writes | `matiere`, `notes`, `erreur`... |
| **`computed`** | a signal derived from others, recalculated only when they change | `etudiantsSansNote`, `isLoggedIn` |
| **`effect`** | code that re-runs automatically when the signals it read change | `Notes` (reload on route change), `NoteFormulaire` |
| **`input()` / `output()`** | how a component receives data from its parent / sends events up | `Notes.id`, `NoteFormulaire` |
| **Control flow** `@if @else @for` | template conditions and loops (replaces `*ngIf`/`*ngFor`); `@for` requires `track` | all templates |
| **Dependency injection** | Angular creates shared objects (services) and hands them to whoever asks: `inject(AuthService)` | services, guards, interceptor |
| **Service** | a class holding shared logic/state; `@Injectable({ providedIn: 'root' })` = one instance for the whole app | `AuthService`, `NotesApi` |
| **RxJS Observable** | a stream of values over time. `HttpClient` calls return one that emits the response then completes; **nothing is sent until someone `subscribe`s** | `NotesApi`, components |
| **Router** | maps URLs to components, supports guards and lazy loading | `app.routes.ts` |
| **Reactive forms** | forms defined in TypeScript (`FormGroup`/`FormControl`) with validators | login, note form |
| **Pipes** | template formatters: `\| number: '1.2-2'`, `\| date: 'dd/MM/yyyy HH:mm'` | notes table |
| **Zoneless change detection** | the screen refreshes when a **signal read by the template changes** or a template event handler runs — not "after any async operation" as with zone.js | whole app |

Two points that explain many lines below:
- **HTTP Observables are cold and single-shot.** `this.http.get(...)` does nothing until `.subscribe(...)`; it emits once, then completes (so no manual unsubscribe is needed).
- **In a template, a signal is *called*:** `{{ erreur() }}`, `@if (chargement())`. Reading it inside the template is what registers the dependency that triggers a re-render.

Angular also **escapes** everything inserted with `{{ }}` (it never interprets it as HTML), which protects against script injection (XSS). We never use `innerHTML`.

---

## 17. Bootstrapping, configuration and routing

### 17.1 `app.config.ts`

```ts
registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: LOCALE_ID, useValue: 'fr' },
  ]
};
```
- `registerLocaleData(localeFr)`: Angular only ships US-English formatting rules; this loads the **French** ones (decimal comma, `dd/MM/yyyy`). It runs when the module loads.
- `providers` is the list of **global services and settings** (the DI configuration):
  - `provideBrowserGlobalErrorListeners()`: routes uncaught errors and unhandled promise rejections to Angular's error handler.
  - `provideRouter(routes, withComponentInputBinding())`: installs the router with our route table. **`withComponentInputBinding()`** makes the router pass route parameters into component **inputs** of the same name: `/matieres/:id/notes` → `Notes.id`.
  - `provideHttpClient(withInterceptors([authInterceptor]))`: makes `HttpClient` injectable, with our interceptor in its pipeline.
  - `{ provide: LOCALE_ID, useValue: 'fr' }`: the pipes (`number`, `date`) now format in French: `14,50`, `25/09/2026`.

### 17.2 `app.routes.ts`

```ts
export const routes: Routes = [
  { path: '', pathMatch: 'full', canActivate: [guestGuard],
    loadComponent: () => import('./accueil/accueil').then((m) => m.Accueil) },
  { path: 'login', canActivate: [guestGuard],
    loadComponent: () => import('./login/login').then((m) => m.Login) },
  { path: 'matieres', canActivate: [authGuard],
    loadComponent: () => import('./matieres/matieres').then((m) => m.Matieres) },
  { path: 'matieres/:id/notes', canActivate: [authGuard],
    loadComponent: () => import('./notes/notes').then((m) => m.Notes) },
  { path: '**', redirectTo: '' },
];
```
- The router tests routes **top to bottom; the first match wins**.
- `path: ''` with **`pathMatch: 'full'`**: without `full`, an empty path is a *prefix* of every URL and would match everything.
- **`canActivate`**: guards that run before the route is entered (§19). `guestGuard` on `''` and `login` sends an already-logged-in user to `/matieres`; `authGuard` on the other two sends a visitor to `/login`.
- **`loadComponent: () => import('...')`** is **lazy loading**: the component's code is downloaded only when its route is first visited. The production build shows this: `login`, `notes`, `matieres`, `accueil` are separate small chunks, so the first download stays small.
- `:id` is a **route parameter**; combined with `withComponentInputBinding()` it arrives as `input.required<string>()` (always a string: the component converts with `Number(...)`).
- `'**'` (wildcard) catches every unknown URL and redirects to the home page.

---

## 18. Core

### 18.1 `core/models.ts`

```ts
export interface Matiere { id: number; code: string; libelle: string; semestre: number; cloturee: boolean; }
export interface Note { id: number; valeur: number; matiereId: number; etudiantId: number;
  etudiantNumInscription: string; etudiantNom: string; etudiantPrenom: string;
  enseignantId: number; dateSaisie: string; dateModification: string | null; }
```
- One `interface` per backend DTO (`MatiereResponse`, `NoteResponse`, `EtudiantResponse`, `EnseignantResponse`). Same field names as the JSON: **this is the contract** between the two halves.
- Interfaces exist **only at compile time**: they let the compiler catch typos (`note.etudiantNon` would not compile), but nothing checks the JSON at runtime.
- `dateSaisie: string`: JSON has no date type; dates arrive as ISO strings and are formatted by the `date` pipe. `dateModification: string | null`: `null` means "never modified" (matches the nullable backend column).
- `valeur: number`: the backend's `BigDecimal` is sent as a JSON number. JavaScript numbers are floating point; that is fine for **displaying** a grade, which is all the frontend does with it (it deliberately doesn't do arithmetic on grades).

### 18.2 `core/erreurs.ts`

```ts
export function messageErreur(erreur: unknown): string {
  if (erreur instanceof HttpErrorResponse) {
    if (erreur.status === 0) { return 'Serveur injoignable, réessayez dans un instant'; }
    const message = erreur.error?.message;
    if (typeof message === 'string') { return message; }
  }
  return 'Une erreur inattendue est survenue';
}
```
- Turns any failure into a **user-facing French sentence**.
- `unknown` (not `any`): forces us to check the type before use.
- `HttpErrorResponse.status === 0` means the browser got **no response at all** (server down, network cut).
- `erreur.error` is the parsed JSON body of the error response: for our API that is `ErreurResponse`, whose `message` is already French (`"La matière « Analyse » est clôturée..."`). So the frontend **shows the backend's own words**; the wording lives in one place.
- Used by every page that loads or sends data.

### 18.3 `core/notes-api.service.ts`

```ts
@Injectable({ providedIn: 'root' })
export class NotesApi {
  private readonly http = inject(HttpClient);

  matieres(): Observable<Matiere[]> { return this.http.get<Matiere[]>('/api/matieres'); }
  notes(matiereId: number): Observable<Note[]> { return this.http.get<Note[]>(`/api/matieres/${matiereId}/notes`); }
  saisir(matiereId: number, note: NouvelleNote): Observable<Note> { return this.http.post<Note>(`/api/matieres/${matiereId}/notes`, note); }
  modifier(noteId: number, valeur: number): Observable<Note> { return this.http.put<Note>(`/api/notes/${noteId}`, { valeur }); }
  cloturer(matiereId: number): Observable<Matiere> { return this.http.post<Matiere>(`/api/matieres/${matiereId}/cloturer`, null); }
  ...
}
```
- `@Injectable({ providedIn: 'root' })`: one shared instance, created on first use.
- `inject(HttpClient)`: asks Angular's DI for the HTTP client (the modern alternative to constructor parameters).
- Each method returns an **`Observable`** and does **not** subscribe: the caller decides when to send the request and what to do with the result.
- `get<Matiere[]>(...)`: the generic only *types* the result; it doesn't validate it.
- URLs are **relative** (`/api/...`), so the same code works with the dev proxy and behind nginx in production.
- `${matiereId}`: a template literal; `post(url, null)` for `cloturer` because the action needs no body.
- The `NouvelleNote` interface (`etudiantId`, `valeur`) is the request body, matching the backend `NoteForm`. `modifier` sends `{ valeur }`, matching `NoteModificationForm`.
- **Why a service just for URLs?** Components never contain a URL; if an endpoint changes there is one file to edit, and tests can replace the whole service.

---

## 19. Authentication

The backend uses **stateless HTTP Basic** (Part 1 §10): every request must carry `Authorization: Basic base64(email:password)`. The frontend therefore has to *remember the credentials* and *attach them to every call*.

### 19.1 `core/auth.service.ts`

```ts
function basic(email: string, password: string): string {
  const octets = new TextEncoder().encode(`${email}:${password}`);
  return 'Basic ' + btoa(String.fromCharCode(...octets));
}
```
Builds the header value. `btoa` (base64) only accepts Latin-1 characters and would throw on accents (`é`); encoding to **UTF-8 bytes first** makes any password work. (Base64 is an encoding, **not** encryption: this is why Basic must only run over HTTPS in production.)

```ts
private readonly http = inject(HttpClient);
private readonly router = inject(Router);
private authorization: string | null = null;
private readonly utilisateur = signal<Enseignant | null>(null);
readonly user = this.utilisateur.asReadonly();
readonly isLoggedIn = computed(() => this.utilisateur() !== null);
```
- `authorization` holds the header value **only in memory**: a plain private field, never `localStorage`/`sessionStorage`. Consequences: a page reload logs the user out, and a malicious script can't read the password from browser storage. (It's a security-over-convenience choice.)
- `utilisateur` is a **writable signal** (kept private); `user` is its **read-only** view exposed to the rest of the app (`asReadonly()`), so only `AuthService` can change who is logged in.
- `isLoggedIn` is a `computed`: true when a user is set. The guards and the top bar read it.

```ts
login(email: string, password: string): Observable<Enseignant> {
  const authorization = basic(email.trim(), password);
  return this.http.get<Enseignant>('/api/me', { headers: { Authorization: authorization } })
    .pipe(tap((user) => { this.authorization = authorization; this.utilisateur.set(user); }));
}
```
- There is no separate "login" endpoint. Logging in means **trying the credentials on a harmless call, `GET /api/me`**, which returns the teacher's profile.
- The header is passed **explicitly** here because it isn't stored yet (and the interceptor skips requests that already have one).
- `tap(...)` runs a side effect **only when the response is a success**: the credentials are remembered and the user signal set only if the server accepted them. A 401 flows through as an error and nothing is stored.
- `email.trim()` removes accidental spaces.

```ts
logout(destination = '/'): void {
  this.authorization = null; this.utilisateur.set(null);
  void this.router.navigateByUrl(destination);
}
authorizationHeader(): string | null { return this.authorization; }
```
- Clears everything, then navigates: home page for a voluntary logout (default `'/'`), `/login` when the session died (the interceptor passes `'/login'`).
- `void` explicitly discards the Promise `navigateByUrl` returns.
- `authorizationHeader()` lets the interceptor read the value without exposing the field.

### 19.2 `core/auth.interceptor.ts`

```ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const authorization = auth.authorizationHeader();

  if (!authorization || !req.url.startsWith('/api') || req.headers.has('Authorization')) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: authorization } })).pipe(
    catchError((erreur) => {
      if (erreur.status === 401) { auth.logout('/login'); }
      return throwError(() => erreur);
    }),
  );
};
```
An **interceptor** is middleware for `HttpClient`: every request and response passes through it.
- `next(req)` passes the request down the chain (eventually to the network).
- **Guard clause:** leave the request untouched if we're not logged in, if it isn't an `/api` call (never leak credentials to other URLs), or if it already carries an `Authorization` header (the login call).
- `req.clone({ setHeaders })`: HTTP requests are **immutable**, so we send a modified *copy*.
- `.pipe(catchError(...))`: if the server answers **401** while we thought we were logged in (password changed, session invalid), log out and go to `/login`; then `throwError` **re-throws** so the caller still sees the failure.
- Registered once in `app.config.ts`; components never touch headers.

### 19.3 `core/auth.guard.ts`

```ts
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/login']);
};
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? inject(Router).createUrlTree(['/matieres']) : true;
};
```
- A **guard** is a function the router calls before entering a route. It returns `true` (proceed) or a **`UrlTree`** (redirect there).
- `authGuard` protects private pages; `guestGuard` is its mirror for public pages (home, login) so a logged-in user pressing **Back** doesn't see the login form with a top bar showing their name (a bug found during testing).
- `inject()` works inside guard functions because the router runs them in an injection context.
- **Important:** guards are a **user-experience feature, not security.** Anyone can edit JavaScript in their browser; the real protection is the backend's `401`/`403`.

---

## 20. The application shell

`app.ts`:
```ts
@Component({ imports: [RouterOutlet], selector: 'app-root', styleUrl: './app.css', templateUrl: './app.html' })
export class App { protected readonly auth = inject(AuthService); }
```
- `imports: [RouterOutlet]`: a standalone component must declare what its template uses (`<router-outlet>`).
- `protected`: usable from the template but not from outside the class.

`app.html`:
```html
@if (auth.user(); as utilisateur) {
  <header class="barre">
    <strong>Saisie des notes</strong>
    <span class="etablissement">Faculté de Droit de Sfax</span>
    <span class="espace"></span>
    <span>{{ utilisateur.prenom }} {{ utilisateur.nom }}</span>
    <button type="button" class="secondaire" (click)="auth.logout()">Déconnexion</button>
  </header>
}
<router-outlet />
```
- `@if (auth.user(); as utilisateur)`: shows the top bar **only when someone is logged in**, and gives the value an alias (`utilisateur`) valid inside the block. `auth.user()` is a signal read: when it changes, the bar appears/disappears.
- `(click)="auth.logout()"`: an event binding.
- **`<router-outlet />`** is a placeholder: the component of the current route (home, login, matières, notes) is rendered here. The top bar stays; only the outlet content changes.
- `app.css`: flexbox for the bar; `.espace { flex: 1 }` pushes the name and button to the right; the establishment name is hidden under 40rem wide (phones).

---

## 21. The home page

`accueil.ts` is an empty component class with `imports: [RouterLink]`: the page is pure content.

```html
<main class="accueil">
  <header class="officiel">
    <div class="armoiries"><img src="armoiries.png" width="90" height="105" alt="Armoiries de la République Tunisienne" /></div>
    <p class="republique">République Tunisienne</p>
    <p class="ministere">Ministère de l’Enseignement Supérieur et de la Recherche Scientifique</p>
    <p class="separateur" aria-hidden="true">— • —</p>
    <p class="universite">Université de Sfax</p>
    <p class="separateur" aria-hidden="true">— • —</p>
    <p class="faculte">Faculté de Droit de Sfax</p>
  </header>
  <section class="application" aria-labelledby="titre-application">
    <h1 id="titre-application">Saisie des notes</h1>
    <p>Espace réservé aux enseignants : ...</p>
    <a class="bouton" routerLink="/login">Se connecter</a>
  </section>
</main>
```
- The text reproduces the official letterhead. `<main>`, `<header>`, `<section>` are **semantic elements** (landmarks for screen readers).
- `width`/`height` on the `<img>` reserve the space before the image loads (no layout jump). `alt` describes the image for screen readers.
- `aria-hidden="true"` hides the decorative `— • —` separators from screen readers; `aria-labelledby` names the section after its `<h1>`.
- **`routerLink="/login"`** on an `<a>` makes a **client-side navigation** (no page reload) while keeping a real `href` (open in new tab works). It's a link, not a button, because it *navigates*; `a.bouton` in `styles.css` gives it the button look.
- CSS: a red top border (the flag colour), the emblem on a **fixed white tile** (the image isn't transparent, so it must stay readable in dark mode), centred column.

---

## 22. The login page

`login.ts`:
```ts
protected readonly form = inject(NonNullableFormBuilder).group({
  email: ['', [Validators.required, Validators.email]],
  password: ['', Validators.required],
});
protected readonly erreur = signal<string | null>(null);
protected readonly enCours = signal(false);
```
- **Reactive forms**: the form is an object in TypeScript. `NonNullableFormBuilder` builds controls whose value is never `null` (a reset returns them to `''`), which keeps the types simple (`string`, not `string | null`).
- `Validators.required` / `Validators.email` are **client-side format checks**.
- `erreur` (message to show) and `enCours` (request in flight) are signals so the template reacts to them.

```ts
protected seConnecter(): void {
  if (this.form.invalid) { this.form.markAllAsTouched(); return; }
  this.enCours.set(true); this.erreur.set(null);
  const { email, password } = this.form.getRawValue();

  this.auth.login(email, password).subscribe({
    next: () => void this.router.navigateByUrl('/matieres', { replaceUrl: true }),
    error: (e: HttpErrorResponse) => {
      this.enCours.set(false);
      this.erreur.set(e.status === 401 ? 'Email ou mot de passe incorrect' : 'Serveur injoignable, réessayez dans un instant');
    },
  });
}
```
- Invalid form → `markAllAsTouched()` makes every field display its error, and we stop **without calling the server**.
- `.subscribe({ next, error })`: **the request is sent here** (cold Observable). `next` runs on success; `error` on failure.
- **`replaceUrl: true`**: navigate by *replacing* the `/login` history entry instead of adding one, so **Back** after login doesn't return to the login form.
- On success `enCours` is left `true` on purpose: we're leaving the page.
- Login is the one place that distinguishes **401** (wrong credentials → specific message) from other failures (server unreachable).

`login.html` highlights:
- `[formGroup]="form"` and `(ngSubmit)="seConnecter()"`: binds the form object and handles submission. **`novalidate`** turns off the browser's own validation bubbles so our French messages are the only ones.
- `formControlName="email"` links each input to its control. `type="email"` / `type="password"` and `autocomplete="username"` / `"current-password"` let **password managers** work properly.
- `@if (form.controls.email.touched && form.controls.email.invalid) { <p class="champ-erreur">...</p> }`: an error appears only **after the user has visited the field** (not while they're still typing their first character).
- `@if (erreur(); as message) { <p class="erreur" role="alert">{{ message }}</p> }`: `role="alert"` makes screen readers announce the message immediately.
- `<button type="submit" [disabled]="enCours()">{{ enCours() ? 'Connexion…' : 'Se connecter' }}</button>`: prevents double submission and gives feedback.

---

## 23. The matieres page

```ts
constructor() {
  this.api.matieres().subscribe({
    next: (matieres) => {
      this.matieres.set([...matieres].sort((a, b) => a.code.localeCompare(b.code)));
      this.chargement.set(false);
    },
    error: (e) => { this.erreur.set(messageErreur(e)); this.chargement.set(false); },
  });
}
```
- The request is sent when the component is created (its constructor). Three signals model the page's **states**: `chargement` (loading), `erreur`, `matieres`.
- `[...matieres].sort(...)`: sort a **copy** (never mutate data you didn't create); `localeCompare` sorts text correctly (accents, case).

Template:
```html
@if (chargement()) { <p class="etat">Chargement…</p> }
@else if (erreur(); as message) { <p class="erreur" role="alert">{{ message }}</p> }
@else if (matieres().length === 0) { <p class="etat">Aucune matière ne vous est affectée pour le moment.</p> }
@else {
  <ul class="grille">
    @for (matiere of matieres(); track matiere.id) {
      <li><a class="carte-matiere" [routerLink]="['/matieres', matiere.id, 'notes']">...
```
- A chain of exclusive states: **loading → error → empty → data**. Handling *every* state (especially empty and error) is what separates a demo from a finished screen.
- `@for (... ; track matiere.id)`: `track` tells Angular how to identify each item, so when the list changes it updates only what changed instead of rebuilding everything.
- `[routerLink]="['/matieres', matiere.id, 'notes']"`: the array form builds `/matieres/1/notes` safely (each segment encoded).
- `@if (matiere.cloturee) { <span class="badge">Clôturée</span> }` shows the closed status. `matieres.css` lays the cards out with CSS grid.

---

## 24. The notes page

This is the most important page: it **reads**, **creates**, **edits** and **closes**.

### 24.1 The component's state

```ts
interface FormulaireOuvert { note: Note | null; }
```
The form's state has **three** possible values: closed (`null` for the whole signal), open to *create* (`{ note: null }`), open to *edit* (`{ note: theNote }`). Wrapping in an object lets us tell "closed" from "open in creation mode".

```ts
readonly id = input.required<string>();

protected readonly matiere = signal<Matiere | null>(null);
protected readonly notes = signal<Note[]>([]);
protected readonly etudiants = signal<Etudiant[]>([]);
protected readonly chargement = signal(true);
protected readonly erreur = signal<string | null>(null);
protected readonly formulaire = signal<FormulaireOuvert | null>(null);
protected readonly succes = signal<string | null>(null);
protected readonly erreurAction = signal<string | null>(null);
protected readonly clotureDemandee = signal(false);
```
- `id = input.required<string>()`: the route parameter (see `withComponentInputBinding()`); `required` means the compiler complains if it isn't provided. It's a **string**: the URL has no types.
- One signal per piece of screen state. `erreur` = the page failed to load; `erreurAction` = an action (closing) failed; `succes` = a confirmation.

```ts
protected readonly etudiantsSansNote = computed(() => {
  const notes = this.notes();
  return this.etudiants().filter((e) => !notes.some((n) => n.etudiantId === e.id));
});
```
- **Derived data**: students that don't have a note yet. The backend already returns only *this teacher's* students; this removes those who already have a note (a student can only have one). `computed` recalculates only when `notes` or `etudiants` change, and the dropdown updates by itself after each save. This mirrors the backend's uniqueness rule for convenience, but the backend still enforces it.

### 24.2 Loading

```ts
constructor() { effect(() => this.charger(Number(this.id()))); }
```
- An **`effect`** re-runs when the signals it *reads synchronously* change. It reads `this.id()`. So the page loads on creation **and again whenever the id changes** (Angular reuses the same component instance when only the parameter of the same route changes, e.g. `/matieres/1/notes` → `/matieres/2/notes`).

```ts
private charger(id: number): void {
  this.chargement.set(true); this.erreur.set(null); this.formulaire.set(null);
  this.succes.set(null); this.erreurAction.set(null); this.clotureDemandee.set(false);

  forkJoin({ matiere: this.api.matiere(id), notes: this.api.notes(id), etudiants: this.api.etudiants(id) })
    .subscribe({
      next: ({ matiere, notes, etudiants }) => { ...set the signals...; this.chargement.set(false); },
      error: (e) => { this.matiere.set(null); this.erreur.set(messageErreur(e)); this.chargement.set(false); },
    });
}
```
- First it **resets** transient state so nothing stale leaks from the previous matière.
- **`forkJoin`** sends the three requests **in parallel** and emits one combined result when **all** have completed; if **any** fails, it fails immediately with that error and the others are cancelled.
- That's why a **403** on the matière (a teacher opening another teacher's matière by URL) simply shows the backend's message and no table: the error path runs, `matiere` is `null`.
- Sorting happens client-side (`trier`: by student name, on a copy).

### 24.3 Actions

```ts
protected onEnregistre(note: Note): void {
  const modifiee = this.notes().some((n) => n.id === note.id);
  this.notes.update((notes) => this.trier([...notes.filter((n) => n.id !== note.id), note]));
  this.formulaire.set(null);
  this.succes.set(`Note de ${note.etudiantPrenom} ${note.etudiantNom} ${modifiee ? 'modifiée' : 'enregistrée'}`);
}
```
- Called when the child form reports a saved note. `filter` removes any old version with the same id, then the saved note is added and the list re-sorted: **one code path handles both creating and updating**.
- `notes.update(fn)` computes the new value from the old one (never mutate the array in place: signals detect changes by reference).
- The list is updated **locally** (no reload): instant feedback; the trade-off is that it wouldn't show changes made by someone else meanwhile.
- `ajouter()` / `modifier(note)` open the form (and clear old messages and any pending closure confirmation); `fermerFormulaire()` closes it.

**Closing a matière is a three-step flow** — `demanderCloture()` shows an inline confirmation, `annulerCloture()` hides it, `confirmerCloture()` sends the request:

```ts
this.api.cloturer(Number(this.id())).subscribe({
  next: (matiere) => { this.matiere.set(matiere); this.clotureDemandee.set(false); this.succes.set('Matière clôturée : ...'); },
  error: (e) => { this.clotureDemandee.set(false); this.erreurAction.set(messageErreur(e)); },
});
```
The confirmation exists because the action is **irreversible**. On success the backend returns the updated matière (`cloturee: true`); putting it into the `matiere` signal is enough to make the badge appear and **all action buttons disappear** (the template reads `m.cloturee`).

### 24.4 The template, line by line

```html
<a class="retour" routerLink="/matieres">← Mes matières</a>
@if (chargement()) { ... } @else if (erreur(); as message) { ... } @else if (matiere(); as m) {
```
Same state chain as the matières page; `matiere(); as m` gives the loaded matière the alias `m`.

```html
@if (!m.cloturee) {
  <div class="barre-actions">
    <button type="button" (click)="ajouter()" [disabled]="etudiantsSansNote().length === 0">Ajouter une note</button>
    <button type="button" class="secondaire" (click)="demanderCloture()">Clôturer la matière</button>
    @if (etudiantsSansNote().length === 0) { <span class="aide">Tous vos étudiants ont déjà une note.</span> }
  </div>
} @else { <p class="aide">Matière clôturée : aucune saisie ni modification possible.</p> }
```
- Actions exist **only while the matière is open**. "Ajouter" is disabled (with an explanation) when there is nobody left to grade.

```html
@if (succes(); as message) { <p class="succes" role="status">{{ message }}</p> }
@if (erreurAction(); as message) { <p class="erreur" role="alert">{{ message }}</p> }
```
`role="status"` = polite announcement (success); `role="alert"` = assertive (error).

```html
@if (clotureDemandee()) { <div class="confirmation" role="alertdialog" aria-label="Confirmer la clôture"> ... }
@if (formulaire(); as f) {
  <app-note-formulaire [matiereId]="m.id" [etudiants]="etudiantsSansNote()" [note]="f.note"
                       (enregistre)="onEnregistre($event)" (annule)="fermerFormulaire()" />
}
```
- The child component is created only while `formulaire()` is set. **Data flows down through `[inputs]`, events flow up through `(outputs)`**: the parent owns the data; the child never modifies the list, it just announces `enregistre` with the saved note.
- `$event` is the value emitted by the output.

The table: `@for (note of notes(); track note.id)`; `{{ note.valeur | number: '1.2-2' }}` (min 1 digit before, exactly 2 after the decimal separator, French: `14,50`); `{{ note.dateSaisie | date: 'dd/MM/yyyy HH:mm' }}`; a `@if (note.dateModification) {...} @else { — }` shows a dash for "never modified"; the "Modifier" column and button render only `@if (!m.cloturee)`; `[attr.aria-label]="'Modifier la note de ' + ..."` gives each identical "Modifier" button a unique accessible name (`attr.` is needed because `aria-label` is an HTML *attribute*, not a DOM property).

---

## 25. The note form component

One component for **both** adding and editing, driven by its inputs.

```ts
readonly matiereId = input.required<number>();
readonly etudiants = input<Etudiant[]>([]);
readonly note = input<Note | null>(null);
readonly enregistre = output<Note>();
readonly annule = output<void>();
protected readonly modification = computed(() => this.note() !== null);
```
- Inputs: which matière, which students to offer, and the note to edit (`null` = creating). Outputs: `enregistre` (carries the saved `Note`) and `annule`. `input()`/`output()` are the modern function-based replacements for `@Input()`/`@Output()`.

### 25.1 Validation, level 1 (format)

```ts
protected readonly form = new FormGroup({
  etudiantId: new FormControl<number | null>(null, Validators.required),
  valeur: new FormControl<number | null>(null, [
    Validators.required, Validators.min(0), Validators.max(20),
    Validators.pattern(/^\d{1,2}(\.\d{1,2})?$/),
  ]),
});
```
- The client-side mirror of the backend's `NoteForm`: required, between 0 and 20, at most 2 decimals. The regex reads: one or two digits, optionally followed by a dot and one or two digits (`12`, `12.5`, `12.75` ✔; `12.755` ✘).
- The control names (`etudiantId`, `valeur`) are **deliberately identical to the backend field names**; that lets server-side field errors be attached to the right control (see `afficherErreur`).
- **Why validate twice?** The browser check gives instant feedback and saves a round trip; the backend check is the **authority** (the API can be called without our UI).

```ts
protected messageValeur(): string | null {
  const erreurs = this.form.controls.valeur.errors;
  if (!this.form.controls.valeur.touched || !erreurs) { return null; }
  if (erreurs['serveur']) return erreurs['serveur'];
  if (erreurs['required']) return 'La note est obligatoire';
  if (erreurs['min'] || erreurs['max']) return 'La note doit être comprise entre 0 et 20';
  return 'La note doit avoir au plus 2 décimales (ex. 12.75)';
}
```
Translates the control's **error keys** into French, in priority order, and only after the field was touched. The special key `serveur` holds a message that came from the backend.

### 25.2 Resetting when the mode changes

```ts
constructor() { effect(() => { const note = this.note(); untracked(() => this.initialiser(note)); }); }

private initialiser(note: Note | null): void {
  this.erreur.set(null);
  this.form.reset({ etudiantId: note?.etudiantId ?? null, valeur: note?.valeur ?? null });
  if (note) { this.form.controls.etudiantId.disable(); } else { this.form.controls.etudiantId.enable(); }
}
```
- Clicking "Modifier" on one row, then on another, **reuses the same component instance** with a different `note` input. The `effect` reacts to that input and refills the form (`reset` with the note's values, or empty for creation).
- **`untracked(...)`** runs the inner code *without* subscribing the effect to the signals read inside it (`erreur`, form state). Otherwise the effect would re-run every time those change, resetting the form while the user types.
- When editing, the student control is **disabled**: the student of an existing note doesn't change (the backend's `NoteModificationForm` only has `valeur`).

### 25.3 Submitting

```ts
protected soumettre(): void {
  this.erreur.set(null);
  if (this.form.invalid) { this.form.markAllAsTouched(); return; }

  const { etudiantId, valeur } = this.form.getRawValue();
  const note = this.note();
  const requete = note ? this.api.modifier(note.id, valeur!)
                       : this.api.saisir(this.matiereId(), { etudiantId: etudiantId!, valeur: valeur! });

  this.enCours.set(true);
  requete.subscribe({
    next: (enregistree) => { this.enCours.set(false); this.enregistre.emit(enregistree); },
    error: (e: HttpErrorResponse) => { this.enCours.set(false); this.afficherErreur(e); },
  });
}
```
- `form.invalid` ignores **disabled** controls, so the disabled student control doesn't block editing.
- **`getRawValue()`** includes disabled controls' values (plain `.value` would omit them).
- One of two requests is chosen (`PUT` to edit, `POST` to create); the `!` (non-null assertion) is safe because the form was just validated as complete.
- On success, the saved note is passed **up** with `enregistre.emit(...)`; the parent updates the table and closes the form.

### 25.4 Validation, level 2 (the backend's answer)

```ts
private afficherErreur(e: HttpErrorResponse): void {
  const champs = e.status === 400 ? (e.error?.champs as Record<string, string> | undefined) : undefined;
  if (!champs) { this.erreur.set(messageErreur(e)); return; }
  for (const [nom, message] of Object.entries(champs)) {
    const controle = this.form.get(nom);
    if (controle) { controle.setErrors({ serveur: message }); controle.markAsTouched(); }
    else { this.erreur.set(message); }
  }
}
```
Two kinds of backend errors, two displays:

| Backend answer | Meaning | Where the user sees it |
|---|---|---|
| **400** with `champs: { valeur: "..." }` | a field failed the backend's format validation | **under that field**: `setErrors({ serveur: message })` on the control named `valeur`; the `serveur` key is read by `messageValeur()` |
| **403 / 409 / 422** (a business rule) | e.g. colleague's student, duplicate, matière closed, student not enrolled | **above the buttons** in a red alert (`erreur` signal) via `messageErreur` |

Because the backend's French messages are shown as-is, the frontend never duplicates the business wording.

### 25.5 The template

```html
<form class="formulaire" [formGroup]="form" (ngSubmit)="soumettre()" novalidate>
  <h2>{{ modification() ? 'Modifier la note' : 'Ajouter une note' }}</h2>
  @if (note(); as n) { <p class="etudiant-fixe">{{ n.etudiantPrenom }} {{ n.etudiantNom }} (...)</p> }
  @else {
    <select id="etudiant" formControlName="etudiantId">
      <option [ngValue]="null" disabled>Choisir un étudiant…</option>
      @for (etudiant of etudiants(); track etudiant.id) {
        <option [ngValue]="etudiant.id">{{ etudiant.nom }} {{ etudiant.prenom }} ({{ etudiant.numInscription }})</option>
      }
    </select>
    ...
  }
  <input id="valeur" type="number" step="0.01" inputmode="decimal" formControlName="valeur" />
  ...
  <button type="submit" [disabled]="enCours()">{{ enCours() ? 'Enregistrement…' : 'Enregistrer' }}</button>
  <button type="button" class="secondaire" (click)="annule.emit()">Annuler</button>
```
- In edit mode the student is displayed as fixed text; in create mode it is a `<select>`.
- **`[ngValue]`** (instead of `value`) lets an `<option>` carry a real **number** or `null` rather than a string, so `etudiantId` is a `number` in the form. The first, `disabled` option is the placeholder (`null`).
- `type="number" step="0.01" inputmode="decimal"`: numeric input with 2-decimal steps and a numeric keyboard on phones. (The browser accepts a comma in French locales and hands Angular a proper number.)
- Two buttons: `type="submit"` triggers `(ngSubmit)`; `type="button"` never submits the form. `(click)="annule.emit()"` sends the "cancel" event up.
- Labels use `for="..."` matching input ids (clicking the label focuses the field, and screen readers announce it).

---

## 26. Styling

- **Global stylesheet (`styles.css`)** defines **CSS custom properties** (variables) on `:root`: `--bg`, `--surface`, `--text`, `--muted`, `--border`, `--primary`, `--danger`, `--ok`... Components use `var(--primary)` and never hard-code colours.
- **Dark mode** is one block: `@media (prefers-color-scheme: dark) { :root { --bg: #12161c; ... } }` redefines the variables when the operating system is in dark mode: no component needs to know. (The white tile behind the emblem is a deliberate exception.)
- Global base styles for `body`, `input`/`select`, `button`, `table`, plus shared classes used across pages: `.page`, `.badge`, `.erreur`, `.succes`, `.champ-erreur`, `.aide`, `.etat`, `a.bouton`, `button.secondaire`.
- **Component styles** (`login.css`, `notes.css`...) are **encapsulated**: Angular adds unique attributes so a rule in `notes.css` only affects `notes` elements; two components can both define `.actions` without clashing.
- **Layout**: CSS grid with `repeat(auto-fill, minmax(15rem, 1fr))` for the matière cards (as many columns as fit); flexbox for bars and button rows; `min-height: 100vh` to centre pages vertically; `overflow-x: auto` around the table so it scrolls sideways on narrow screens instead of breaking the page; one media query hides the establishment name on phones.
- **Accessibility**: `:focus-visible` outlines for keyboard users, colour contrast through the variables, semantic elements, `role`/`aria-*` on messages, labels tied to inputs.

---

## 27. Frontend tests

`ng test` runs **Vitest** with **jsdom** (a browser simulated in Node): **45 tests**, no real browser and no backend needed.

| File | Tests | What it proves |
|---|---|---|
| `auth.service.spec.ts` | 8 | login sends Basic and remembers the user; wrong login stores nothing; accented passwords work; the interceptor signs `/api` calls only, and only when logged in; a 401 during a session logs out and redirects to `/login`; logout clears everything and goes to `/` |
| `auth.guard.spec.ts` | 4 | `authGuard` and `guestGuard` redirect (or allow) correctly |
| `login.spec.ts` | 3 | empty form doesn't call the server; wrong credentials show the message; success navigates to `/matieres` |
| `accueil.spec.ts` | 3 | letterhead text, login link, image alt text |
| `matieres.spec.ts` | 5 | loading → sorted list; closed badge; empty state; server message on error; "server unreachable" message |
| `notes.spec.ts` | 12 | table content and French formatting; empty state; 403 shows the message; closed matière hides every action; the dropdown excludes students who have a note; adding a note updates the table and confirms; editing; the closing flow (confirm, cancel, server failure) |
| `note-formulaire.spec.ts` | 9 | empty/out-of-range/too-many-decimals input never reaches the server; correct request bodies (`POST`/`PUT`); backend 400 shown under the field; backend 409 shown as an alert; edit mode pre-fills and hides the student list; cancel |
| `app.spec.ts` | 1 | the root component is created |

Techniques you will see:
- **`TestBed.configureTestingModule({ imports: [Component], providers: [...] })`**: builds a mini Angular environment for one component.
- **`provideHttpClientTesting()` + `HttpTestingController`**: replaces the network. `expectOne(url)` asserts a request was made; **`.flush(body, {status})`** simulates the server's reply; `expectNone` asserts nothing was sent (proving format validation stops bad input before the network).
- **`vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true)`** and **`vi.fn()`**: observe calls without really navigating.
- **`fixture.componentRef.setInput('id', '1')`** sets a component input; **`await fixture.whenStable()`** waits for signals/effects to settle and the view to refresh.
- The notes tests register the **French locale** so they assert what users actually see (`13,63`-style formatting).

What isn't covered by committed tests: full end-to-end flows in a real browser. During development they were verified with scripted browser runs (login, add, edit, close, Back button), but those scripts aren't part of the project.

---

## 28. Recap of the frontend

### 28.1 What each user action calls

| UI action | HTTP call | Backend answers → what the UI does |
|---|---|---|
| Log in | `GET /api/me` with `Authorization` | 200 → store credentials, go to `/matieres`; 401 → "Email ou mot de passe incorrect"; other → "Serveur injoignable" |
| Open matières | `GET /api/matieres` | 200 → cards; error → message |
| Open a matière | `GET /api/matieres/{id}` + `/notes` + `/etudiants` (parallel) | 200 → header + table; 403 → the message ("pas affecté…"), no table |
| Add a note | `POST /api/matieres/{id}/notes` | 201 → row added; 400 → under the field; 403/409/422 → alert |
| Edit a note | `PUT /api/notes/{id}` | 200 → row updated; 403/409/422 → alert |
| Close a matière | `POST /api/matieres/{id}/cloturer` | 200 → badge, buttons removed; 403 → alert |
| Any call, 401 | — | interceptor logs out and redirects to `/login` |

### 28.2 Decisions in one table

| Decision | Reason |
|---|---|
| SPA with standalone components, no `NgModule` | modern Angular: less boilerplate, explicit dependencies per component |
| Signals + `computed` + `effect` (zoneless) | simple, precise reactivity; the view refreshes only when its data changes |
| One `NotesApi` service, relative `/api` URLs | one place for URLs; same code with the dev proxy and in production behind nginx |
| Dev-server proxy | same origin → no CORS setup; mirrors production |
| Interceptor for `Authorization` and 401 handling | components never deal with credentials; central session handling |
| Credentials in memory only | not readable from browser storage; the cost is that a reload logs you out |
| Guards (`authGuard`, `guestGuard`) | good navigation UX; **not** security (the backend is) |
| Lazy-loaded routes | small first download |
| Reactive forms with the same field names as the backend DTOs | server field errors map straight onto the right controls |
| Validate format in the browser, business rules only in the backend | instant feedback without duplicating rules that need the database |
| Backend messages displayed as-is | wording lives in one place |
| Parent owns data, child emits events (inputs down, outputs up) | predictable data flow, reusable form |
| Handle loading / error / empty states everywhere | complete UI, not just the happy path |
| Inline confirmation for the irreversible closing | avoids accidents; testable (unlike `window.confirm`) |
| CSS variables + `prefers-color-scheme` | theming and dark mode in one place |

### 28.3 Known limitations (say them before a reviewer does)

1. **A page reload logs the user out** (credentials only in memory). `sessionStorage` would survive reloads at some cost in security; a token (JWT) with a short lifetime would be the proper fix.
2. **HTTP Basic sends the password on every request.** Acceptable only over **HTTPS**; JWT would avoid it.
3. **Possible race when the id changes quickly:** `charger` subscribes without cancelling the previous request, so a slow reply for matière 1 could overwrite matière 2's data. The fix is a `switchMap` (cancels the previous request) instead of a bare `subscribe` inside an `effect`.
4. **The list is updated locally after a save** without reloading, so changes made elsewhere in the meantime aren't shown until the page is reopened (the backend's rules still prevent conflicting writes).
5. **Validation rules exist in two places** (browser and backend). If the grade range changed, both would need editing; the backend remains the authority.
6. **No end-to-end tests are committed**; only unit/component tests.
7. **Keyboard focus isn't moved** when the form opens or closes; an accessibility polish item.
8. **French only** (no i18n mechanism); no pagination or search (unnecessary for a class-sized list).
9. The emblem image is low resolution.

### 28.4 Questions you should be ready for

- **What is a signal, and how does it differ from an Observable?** A signal always has a current value and is read synchronously; it drives the view. An Observable is a stream over time, used here for HTTP (one value then complete).
- **Why standalone components? What replaced `NgModule`?** Each component lists its own `imports`; `app.config.ts` holds global providers.
- **What does an interceptor do here, and why not add the header in each call?** Central, cannot be forgotten, also handles 401.
- **Why is the HTTP call not sent until `subscribe`?** HttpClient returns cold Observables.
- **`forkJoin` vs three separate subscriptions?** Parallel requests, a single completion point, and one error path for the whole page.
- **What is `effect` for, and why `untracked` in the form?** Reacting to signal changes with side effects; `untracked` prevents the effect from depending on signals it merely touches.
- **Why are the guards not security?** The code runs in the user's browser and can be altered; the backend enforces access.
- **Why the proxy?** Same origin ⇒ no CORS; mirrors nginx in production.
- **Where are the credentials stored and why?** In memory only, to keep them out of browser storage.
- **How do backend validation errors reach the right field?** Same field names in the DTO and the form; `setErrors({ serveur: msg })` on the control.
- **Why `track` in `@for`?** So Angular can update only changed items.
- **How do you prevent a teacher from seeing another's students?** Not in the frontend: the backend filters by teacher; the frontend only displays what it receives.
- **What would you improve first?** `switchMap` for loading, token-based auth with a refresh flow, and an end-to-end test suite.
