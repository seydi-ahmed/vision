# 1) Contexte & Objectifs

**But**: Concevoir et réaliser une plateforme web de gestion de quincailleries (multi‑magasins) permettant :

* au **propriétaire** de gérer une ou plusieurs quincailleries, leurs produits et leurs gérants ;
* au **gérant** de gérer l’inventaire, les prix et la disponibilité ;
* au **visiteur** de parcourir publiquement les quincailleries et les produits.

**Objectif d’apprentissage**: Projet complet, moderne, orienté bonnes pratiques pour évoluer vers un niveau expert full‑stack Java/Angular (tests, CI/CD, sécurité, observabilité, qualité de code, accessibilité, i18n).

---

# 2) Périmètre fonctionnel

## 2.1 Rôles & droits (RBAC)

* **VISITOR** (anonyme) :

  * Voir la liste des quincailleries (nom, localisation, horaires, note moyenne).
  * Voir la fiche d’une quincaillerie (gérant, coordonnées, catégories, produits disponibles).
  * Voir la fiche d’un produit (prix, stock, magasins l’ayant en stock, marque, catégorie, images, description).
  * Rechercher/filtrer/ordonner (par prix, stock, catégorie, localisation).
* **MANAGER** (gérant, créé par un propriétaire) :

  * CRUD limité sur **Inventaire** de SA quincaillerie : stock, prix, seuil d’alerte.
  * CRUD sur **Produits** (métadonnées) si délégué par le propriétaire.
  * Import CSV d’inventaire, génération d’étiquettes (PDF/QR code).
  * Tableau de bord : alertes stock bas, Top ventes (si module ventes activé plus tard), dernières modifications.
* **OWNER** (propriétaire) :

  * CRUD **Quincailleries** (plusieurs magasins), **Managers**.
  * CRUD **Produits** globaux (catalogue de référence) + images.
  * Paramètres de l’**organisation** (logo, charte, adresses, devise, taxes par défaut).
  * Tableaux de bord consolidés (inventaire total, valorisation stock, anomalies).

> Optionnel (phase 2+) : module **commandes** B2B/B2C, listes d’envies, avis/notes, promotions, multi‑devises.

## 2.2 Parcours clés

* **Navigation**: Logo | Accueil | Dashboard (si connecté) | Inscription/Connexion/Déconnexion.
* **Dashboard OWNER**: carte d’identité du propriétaire, liste paginée de ses quincailleries, indicateurs (nb gérants, valorisation stock, produits en rupture, alertes).
* **Fiche Quincaillerie**: infos, gérant, liste produits (paginée, filtrable), adresse/Google Map\*, horaires.
* **Fiche Produit**: infos, catégorie, marque, images, prix & stock par quincaillerie.

\*L’intégration de carte peut être faite avec Leaflet/OSM (gratuit) ou Google Maps (payant au-delà d’un quota).

## 2.3 Recherche & filtres

* Recherche plein texte (produit, quincaillerie, marque, catégorie).
* Filtres par catégorie, disponibilité, fourchette de prix, localisation, tri (prix, stock, nom).

## 2.4 Internationalisation & accessibilité

* i18n (français par défaut, clé/valeurs extensibles).
* A11y (WCAG AA): contrastes, focus visibles, labels, ARIA.

## 2.5 Sécurité

* Auth JWT (access + refresh), mot de passe hashé (BCrypt), verrouillage après N tentatives.
* CSRF (non nécessaire en JWT pur sur API stateless), CORS restreint.
* Validation serveur et client, rate‑limiting, logs d’audit (qui a modifié quoi et quand).
* RBAC stricte au niveau des endpoints et au niveau données (vérifier tenant\_id et store\_id).

---

# 3) Modèle de données (ERD logique)

**Multi‑tenant**: Modèle *row‑level* avec `tenant_id` (l’« organisation » = propriétaire). Chaque ressource rattachée à un `tenant_id`. Les visiteurs accèdent aux données publiques filtrées par statut `ACTIVE`.

Entités principales :

* **User**(id, email, hashed\_password, roles\[OWNER|MANAGER], status, created\_at, last\_login)
* **OwnerProfile**(id, user\_id FK, tenant\_id, display\_name, logo\_url)
* **ManagerProfile**(id, user\_id FK, tenant\_id, store\_id FK, phone)
* **Store**(id, tenant\_id FK, name, slug, description, address\_id FK, manager\_id FK nullable, opening\_hours JSON, phone, email, is\_public, status)
* **Address**(id, line1, line2, city, state, country, postal\_code, lat, lng)
* **Category**(id, tenant\_id FK (ou global), name, slug, parent\_id nullable)
* **Brand**(id, tenant\_id FK (ou global), name)
* **Product**(id, tenant\_id FK, sku, name, slug, description, category\_id FK, brand\_id FK, images JSON, attributes JSON)
* **Inventory**(id, tenant\_id FK, store\_id FK, product\_id FK, price, stock, reorder\_level, updated\_by FK User, updated\_at)
* **AuditLog**(id, tenant\_id, user\_id, entity\_type, entity\_id, action, diff JSON, created\_at)

> Option global vs tenant pour **Category**/**Brand**: commencer global simple, passer multi‑tenant si besoin.

**Index & contraintes** :

* Unicité `(tenant_id, sku)` sur Product ; `(tenant_id, store_id, product_id)` sur Inventory.
* Index texte sur `Product.name/description`.
* Index géo sur `Address(lat,lng)` si géolocalisation/tri par proximité.

---

# 4) API REST (Spring Boot)

Base URL: `/api/v1`

## 4.1 Auth

* `POST /auth/register-owner` : créer un OWNER + tenant.
* `POST /auth/login` : email + password → tokens (access, refresh).
* `POST /auth/refresh` : renouveler access token.
* `POST /auth/logout` : invalider refresh token (token store en DB/Redis).

## 4.2 Owner & Manager

* `GET /me` : profil connecté.
* `POST /owners/stores` (OWNER) : créer une quincaillerie.
* `GET /owners/stores` (OWNER) : lister ses quincailleries.
* `PATCH /owners/stores/{id}` (OWNER) : MAJ.
* `POST /owners/stores/{id}/managers` (OWNER) : créer un manager (invite par email).
* `GET /owners/managers` (OWNER) : lister ses managers.

## 4.3 Catalogue & Inventaire

* `POST /catalog/products` (OWNER) : créer produit.
* `GET /catalog/products` : liste produits (OWNER voit tout ; VISITOR voit *public only* via `/public/products`).
* `GET /catalog/products/{id}` : détails.
* `PATCH /catalog/products/{id}` (OWNER/Manager si délégué).
* `POST /inventory` (MANAGER/OWNER) : créer/MAJ inventaire (upsert par `(store_id, product_id)`).
* `GET /inventory?storeId=&productId=` : lire inventaire.
* `POST /inventory/import` : upload CSV (sécuriser, valider schéma).

## 4.4 Public

* `GET /public/stores` : liste des quincailleries publiques (paginée, filtres, tri).
* `GET /public/stores/{slug}` : détails d’une quincaillerie.
* `GET /public/products` : recherche/filtre produits.
* `GET /public/products/{slug}` : détails + magasins en stock.

## 4.5 Utilitaires

* `GET /meta/categories` ; `GET /meta/brands` ; `GET /meta/config` (devise, format prix).
* `GET /health` ; `GET /version`.

### Conventions

* Pagination: `page`, `size`, `sort` (ex: `sort=price,asc`).
* Filtrage: par `q` (texte), `category`, `brand`, `minPrice`, `maxPrice`, `inStock`, `near` (lat,lng,radius).
* Erreurs JSON: `{ timestamp, status, error, message, path }`.

---

# 5) Frontend (Angular moderne)

## 5.1 Stack & bibliothèques

* Angular 17+ (Standalone APIs, Signals, routing standalone).
* Angular Material + TailwindCSS ; icônes Lucide ou Material Icons.
* Formulaires réactifs + Zod/Yup pour validation côté client.
* HTTP Interceptors (Auth, Error, Loading), Route Guards (auth, role, ownership).
* State management: Signals store (léger) + Query services ; NgRx possible si préférence.
* Internationalisation: `@ngx-translate/core` ou i18n Angular.
* Accessibilité: ESLint A11y, cdk‑a11y.

## 5.2 Structure projet (suggestion)

```
apps/web/ (si monorepo) ou ./
  src/app/
    core/ (services, interceptors, guards, config)
    shared/ (ui lib, composants réutilisables, pipes)
    features/
      public/
        home/, stores/, products/
      auth/
      dashboard/
        owner/ (stores, managers, analytics)
        manager/ (inventory)
    app.routes.ts
    app.config.ts
```

## 5.3 Routes & pages

* `/` Accueil (recherche, dernières quincailleries, catégories populaires)
* `/login`, `/register-owner`
* `/dashboard` (layout privé) → `/dashboard/owner` ou `/dashboard/manager`
* `/stores/:slug` (public)
* `/products/:slug` (public)

## 5.4 UI/UX

* Header sticky, thème clair/sombre, pagination infinie (CDK Virtual Scroll) pour listes.
* Cartes produits : image, nom, prix min, badge "En stock" ; clic → détail.
* Fiche quincaillerie : onglets *Aperçu*, *Produits*, *Infos*, *Gérant*.
* Tableaux (inventaire) avec tri, filtres, export CSV.

---

# 6) Backend (Java/Spring Boot)

## 6.1 Stack & modules

* Java 21, Spring Boot 3.x, Spring Security 6 (JWT), Spring Web, Spring Data JPA, Validation, Lombok, MapStruct, PostgreSQL, Flyway, OpenAPI (springdoc), Bean Validation (Jakarta), Bucket4j (rate limit), Testcontainers.
* Build: Maven ou Gradle.

## 6.2 Packaging (hexagonal light)

```
com.example.hardware
  domain/ (model, services)
  application/ (usecases)
  infrastructure/
    persistence/ (entities JPA, repos)
    security/
    web/ (controllers, DTO, mappers)
  config/
```

## 6.3 Multi‑tenant

* Filtre Hibernate/JPA par `tenant_id` (injecté via token). Vérification systématique en service.
* Stratégie simple row‑level (une seule base, schéma partagé). Option : évolutions futures vers schéma‑par‑tenant.

## 6.4 Sécurité

* JWT (Access 15 min, Refresh 7 jours). Rotation des refresh tokens (table `refresh_token`).
* Roles: `ROLE_OWNER`, `ROLE_MANAGER`. `VISITOR` = non authentifié.
* Method Security `@PreAuthorize("hasRole('OWNER')")` + vérifs de propriété (store belong to tenant).
* Politiques CORS par env.

## 6.5 Observabilité

* Logs JSON (Logback encoder), correlationId (MDC), traces OpenTelemetry (OTLP), métriques Micrometer/Prometheus, health checks Actuator.

---

# 7) Schéma SQL (extrait minimal)

```sql
CREATE TABLE tenant (
  id UUID PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE app_user (
  id UUID PRIMARY KEY,
  tenant_id UUID REFERENCES tenant(id),
  email CITEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  roles TEXT[] NOT NULL,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE store (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenant(id),
  name VARCHAR(140) NOT NULL,
  slug VARCHAR(160) UNIQUE NOT NULL,
  description TEXT,
  address_id UUID,
  opening_hours JSONB,
  manager_user_id UUID REFERENCES app_user(id),
  is_public BOOLEAN DEFAULT TRUE,
  status VARCHAR(20) DEFAULT 'ACTIVE'
);

CREATE TABLE category (
  id UUID PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(140) UNIQUE NOT NULL,
  parent_id UUID NULL
);

CREATE TABLE brand (
  id UUID PRIMARY KEY,
  name VARCHAR(120) UNIQUE NOT NULL
);

CREATE TABLE product (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenant(id),
  sku VARCHAR(64) NOT NULL,
  name VARCHAR(200) NOT NULL,
  slug VARCHAR(220) NOT NULL,
  description TEXT,
  category_id UUID REFERENCES category(id),
  brand_id UUID REFERENCES brand(id),
  images JSONB,
  attributes JSONB,
  UNIQUE (tenant_id, sku)
);

CREATE TABLE inventory (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenant(id),
  store_id UUID NOT NULL REFERENCES store(id),
  product_id UUID NOT NULL REFERENCES product(id),
  price NUMERIC(12,2) NOT NULL,
  stock INT NOT NULL,
  reorder_level INT DEFAULT 0,
  updated_by UUID REFERENCES app_user(id),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE (tenant_id, store_id, product_id)
);
```

---

# 8) Contrats d’API (exemples OpenAPI)

## 8.1 `POST /auth/login`

**Request**

```json
{ "email": "owner@ex.com", "password": "••••••" }
```

**Response**

```json
{
  "accessToken": "jwt...",
  "refreshToken": "jwt...",
  "expiresIn": 900
}
```

## 8.2 `GET /public/products?q=perceuse&category=outillage&inStock=true&sort=price,asc&page=0&size=20`

**Response (200)**

```json
{
  "content": [
    {
      "id": "...",
      "slug": "perceuse-xxx",
      "name": "Perceuse percussion 750W",
      "minPrice": 34990,
      "category": "Outillage électroportatif",
      "brand": "Bosch",
      "images": ["/cdn/p/1.jpg"]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 125
}
```

## 8.3 `GET /public/products/{slug}`

**Response (200)**

```json
{
  "id": "...",
  "name": "Perceuse percussion 750W",
  "description": "...",
  "category": {"id": "...", "name": "Outillage"},
  "brand": {"id": "...", "name": "Bosch"},
  "images": ["/cdn/p/1.jpg"],
  "stores": [
    {"storeId": "...", "storeName": "Quincaillerie A", "price": 34990, "stock": 12}
  ]
}
```

---

# 9) Sécurité & conformité (détaillée)

* **Authentification**: JWT signé (RSA), rotation refresh, liste de révocation.
* **Autorisations**: contrôles méthode + filtre tenant + contrôle propriété (ex: un MANAGER ne modifie que son `store_id`).
* **Validation**: DTO annotés (Jakarta Validation), messages internationalisés.
* **Protection**: Rate limit (Bucket4j), headers sécurité (CSP, HSTS, X‑Frame‑Options), taille max payload, upload virus‑scan (ClamAV en sidecar) si besoin.
* **Audits**: journaliser CRUD sensibles avec diff JSON.

---

# 10) Qualité & Tests

* **Backend**: tests unitaires (JUnit5/Mockito), tests d’intégration (Spring Boot + Testcontainers PostgreSQL), tests de contrat (Spring REST Docs/OpenAPI), mutation testing (PIT)
* **Frontend**: tests unitaires (Jest), tests composants (Testing Library), tests end‑to‑end (Cypress).
* **Qualimétrie**: ESLint + Prettier (FE), Checkstyle/Spotless (BE), SonarQube local, couverture cible ≥ 80%.

---

# 11) CI/CD & Déploiement

* **CI GitHub Actions** (ou GitLab CI): build, tests, lint, SCA (OWASP Dependency‑Check), build images Docker, publication.
* **Infra**: Docker Compose (dev), Kubernetes (prod) ou PaaS (Railway/Fly.io/Render). Base PostgreSQL managée.
* **Config**: 12‑factor, variables d’env, profils Spring `dev|staging|prod`.
* **CD**: déploiement blue/green, migrations Flyway auto.
* **Observabilité**: Prometheus/Grafana, OpenTelemetry → Tempo/Jaeger, logs centralisés (ELK ou Loki).

---

# 12) Roadmap pédagogique (itérative, 6 sprints)

**Sprint 1 – Bootstraps & Auth** (1‑2 semaines)

* Init monorepo (Nx) ou repos séparés.
* Backend: Spring Boot, Postgres, Flyway, Auth JWT, `/auth/*`, `/me`.
* Frontend: Angular shell, routing, formulaires Login/Signup Owner, guards, interceptors.

**Sprint 2 – Modèle & CRUD de base**

* Entités `Store`, `Product`, `Category`, `Brand`.
* Endpoints catalog & public (GET liste, détail). OpenAPI.
* FE: pages Accueil, liste stores, liste produits publiques.

**Sprint 3 – Inventaire & Dashboard**

* `Inventory` CRUD + import CSV.
* Tableaux de bord Owner/Manager (kpis basiques, alertes stock bas).

**Sprint 4 – Qualité & Sécurité avancée**

* Rate limiting, audit logs, tests d’intégration + E2E.
* Accessibilité et i18n, thème dark.

**Sprint 5 – Observabilité & CI/CD**

* Pipelines CI complets, images Docker, staging déployé.
* Monitoring, traces, alerting basique.

**Sprint 6 – Optimisations & UX**

* Recherche full‑text (Postgres `tsvector`) + filtres.
* Virtual scrolling, lazy loading, perf back (requêtes N+1), cache HTTP/ETag.

---

# 13) Exigences non‑fonctionnelles (NFR)

* **Performance**: TTFB < 300 ms sur endpoints clés (cache & index), P95 < 800 ms.
* **Scalabilité**: horizontal back stateless, DB optimisée, CDN pour assets.
* **Disponibilité**: 99.5% cible (staging/prod).
* **Sécurité**: OWASP Top 10 pris en compte.
* **Accessibilité**: WCAG 2.1 AA.
* **Portabilité**: Dockerisé, 12‑factor.

---

# 14) Formats d’import/export

* **Import Inventaire (CSV)** : colonnes `sku;store_slug;price;stock;reorder_level` ; validation stricte, rapport d’erreurs.
* **Export** : CSV inventaire, PDF étiquettes (QR: `hardware://product/{id}` ou URL publique).

---

# 15) Exemple de structures de dossiers

## Backend (Maven)

```
src/main/java/com/example/hardware/
  config/
  domain/
  application/
  infrastructure/
    persistence/
    security/
    web/
src/main/resources/
  db/migration/ (Flyway)
  application.yml
```

## Frontend (Angular)

```
src/app/
  core/
    interceptors/
    guards/
    services/
    config/
  shared/
    components/
    pipes/
    ui/
  features/
    public/
      stores/
      products/
    auth/
    dashboard/
      owner/
      manager/
```

---

# 16) Check‑list de livraison

* [ ] OpenAPI 3.1 publié `/swagger-ui` + fichier `openapi.json`
* [ ] 80% coverage back/front
* [ ] Scripts `make dev|test|lint|build|start` (ou npm scripts)
* [ ] Docker Compose (api, web, db, pgadmin)
* [ ] Comptes de démo (owner, manager)
* [ ] Données seed (quelques catégories, marques, produits)
* [ ] Tableaux de bord fonctionnels
* [ ] Import CSV & rapport d’erreurs
* [ ] Politique CORS, headers sécurité, rate limit
* [ ] Logs d’audit vérifiés

---

# 17) User stories (extraits)

* En tant que **visiteur**, je veux filtrer par *catégorie* et *prix* pour trouver rapidement un produit.
* En tant que **gérant**, je veux importer un CSV d’inventaire pour gagner du temps.
* En tant que **propriétaire**, je veux voir les produits en rupture sur tous mes magasins.
* En tant que **propriétaire**, je veux inviter un gérant par email et définir ses permissions.

**Critères d’acceptation** détaillés (exemple, import CSV):

* Si une ligne contient un `sku` inconnu → marquer erreur `UNKNOWN_SKU`.
* Si `price < 0` ou `stock < 0` → erreur de validation.
* À la fin, afficher le nombre de lignes importées, d’erreurs, et fournir un CSV des erreurs.

---

# 18) Extensions futures (idées)

* Module **Commandes** (panier public / pro), paiements (Stripe), facturation PDF.
* **Promotions** & prix par segment (pro/retail).
* **Multi‑devises** + taux FX.
* **Intégration** marketplace (Jumia, Shopify) via connecteurs.
* **Mobile** : PWA + mode hors‑ligne pour inventaire.

---

# 19) Conseils d’expert & bonnes pratiques

* Utiliser DTO distincts des entités JPA (MapStruct) pour éviter l’exposition directe du modèle.
* Règles de validation côté client et serveur alignées ; messages d’erreurs utilisables.
* Limiter les N+1 en JPA (fetch graphs, projections).
* Paginer systématiquement les listes.
* Mettre en place un *Feature Toggle* simple (ex. `ff4j` ou config maison) pour activer des modules (commandes, promos).
* Mesurer (APM) avant d’optimiser ; profiler requêtes lentes.

---

# 20) Annexes

## 20.1 Exemple d’Interceptor Angular (pseudo‑code)

```ts
export function authInterceptor(req: HttpRequest<any>, next: HttpHandler) {
  const token = authStore.accessToken();
  const cloned = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` }}) : req;
  return next.handle(cloned).pipe(
    catchError(err => { /* route vers /login si 401 */ return throwError(() => err); })
  );
}
```

## 20.2 Politique de slugs

* Générer à partir du nom, normaliser (minuscules, accents retirés), vérifier unicité.

## 20.3 Règles d’URL publiques

* `/stores/:slug` et `/products/:slug` stables, SEO‑friendly, méta‑tags (Open Graph) pour partage.

---

**Fin du cahier des charges v1.**
