# 🧭 Vision

Construire **SaaS multi‑tenant de gestion de projets & facturation** (Projects, Tasks, Time‑Tracking, Invoicing, Payments, Notifications), production‑ready. L’objectif est d’exercer toutes les compétences full‑stack Angular/Java (Spring Boot).

---

# 🧩 Périmètre fonctionnel

## MVP (Sprints 1 → 4)

* Authentification / Autorisation (JWT) + rôles (OWNER, ADMIN, MEMBER).
* Organisations (tenants), équipes, membres.
* Projets, tâches, time entries (pointages de temps).
* Invoices PDF (génération), envoi par email.
* Tableau de bord personnel.

## Features avancées (Sprints 5 → 12)

* Paiements Stripe (cartes), statut facture payé/impayé.
* Notifications temps réel (WebSocket) + emails asynchrones.
* Recherche plein‑texte (Elasticsearch) + filtres complexes.
* Fichiers (uploads vers S3/MinIO).
* i18n (fr/en), thèmes light/dark.
* Audit log & RGPD (export/suppression).
* Observabilité (metrics, logs, traces), cache (Redis), rate‑limiting.
* CI/CD, Docker/K8s, Testcontainers, Cypress e2e, qualité (coverage, Sonar‑like).

---

# 🏗️ Architecture

* **Frontend** : Angular 17+, Standalone Components, Nx (optionnel), modules `core/`, `shared/`, features `auth/`, `projects/`, `invoices/`, `settings/`.
* **Backend** : Spring Boot 3.3+, Java 21, REST (OpenAPI), JPA/Hibernate, MapStruct, Validation, Spring Security (JWT), Spring Scheduling.
* **DB** : PostgreSQL 16.
* **Infra Dev** : Docker Compose (postgres, redis, mailhog, minio, elasticsearch/kibana).
* **Cache/queues** : Redis (pub/sub + cache), éventuellement Kafka (avancé).
* **Build/Qualité** : Maven ou Gradle, Checkstyle/Spotless, Jacoco, ESLint/Prettier, Husky.
* **Tests** : JUnit5 + Spring Boot Test + Testcontainers (backend), Jest ou Karma (Angular), Cypress e2e.

---

# 🗺️ Plan par sprints (guide pas‑à‑pas)

## Sprint 1 — Bootstrapping + Auth (JWT) + Multi‑tenant (basique)

**Objectifs**

1. Scaffolding back/front, 2) Login/password, 3) JWT stateless, 4) Modèle tenant simple.

**Étapes**

1. **Repo & conventions**

   ```bash
   mkdir saas-pro && cd saas-pro && git init
   ```

   * Branches: `main` (stable), `dev` (intégration), feat branches.

2. **Backend — Spring Boot**

   * Crée via Spring Initializr: Spring Web, Spring Security, Spring Data JPA, Validation, Lombok, PostgreSQL, MapStruct, Springdoc OpenAPI, Testcontainers.
   * Structure packages: `com.acme.saas` → `config`, `security`, `tenant`, `user`, `project`, `time`, `invoice`, `common`.

3. **DB & Docker Compose (dev)**
   `docker-compose.yml` minimal (Postgres, Redis, Mailhog):

   ```yaml
   services:
     db:
       image: postgres:16
       environment:
         POSTGRES_DB: saas
         POSTGRES_USER: saas
         POSTGRES_PASSWORD: saas
       ports: ["5432:5432"]
       volumes: ["pgdata:/var/lib/postgresql/data"]
     redis:
       image: redis:7
       ports: ["6379:6379"]
     mailhog:
       image: mailhog/mailhog
       ports: ["8025:8025"]
   volumes:
     pgdata:
   ```

4. **Config Spring** (`application.yml`)

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/saas
       username: saas
       password: saas
     jpa:
       hibernate:
         ddl-auto: update
       properties:
         hibernate:
           format_sql: true
     jackson:
       serialization:
         WRITE_DATES_AS_TIMESTAMPS: false
   server:
     port: 8080
   app:
     jwt:
       secret: change-me
       expiration: 3600000 # 1h
   ```

5. **Modèle de données (base)**

   * `Organization(id, name, slug, createdAt)`
   * `User(id, email, passwordHash, fullName, enabled, createdAt)`
   * `Membership(id, organization_id, user_id, role[OWNER|ADMIN|MEMBER])`
   * `Project(id, organization_id, name, description, status)`

6. **Sécurité — JWT**

   * Passwords `BCryptPasswordEncoder`.
   * Endpoints publics: `/api/auth/register`, `/api/auth/login`.
   * Filtre `JwtAuthenticationFilter` (lecture header `Authorization: Bearer <token>`).
   * `SecurityConfig` stateless, CORS ouvert pour dev.

7. **Back — Endpoints init**

   * `POST /api/auth/register` → crée user + org + membership OWNER.
   * `POST /api/auth/login` → email+password → JWT.
   * `GET /api/me` → profil + organisations.
   * `GET /api/projects` (scopé à l’org courante, multi‑tenant via header `X-Org-Id` ou subdomain > avancé plus tard).

8. **Frontend — Angular**

   ```bash
   npm i -g @angular/cli
   ng new web --routing --style=scss
   cd web && ng g environments
   ```

   * Bibliothèques: `ngx-toastr`, `@ngrx/store` (optionnel), `ngx-translate/core`, `zod` (validation), `date-fns`.
   * Dossier: `src/app/core`, `src/app/shared`, `src/app/features/{auth,projects,invoices,settings}`.

9. **Angular — Auth**

   * `AuthService` (login/register, stockage JWT dans `localStorage`).
   * `JwtInterceptor` (ajoute `Authorization: Bearer`).
   * `AuthGuard` (redirige vers `/login`).
   * Pages: `LoginComponent`, `RegisterComponent`, `DashboardComponent`.

10. **Contrat d’API (OpenAPI)**
    `springdoc-openapi-starter` → Swagger UI sur `/swagger-ui.html`.

11. **Tests init**

    * Backend: test d’intégration login/register (Testcontainers Postgres).
    * Frontend: tests unitaires `AuthService`, `LoginComponent`.

**Critères d’acceptation**

* On peut créer un compte, se connecter, voir son dashboard, créer des projets (CRUD minimal) dans son organisation.

---

## Sprint 2 — Time‑Tracking & Tâches

**Objectifs** : CRUD Tâches, Time Entries; totaux journalier/hebdo; validation.

* Modèles: `Task(id, project_id, title, status, assignee_id)`, `TimeEntry(id, task_id, user_id, startedAt, endedAt, minutes, note)`.
* Endpoints: CRUD, `GET /api/time/summary?from=&to=`.
* UI: page projet (liste tâches), panneau time‑tracking (start/stop), timesheet.
* Tests: règles (pas d’overlap de time entries), arrondi, permissions.

## Sprint 3 — Facturation & PDF

* Génération PDF (`OpenPDF`/`iText`), stockage S3/MinIO.
* Entités: `Client`, `Invoice`, `InvoiceItem`.
* Envoi email (Mailhog) avec lien public.

## Sprint 4 — Tableau de bord & Filtres

* Widgets: heures loggées, tâches en retard, revenus (fictifs).
* Recherche: filtres back (pageable, sort), spec JPA.

## Sprint 5 — Paiements Stripe

* Webhooks Stripe (paiement facture), statuts, pages publiques de paiement.

## Sprint 6 — Notifications temps réel

* Spring WebSocket (STOMP) + Angular RxJS; notifications: assignation, paiement reçu, commentaire tâche.

## Sprint 7 — Recherche avancée

* Sync vers Elasticsearch; recherche plein‑texte sur projets/tâches/clients.

## Sprint 8 — Fichiers & commentaires

* Uploads vers MinIO, pré‑signés; commentaires sur tâches (rich text markdown).

## Sprint 9 — i18n & Thème

* `@ngx-translate/core`, fichiers en/fr; thèmes SCSS, variables CSS.

## Sprint 10 — Sécurité & RGPD

* 2FA TOTP, export/suppression compte, audit trail (Spring Data Envers ou custom), rate limiting (Bucket4j).

## Sprint 11 — Observabilité

* Micrometer/Prometheus, logs JSON, tracing (OpenTelemetry), alerting basique.

## Sprint 12 — CI/CD & Scalabilité

* GitHub Actions (build/test, Docker, déploiement), Helm/Kubernetes (optionnel), blue‑green.

---

# 🔐 Backend — Exemples de code (Sprint 1)

## `SecurityConfig`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
        .anyRequest().authenticated()
      )
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
}
```

## `JwtAuthFilter` (extrait)

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
    String token = header.substring(7);
    String email = jwtService.extractUsername(token);
    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails user = userDetailsService.loadUserByUsername(email);
      if (jwtService.isTokenValid(token, user)) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
          user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(request, response);
  }
}
```

## Entités (extraits)

```java
@Entity @Table(name = "users")
@Getter @Setter
public class User {
  @Id @GeneratedValue private UUID id;
  @Column(nullable=false, unique=true) private String email;
  @Column(nullable=false) private String passwordHash;
  private String fullName; private boolean enabled = true;
  @CreationTimestamp private Instant createdAt;
}

@Entity
@Getter @Setter
public class Organization {
  @Id @GeneratedValue private UUID id;
  @Column(nullable=false, unique=true) private String name;
  @Column(nullable=false, unique=true) private String slug;
  @CreationTimestamp private Instant createdAt;
}

@Entity
@Getter @Setter
public class Membership {
  @Id @GeneratedValue private UUID id;
  @ManyToOne(optional=false) private Organization organization;
  @ManyToOne(optional=false) private User user;
  @Enumerated(EnumType.STRING) private Role role;
  public enum Role { OWNER, ADMIN, MEMBER }
}
```

## AuthController (extrait)

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
    return authService.register(req);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req);
  }
}
```

---

# 🖥️ Frontend — Exemples (Sprint 1)

## Interceptor JWT

```ts
@Injectable({ providedIn: 'root' })
export class JwtInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('jwt');
    if (!token) return next.handle(req);
    const cloned = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    return next.handle(cloned);
  }
}
```

## Guard simple

```ts
export const authGuard: CanActivateFn = () => {
  return !!localStorage.getItem('jwt');
};
```

## Routes (extrait)

```ts
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login.component') },
  { path: 'register', loadComponent: () => import('./features/auth/register.component') },
  { path: '', canActivate: [authGuard], children: [
      { path: '', loadComponent: () => import('./features/dashboard/dashboard.component') },
      { path: 'projects', loadChildren: () => import('./features/projects/routes') },
  ]},
];
```

---

# 🧪 Tests — Démarrage rapide

## Backend (Testcontainers)

```java
@Testcontainers
@SpringBootTest
class AuthIntegrationTest {
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
  @DynamicPropertySource static void props(DynamicPropertyRegistry r){
    postgres.start();
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }
  @Test void register_and_login_ok(){ /* ... */ }
}
```

## Frontend (Cypress — smoke test)

```bash
npx cypress open
```

Test: se connecter, voir dashboard, créer un projet.

---

# 🛡️ Sécurité & bonnes pratiques

* Stockage secrets via variables d’env (pas en `application.yml`).
* Hash: BCrypt (strength 10+) ; verrouillage compte après X échecs (Bucket4j).
* Validation côté back (Jakarta Validation) et front (zod/Angular forms).
* Pagination/tri systématiques, DTOs (ne pas exposer entités JPA).
* Logs JSON, corrélation `X-Request-Id`.
* Backups DB, migrations (Flyway) à partir du Sprint 2.

---

# ⚙️ CI/CD – GitHub Actions (exemple)

```yaml
name: ci
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - run: mvn -B -q -DskipTests=false verify
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: cd web && npm ci && npm run test -- --watch=false && npm run build
```

---

# 🧰 Premiers TODOs (checklist exécutables aujourd’hui)

* [ ] Initialiser repo, Docker Compose, Postgres up.
* [ ] Générer Spring Boot + config `application.yml` + entités User/Org/Membership.
* [ ] Implémenter SecurityConfig + JwtAuthFilter + AuthController.
* [ ] Exposer `/api/auth/register`, `/api/auth/login`, `/api/me`.
* [ ] Créer Angular app + Interceptor/Guard + pages Login/Register/Dashboard.
* [ ] Intégration: se connecter du front au back.
* [ ] Test e2e Cypress: login → dashboard → créer projet.

---

# 🔜 Étapes suivantes (quand Sprint 1 est vert)

* Flyway (migrations) + données de démo.
* CRUD complet Projects/Tasks/Time, règles métier & tests.
* Génération PDF factures + envoi email.

---

# ❓Besoin d’aide ciblée

Dis‑moi sur quelle partie tu veux que je développe du code complet (ex: `AuthService` Spring, `RegisterComponent` Angular, `docker-compose.yml` détaillé, Flyway, PDF, Stripe, WebSocket, Elasticsearch). Je peux te donner les fichiers complets prêts à coller.
