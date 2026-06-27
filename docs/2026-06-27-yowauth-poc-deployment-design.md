# Design — Déploiement POC de YowAuth (slice auth isolé)

- **Date** : 2026-06-27
- **Auteur** : Tchoutzine (avec Claude Code)
- **Contexte** : projet noté — Réseaux Intelligents (Master). Le serveur complet RT-Comops
  (~20 cores) est déployé ailleurs mais instable et hors du contrôle de l'auteur.
- **Objectif** : extraire le module d'authentification (`auth-core` / YowAuth) dans un
  repo autonome, le déployer **gratuitement** sur Render, et prouver via un POC que les
  endpoints d'authentification répondent en ligne.

## 0. Isolation vis-à-vis du projet de base (fork)

Le projet de base `KSM_Kernel_Layer` est un **fork** du dépôt amont. Tout le travail du POC
doit rester **strictement séparé** pour ne pas polluer le fork ni gêner les pulls amont.

Règles d'isolation :

- Ce POC vit dans un **dépôt git distinct**, dossier frère du fork :
  `c:\Users\Tchoutzine\Documents\GitHub\yowauth-poc\` (≠ `…\KSM_Kernel_Layer\`).
- **Aucun** fichier du POC n'est commité dans le fork. La spec de conception elle-même est
  hébergée ici, dans `yowauth-poc/docs/`.
- Les 5 cores nécessaires sont **copiés** (snapshot figé) depuis le fork dans ce repo ; ils ne
  sont pas référencés via chemin relatif vers le fork. Le POC est autonome et compilable seul.
- Remote GitHub propre au POC (≠ remote du fork).

## 1. Objectif et critères de succès

Livrer un repo `yowauth-poc` autonome qui :

1. compile et boote en **mode in-memory** (aucune base de données externe) via `mvn package`,
2. expose la surface d'authentification réelle de YowAuth,
3. est packagé en image Docker et déployé sur **Render** (URL HTTPS publique),
4. est accompagné d'un POC (script smoke + frontend **Next.js** déployé sur Vercel) qui
   démontre en live le flux complet.

**Critères de succès :**

- `register → login → token exchange (OIDC) → /.well-known/jwks.json → userinfo` réussissent
  contre l'URL Render publique.
- Le flux multi-organisation (`discover` / `select`, organisations dans `login` / `users/me`)
  retourne des organisations seedées (logique réelle, données simulées).
- Le POC Next.js, déployé sur Vercel, appelle l'API Render et affiche les réponses + le JWT décodé.

**Hors périmètre :** persistance PostgreSQL réelle, Liquibase, Kafka, Redis, Elasticsearch,
les ~15 autres cores métier, le flux MFA/OTP/reset (présents dans le code mais non requis
pour la démo notée).

## 2. Contrainte architecturale découverte

`auth-core` est un module d'un monolithe modulaire hexagonal ; seul `RT-comops-bootstrap`
est exécutable et il tire **tous** les cores. Extraire « juste l'auth » impose de fournir,
dans le classpath réduit, les beans dont la chaîne de sécurité de `kernel-core` a besoin à
la construction du contexte Spring.

Dépendances Maven compile d'`auth-core` : `common-core`, `kernel-core`, `actor-core`.

Beans requis au démarrage qui ne sont **pas** dans ces 3 modules :

| Bean / port (défini dans) | Implémentation réelle | Décision pour le slice |
| --- | --- | --- |
| `ReactivePermissionResolver` (kernel) | `RolesPermissionResolver` (roles-core) | **inclure roles-core** (ne dépend que de common + kernel, aucun cascade) |
| `OrganizationServiceRuntimeEntitlementDirectory` (kernel) | `OrganizationCore…` (bootstrap → org-core) | **stub in-memory** : toujours « entitled », pas de quota |
| `UserOrganizationAccessDirectory` (auth) | `OrganizationCoreUserOrganizationAccessDirectory` (bootstrap → org-core) | **adaptateur in-memory seedé** (organisations d'exemple par user) |
| `SignUpContextDirectory` (auth) | `OrganizationCoreSignUpContextDirectory` (bootstrap → org-core) | **adaptateur in-memory seedé** (code organisation d'exemple) |
| `AuthenticateClientApplicationUseCase` (kernel) | `ClientApplicationService` (kernel-core) | déjà présent ✓ |

**On n'inclut pas `organization-core`.** Les 3 ports ci-dessus liés à l'organisation sont
remplacés par des adaptateurs de sortie alternatifs — usage prévu de l'hexagonal. La logique
d'auth exercée reste 100 % réelle (signature RS256, tokens de contexte, résolution
multi-tenant) ; seules les **données** d'organisation sont simulées.

> Risque résiduel : d'autres ports de `kernel-core`/`actor-core` peuvent manquer au premier
> boot. Méthode : boot local → lire « no qualifying bean » → ajouter un stub in-memory dans
> `auth-poc-app` → répéter. L'auteur a choisi de s'engager **100 %** sur le slice pur (pas de
> repli sur l'app complète).

## 3. Périmètre du slice (modules clonés)

Repo `yowauth-poc/` contenant (cores **copiés** depuis le fork) :

| Module | Rôle |
| --- | --- |
| `RT-comops-common-core` | socle (`ApiResponse`, adresses/contacts) |
| `RT-comops-kernel-core` | sécurité multi-couches, JWT RS256, JWKS, ClientApplication |
| `RT-comops-actor-core` | dépendance compile d'auth-core |
| `RT-comops-roles-core` | fournit `ReactivePermissionResolver` |
| `RT-comops-auth-core` | **module cible** : register/login/OIDC/token/context |
| `auth-poc-app` *(nouveau)* | runner Spring Boot exécutable + adaptateurs in-memory |

Les dossiers des cores sont copiés tels quels (groupId `yowyob.comops.api`, version
`0.1.0-SNAPSHOT` conservés). Le `pom.xml` parent est copié puis sa liste `<modules>` est
réduite aux 5 cores + `auth-poc-app`. La `dependencyManagement` et la config plugin du parent
sont conservées intactes.

## 4. Le module `auth-poc-app`

Unité unique, responsabilité unique : assembler et faire tourner le slice auth.

```
auth-poc-app/
├── pom.xml                       # starters Spring Boot + 5 cores du slice
└── src/main/
    ├── java/yowyob/comops/api/pocapp/
    │   ├── AuthPocApplication.java          # @SpringBootApplication(scanBasePackages="yowyob.comops.api")
    │   └── config/
    │       ├── InMemoryOrganizationAdaptersConfig.java   # 3 beans in-memory ci-dessous
    │       └── (stubs additionnels découverts au boot)
    └── resources/
        └── application.yml       # profil in-memory par défaut
```

**`pom.xml`** : reprend les starters du bootstrap réel nécessaires uniquement à l'auth —
`webflux`, `security`, `validation`, `mail`, `actuator`, `springdoc-openapi-starter-webflux-ui`,
`micrometer-registry-prometheus` — **sans** `data-r2dbc`, `data-redis-reactive`,
`data-elasticsearch`, `r2dbc-postgresql`, `liquibase`. Plus les 5 modules du slice. Plugin
`spring-boot-maven-plugin` avec `mainClass = …pocapp.AuthPocApplication` et goal `repackage`.

**`AuthPocApplication`** : identique au bootstrap réel (`scanBasePackages = "yowyob.comops.api"`,
`@EnableScheduling`, `@EnableAspectJAutoProxy`), mais avec un classpath réduit donc seuls les
beans des 5 cores sont scannés.

**`InMemoryOrganizationAdaptersConfig`** — 3 beans :

1. `OrganizationServiceRuntimeEntitlementDirectory` → renvoie
   `OrganizationServiceRuntimeEntitlement(serviceCode, effective=true, quota=null, window=null)`.
2. `UserOrganizationAccessDirectory.listUserOrganizations(tenantId, userId)` → `Flux` de 1–2
   `UserOrganizationAccess` seedés (organizationId fixe, code/shortName/longName/services d'exemple).
3. `SignUpContextDirectory.findByOrganizationCode(code)` → `Flux` d'un `SignUpContext` seedé
   quand `code` correspond à l'organisation d'exemple, vide sinon.

Les UUID seedés (tenant, organisation) sont des constantes documentées, réutilisées par le
script smoke et le frontend.

## 5. Configuration runtime (`application.yml`)

Profil in-memory **par défaut** (pas besoin de `SPRING_PROFILES_ACTIVE`), repris de
`application-test-memory.yml` du bootstrap :

- `spring.liquibase.enabled: false`
- `spring.autoconfigure.exclude` : R2DBC, DataR2dbc, Redis (réactif + repos)
- `iwm.persistence.mode: memory`
- `iwm.outbox` : `delivery.type: recording`, `consumers.mode: inline`, `relay.enabled: false`,
  `replay-on-startup.enabled: false`
- `iwm.redis.permission-cache.enabled: false`, `iwm.search.elasticsearch.enabled: false`
- `iwm.security.jwt.auto-generate-key-pair: true` (paire RSA en mémoire → JWKS + RS256 OK)
- `iwm.security.client-applications.bootstrap` : `enabled: true`, `client-id: poc-client`,
  `secret: <secret POC>`, `allowed-services` couvrant le(s) service(s) des routes `/api/auth/**`
  (valeur exacte figée par le smoke-test au premier boot)
- `iwm.management.security.api-key` : valeur ≥ 16 caractères, hors liste interdite
- `server.port: ${PORT:8080}` (Render injecte `PORT`)
- CORS : `iwm.security.cors.allowed-origins` inclut l'URL Vercel du POC

> Note : la paire RSA étant régénérée à chaque démarrage, les tokens ne survivent pas à un
> cold start Render. Acceptable pour une démo live.

## 6. Déploiement Render

- **`Dockerfile`** (racine du nouveau repo) : multi-stage `maven:3.9-eclipse-temurin-21` →
  build `mvn -pl auth-poc-app -am -DskipTests package` → runtime `eclipse-temurin:21-jre`,
  copie du jar `auth-poc-app`, `EXPOSE 8080`, entrypoint `java -jar`.
- **`render.yaml`** (blueprint) : un `web service` type `docker`, plan `free`, health check
  sur `/actuator/health`, variables d'env (`PORT` auto, management api key, secret client POC,
  origines CORS).
- Build déclenché depuis le repo GitHub `yowauth-poc`. URL publique HTTPS auto.
- Cold start ~50 s après veille — acceptable pour la soutenance.

## 7. POC

### 7.1 Script smoke (`poc/smoke.ps1` + `poc/smoke.sh`)
Enchaîne, contre `$BASE_URL`, avec les en-têtes client bootstrap
(`X-Client-Id: poc-client`, `X-Api-Key: <secret>`) :

1. `POST /api/auth/register` (création compte sur le tenant seedé)
2. `POST /api/auth/login` (par username ou email) → SSO/session token + organisations
3. échange OIDC : `POST` token exchange → access token JWT (`tokenType=Bearer`)
4. `GET /.well-known/jwks.json` → clé publique RSA
5. `GET /api/users/me` (ou userinfo OIDC) → profil + organisations accessibles

Chaque étape imprime le code HTTP et le JSON. Les chemins/headers exacts sont confirmés par
le premier passage et figés dans le script.

### 7.2 Frontend Next.js (`poc/web/`)
- App Next.js (App Router), une page de démo.
- `NEXT_PUBLIC_API_URL` → URL Render. Appels `fetch` vers register/login + affichage du JWT
  décodé (header/payload) et du JWKS.
- Déployé sur **Vercel** (gratuit, import du sous-dossier `poc/web`).
- L'origine Vercel est ajoutée aux `allowed-origins` CORS de l'API.

## 8. README de démo (`yowauth-poc/README.md`)
Sections : lancer en local (`mvn -pl auth-poc-app -am spring-boot:run`), lancer le smoke,
lancer le frontend (`npm run dev`), déployer sur Render, déployer sur Vercel, scénario de
soutenance pas-à-pas.

## 9. Découpage en unités testables
- `auth-poc-app` : assemblage runtime, aucune logique métier ; testé par le boot du contexte.
- `InMemoryOrganizationAdaptersConfig` : adaptateurs de sortie purs ; comportement seedé
  déterministe, vérifiable unitairement.
- Cores copiés : inchangés ; leurs tests existants (104 tests auth + roles) restent verts.

## 10. Ordre d'implémentation prévu
1. Créer le repo/dossier `yowauth-poc` (fait), copier les 5 cores + pom parent trimé.
2. Créer `auth-poc-app` (pom, application, `application.yml`).
3. Boot local → résoudre itérativement les beans manquants (3 adaptateurs org connus + tout
   autre révélé par le boot) jusqu'au démarrage propre.
4. Smoke-test local → figer chemins/headers/services autorisés.
5. Dockerfile + build image + run local du conteneur.
6. Repo GitHub + déploiement Render via `render.yaml`.
7. Smoke-test contre l'URL Render.
8. Frontend Next.js → déploiement Vercel → CORS.
9. README de démo.
