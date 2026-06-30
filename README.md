# YowAuth — POC de déploiement (slice `auth-core` isolé)

POC du module d'authentification **YowAuth** (`auth-core`) de la plateforme Yowyob,
extrait du monolithe modulaire `KSM_Kernel_Layer` dans un dépôt **autonome**, déployé
**gratuitement**, et démontré de bout en bout.

- **API live (Render)** : https://yowauth-poc.onrender.com
- **Démo web (Vercel)** : https://yowauth-poc.vercel.app
- **Swagger UI** : https://yowauth-poc.onrender.com/swagger-ui.html
- **OIDC discovery** : https://yowauth-poc.onrender.com/.well-known/openid-configuration

> ⏱️ Le service tourne sur le plan **gratuit** de Render : il se met en veille après
> ~15 min d'inactivité. Le **premier** appel après veille prend ~30-50 s (cold start).
> Avant une démo, ouvrir l'URL une fois pour « réveiller » le service.

---

## Ce que le POC démontre

YowAuth est le fournisseur d'identité (IdP) de l'écosystème Yowyob : comptes utilisateurs,
multi-tenant, multi-organisation, JWT **RS256**, endpoints **OIDC** standard. Le flux complet
prouvé en ligne :

1. `GET /.well-known/openid-configuration` — découverte OIDC
2. `GET /.well-known/jwks.json` — clé publique RSA (vérification RS256)
3. `POST /api/auth/sign-up` — inscription
4. `POST /api/auth/email-verification/resend` — challenge (mode `PREVIEW_ONLY` : token renvoyé en clair, pas de serveur mail)
5. `POST /api/auth/email-verification/confirm` — vérification de l'email
6. `POST /api/auth/login` — connexion → `accessToken` + session SSO + **organisations accessibles**
7. `GET /api/users/me` — profil (bearer)
8. `POST /oauth2/token` — token-exchange (`context_id` + `service_code`) → access token de service
9. `GET /oauth2/userinfo` — infos utilisateur (bearer)

---

## Architecture du slice

Monolithe modulaire hexagonal d'origine : seul `bootstrap` est exécutable et tire ~20 cores.
Ce POC n'embarque que la **chaîne d'identité** :

| Module | Rôle |
|---|---|
| `RT-comops-common-core` | socle (ApiResponse, adresses/contacts) |
| `RT-comops-kernel-core` | sécurité, JWT RS256, JWKS, ClientApplication |
| `RT-comops-file-core` | dépendance transitive d'actor-core |
| `RT-comops-actor-core` | BusinessActor (dépendance d'auth-core) |
| `RT-comops-roles-core` | `ReactivePermissionResolver` (RBAC) |
| `RT-comops-auth-core` | **module cible** : register/login/OIDC/token/contexte |
| `auth-poc-app` | runner Spring Boot + adaptateurs in-memory |

**Organisation simulée** : les 3 ports normalement servis par `organization-core`
(`UserOrganizationAccessDirectory`, `SignUpContextDirectory`,
`OrganizationServiceRuntimeEntitlementDirectory`) sont remplacés par des **adaptateurs de
sortie in-memory seedés** dans `auth-poc-app` (extensibilité prévue par l'hexagonal). La
logique d'auth exercée reste 100 % réelle ; seules les *données* d'organisation sont simulées
(organisation `YOWAUTH-DEMO`, services `SALES`/`IAM`).

**Persistance** : profil `test-memory` (tout en mémoire), JWT RSA auto-généré. **Aucune base
de données externe.**

---

## Lancer en local

Prérequis : **JDK 21**. Maven via le wrapper IntelliJ ou une install Maven 3.9+.

```bash
# build du jar exécutable
mvn -pl auth-poc-app -am -DskipTests package

# lancement (profil in-memory par défaut)
IWM_MANAGEMENT_API_KEY=local-management-key-123456 \
IWM_POC_CLIENT_SECRET=poc-secret-key \
java -jar auth-poc-app/target/auth-poc-app-0.1.0-SNAPSHOT.jar
```

L'API écoute sur http://localhost:8080.

### Smoke-test (prouve le flux complet)

```bash
# en local
BASE_URL=http://localhost:8080 bash poc/smoke.sh
# ou contre la prod
BASE_URL=https://yowauth-poc.onrender.com API_KEY=poc-secret-key bash poc/smoke.sh
```

PowerShell : `./poc/smoke.ps1 -BaseUrl "https://yowauth-poc.onrender.com"`

### Front Next.js en local

```bash
cd poc/web
cp .env.example .env.local   # NEXT_PUBLIC_API_URL pointe sur Render par défaut
npm install
npm run dev                  # http://localhost:3000
```

---

## Déploiement

### API → Render (Docker, plan free)
Blueprint `render.yaml` (web service Docker). Variables à fournir :
`IWM_POC_CLIENT_SECRET` (= `poc-secret-key`), `IWM_CORS_ALLOWED_ORIGINS`
(inclure l'URL Vercel). `IWM_MANAGEMENT_API_KEY` est auto-généré.

### Front → Vercel
Importer le repo, **Root Directory = `poc/web`**, variables `NEXT_PUBLIC_*`
(voir `poc/web/.env.example`). Puis ajouter l'URL Vercel à
`IWM_CORS_ALLOWED_ORIGINS` côté Render.

---

## Conteneur Docker

```bash
docker build -t yowauth-poc:local .
docker run --rm -p 8080:8080 \
  -e IWM_MANAGEMENT_API_KEY=local-management-key-123456 \
  -e IWM_POC_CLIENT_SECRET=poc-secret-key \
  yowauth-poc:local
```

---

## Scénario de soutenance

1. Réveiller l'API (ouvrir l'URL Render, attendre le 200).
2. Ouvrir la démo Vercel, cliquer **« Lancer le flux »**.
3. Commenter au fil des étapes :
   - openid-configuration / JWKS → conformité OIDC + clé publique RS256
   - sign-up + vérification email (mode PREVIEW, sans serveur mail)
   - login → **JWT RS256** + **organisations accessibles** (multi-org)
   - token-exchange OIDC → access token de service (`svc: SALES`)
   - userinfo → identité résolue depuis le token
4. Montrer le **JWT décodé** affiché par la page (claims `sub`, `tid`, `svc`, `iss`).
5. (option) Rejouer `poc/smoke.sh` pour la preuve reproductible en ligne de commande.

---

## Limites connues (assumées pour le POC)

- Données **en mémoire** : tout est réinitialisé à chaque redémarrage / cold start.
- Paire **RSA régénérée** au démarrage : les tokens ne survivent pas à un redémarrage.
- Données d'organisation **simulées** (pas de `organization-core` réel).
- Flux secondaires (MFA, OTP, mot de passe oublié) présents dans le code mais hors périmètre de la démo.
- Plan Render gratuit : cold start + 512 Mo RAM.

---

## Isolation vis-à-vis du projet de base

Ce dépôt est **distinct** du fork `KSM_Kernel_Layer`. Les 6 cores y sont **copiés** (snapshot
figé) ; rien n'est écrit dans le fork. Conception détaillée :
[`docs/2026-06-27-yowauth-poc-deployment-design.md`](docs/2026-06-27-yowauth-poc-deployment-design.md).
