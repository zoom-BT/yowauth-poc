# YowAuth — Script de soutenance

> Déploiement isolé et démonstration du module d'authentification (`auth-core`)
> de l'écosystème Yowyob.
> **Auteur :** Tchoutzine · **Cours :** Réseaux Intelligents (Master)
> **Liens :** API → https://yowauth-poc.onrender.com · Démo → https://yowauth-poc.vercel.app · Code → https://github.com/zoom-BT/yowauth-poc

Ce document est un **script** : il se lit comme un fil de présentation. Chaque section
contient ce que je dis (le fond) et, en encadré, ce que je montre à l'écran.

---

## 0. En une phrase

> « J'ai pris **YowAuth**, le fournisseur d'identité de la plateforme Yowyob, je l'ai
> **extrait** du gros monolithe dans lequel il vit, je l'ai rendu **exécutable seul**, je l'ai
> **déployé gratuitement**, et je prouve que **tous ses endpoints d'authentification
> fonctionnent en ligne** — du compte créé jusqu'au jeton JWT vérifiable. »

---

## 1. De quoi on parle : les concepts (pour poser le vocabulaire)

### 1.1 Qu'est-ce que YowAuth ?
YowAuth est un **fournisseur d'identité** (IdP, *Identity Provider*). C'est l'équivalent
du « **Se connecter avec Google** », mais pour les applications de Yowyob
(BusinessBook, Rental, Fleetman…). Son rôle : **gérer les comptes**, **authentifier**
les utilisateurs, et **émettre des jetons** que les autres services pourront faire confiance.

### 1.2 Multi-tenant et multi-organisation
Deux niveaux d'isolation, souvent confondus :
- **Tenant** = un espace client complètement étanche. Fleetman et BusStation peuvent vivre
  sur le même serveur sans jamais voir les données l'un de l'autre. Chaque ligne en base
  porte un `tenantId`.
- **Organisation** = à l'intérieur d'un tenant, une entreprise/structure à laquelle
  l'utilisateur a accès, avec un sous-ensemble de **services** (ex. `SALES`, `IAM`).

> Un même email peut donc exister dans plusieurs organisations : à la connexion, YowAuth
> renvoie **la liste des organisations accessibles** — c'est le cœur « multi-org ».

### 1.3 JWT signé en RS256
Un **JWT** (*JSON Web Token*) est un jeton porteur de 3 parties : `header.payload.signature`.
Le payload contient des *claims* (ex. `sub` = identifiant utilisateur, `tid` = tenant,
`svc` = service, `iss` = émetteur).

**RS256** = signature **asymétrique** (RSA) :
- l'IdP signe avec sa **clé privée** (qu'il garde) ;
- n'importe quel service vérifie avec la **clé publique** (qu'on peut distribuer largement).

> Pourquoi c'est malin : on n'a **aucun secret partagé** à distribuer. Chaque microservice
> peut vérifier un jeton tout seul, hors-ligne, sans rappeler l'IdP.

### 1.4 JWKS
La clé publique est exposée au format standard **JWKS** (*JSON Web Key Set*) sur
`/.well-known/jwks.json`. Les services la récupèrent une fois et vérifient les signatures.

### 1.5 OIDC et token-exchange
**OIDC** (*OpenID Connect*) standardise la découverte (`/.well-known/openid-configuration`)
et les endpoints (`/oauth2/token`, `/oauth2/userinfo`, `/oauth2/introspect`).

⚠️ Point important que j'assume : YowAuth **n'implémente pas** le flux « écran de
consentement à la Google » (`authorization_code` + page « cette app veut accéder à votre
compte »). Le seul *grant* supporté est le **token-exchange** (RFC 8693) :
> on présente un jeton de session SSO + le **contexte** choisi (organisation) + le **service**
> visé, et on reçoit en échange un **access token dédié à ce service**.
C'est cohérent : YowAuth est l'IdP des apps **first-party** de Yowyob, pas un OAuth public
pour applications tierces.

### 1.6 Architecture hexagonale (ports & adapters)
Chaque module est découpé en :
- **domaine** (les règles métier pures, ex. `UserAccount`) ;
- **ports** = des **interfaces** (entrées : cas d'usage ; sorties : besoins externes comme
  « lire un compte », « lister les organisations ») ;
- **adapters** = les **implémentations** concrètes des ports (REST, base de données…).

> Le domaine ne connaît jamais la base ni le web : il ne parle qu'à des **interfaces**.
> **C'est cette propriété qui a rendu mon extraction possible** (j'y reviens en §4).

### 1.7 Monolithe modulaire
Le projet d'origine est un **monolithe modulaire** : ~20 modules (`*-core`) dans un seul
dépôt, **un seul exécutable** (`bootstrap`) qui les assemble tous. Avantage : code organisé
par domaine, mais déploiement unique.

---

## 2. Le point de départ et l'objectif

**Mon rôle dans le projet :** j'étais en charge de `auth-core` (j'ai notamment écrit
**104 tests unitaires** couvrant tout le flux d'authentification).

**Le problème ce soir :** le vrai serveur (avec tous les cores) est déployé ailleurs, mais
**instable** et **hors de mon contrôle**. Je veux pouvoir **démontrer MON module seul**,
de façon fiable et gratuite.

**L'objectif :**
1. extraire tout ce qui concerne l'authentification dans un **dépôt séparé** ;
2. le **déployer gratuitement** sur une autre plateforme ;
3. **prouver** que les endpoints passent, via un **POC**.

---

## 3. La méthode (comment j'ai travaillé)

J'ai suivi une démarche d'ingénieur, pas du « code au hasard » :
1. **Cadrage (design)** : j'ai écrit une **spec** qui fige le périmètre, les risques et les
   décisions → `docs/2026-06-27-yowauth-poc-deployment-design.md`.
2. **Plan d'implémentation** : 9 tâches découpées, testables une par une →
   `docs/superpowers/plans/2026-06-27-yowauth-poc-deployment.md`.
3. **Exécution** avec **commits fréquents** et **vérification à chaque étape** (compilation,
   tests, smoke, conteneur, prod).
4. **Isolation stricte** : tout vit dans un dépôt **distinct** du fork de base ; le fork
   n'a **rien** reçu de mon POC (il sert seulement de source de copie).

> Message clé pour le jury : chaque décision est **documentée et justifiée**, et chaque
> affirmation (« ça marche ») est **prouvée** par une commande dont je montre la sortie.

---

## 4. Le parcours technique — les chemins traversés

C'est le cœur de la présentation : *comment* on passe d'un module non-exécutable à un
service en ligne. Chaque obstacle a une explication.

### 4.1 Décision : extraire une « tranche » plutôt que tout déployer
`auth-core` n'est pas exécutable seul (seul `bootstrap` l'est). Deux options :
- déployer **tout** le monolithe (lourd, ~20 cores, et ce n'est pas « mon » module isolé) ;
- extraire une **tranche** (*slice*) = `auth-core` + le strict nécessaire.

→ J'ai choisi la **tranche**, car elle correspond à mon rôle et reste légère.

### 4.2 Quels modules embarquer ? (résolution des dépendances)
En partant des dépendances Maven d'`auth-core`, puis en compilant, j'ai découvert la
chaîne réelle :
```
common ← kernel ← file ← actor ← auth
                          roles ─┘
```
- `auth` dépend de `actor`, `kernel`, `common` ;
- `actor` dépend de `file` (surprise révélée par la compilation) ;
- `roles` fournit le calcul des permissions (RBAC) attendu par la sécurité du `kernel`.

→ **6 cores** dans la tranche + un module **runner** que j'ai créé : `auth-poc-app`.

### 4.3 Le vrai défi : faire démarrer le contexte Spring
Quand on retire des modules, certains **beans** (objets gérés par Spring) ne sont plus là.
J'ai fait une **boucle de résolution** : démarrer → lire l'erreur « no qualifying bean » →
fournir le bean → recommencer. Trois cas, trois explications :

1. **`AddressRepository` introuvable.**
   *Cause :* les modules choisissent leur implémentation de persistance par **profil Spring**
   (`@Profile("test-memory")` pour la version en mémoire, `@Profile("r2dbc")` pour PostgreSQL).
   *Solution :* activer le profil **`test-memory`** → tout passe en mémoire (zéro base de
   données externe).

2. **`ObjectMapper` (Jackson) introuvable.**
   *Cause :* le `bootstrap` réel définit sa propre config Jackson ; en l'enlevant, plus
   d'`ObjectMapper`. *Solution :* j'ai repris cette petite config dans `auth-poc-app`.

3. **`UserAccountDirectory` introuvable.**
   *Cause :* c'est un **port** d'`actor-core` dont l'adaptateur vit dans `bootstrap` et
   pointe vers `auth-core`. *Solution :* j'ai repris cet adaptateur **tel quel** (il ne
   dépend que de modules présents).

### 4.4 L'organisation : « on n'a pas organization-core, et alors ? »
`auth-core` a besoin de 3 informations d'organisation, via **3 ports de sortie**
(lister les organisations d'un user, contextes d'inscription, droits de service). Leurs
adaptateurs réels tapent `organization-core`, **que je n'ai pas embarqué**.

→ Grâce à l'**hexagonal**, j'ai branché **3 adaptateurs in-memory seedés** dans
`auth-poc-app` (une organisation d'exemple `YOWAUTH-DEMO`, services `SALES`/`IAM`).

> Point conceptuel à défendre : **la logique d'authentification exercée reste 100 % réelle**
> (signature RS256, sélection de contexte, multi-tenant). Seules les **données**
> d'organisation sont simulées. C'est exactement l'intérêt des ports & adapters :
> remplacer une source de données sans toucher au métier.

### 4.5 Le smoke-test : découvrir le vrai contrat des endpoints
J'ai sondé l'API en marche pour **figer les chemins exacts**. Découvertes :
- `/api/auth/register` est **protégé** (réservé admin) → l'inscription publique se fait via
  `/api/auth/sign-up`.
- Mode **strict** : un compte local non vérifié **ne peut pas se connecter**.
- Sans serveur mail, les endpoints de vérification renvoient le token en clair
  (mode **`PREVIEW_ONLY`**) → je peux donc vérifier l'email automatiquement pour la démo.
- `/oauth2/token` exige **`context_id`** (l'organisation) **+ `service_code`** (le service).

→ Flux complet validé : `sign-up → vérif email (PREVIEW) → login → users/me →
token-exchange → userinfo`.

### 4.6 Mise en conteneur (Docker)
`Dockerfile` multi-étapes : une étape **build** (Maven + JDK 21 → le jar), une étape
**runtime** (JRE seul, plus léger). J'ai **validé l'image localement** : le conteneur passe
le smoke-test complet.

### 4.7 Déploiement de l'API (Render, gratuit)
Blueprint `render.yaml` : Render construit l'image Docker côté serveur et expose une URL
HTTPS. J'ai relancé le smoke-test **contre la prod** → tout vert.
> *Caveat assumé :* plan gratuit = **mise en veille** après ~15 min (un 503 transitoire au
> réveil, puis cold start ~30-50 s). Avant la démo, je « réveille » l'API.

### 4.8 Le front (Next.js → Vercel)
J'ai d'abord eu un **404** : Vercel buildait depuis la racine (un projet Maven, pas Next.js).
*Solution :* régler le **Root Directory = `poc/web`** puis redéployer. Ensuite, réglage
**CORS** côté Render pour autoriser l'origine Vercel (sinon le navigateur bloque les appels
cross-origine).

---

## 5. Ce que j'ai implémenté (le livrable)

| Élément | Rôle |
|---|---|
| `auth-poc-app` | runner Spring Boot exécutable (la tranche assemblée) |
| 3 adaptateurs in-memory | simulent l'organisation sans `organization-core` |
| `application.yml` | profil in-memory, JWT auto-généré, client bootstrap |
| `poc/smoke.sh` / `.ps1` | preuve reproductible du flux complet (9 étapes) |
| `Dockerfile` + `render.yaml` | conteneurisation + déploiement gratuit |
| `poc/web` (Next.js) | **vraie page de connexion** (login/inscription) + **dashboard** + **démo technique** (`/demo`) |

**La page web** consomme réellement l'IdP : on crée un compte, on se connecte, et on voit
son **profil**, ses **organisations** et son **JWT décodé**. Une page `/demo` déroule en plus
les 9 étapes techniques une à une (utile pour montrer le détail OIDC).

---

## 6. Démonstration live (déroulé)

> **Avant :** ouvrir l'URL Render une fois pour réveiller le service (attendre le chargement).

1. **Conformité OIDC** — ouvrir `/.well-known/openid-configuration` puis `/.well-known/jwks.json`.
   *Je dis :* « voici le contrat OIDC et la clé publique RSA qui sert à vérifier les jetons. »
2. **Inscription** — sur la démo Vercel, onglet **Inscription** → créer un compte.
   *Je dis :* « le compte est créé, l'email est vérifié automatiquement (mode preview, sans
   serveur mail), et je suis connecté. »
3. **Dashboard** — montrer profil + **organisation `YOWAUTH-DEMO`** + **JWT décodé**.
   *Je dis :* « le multi-organisation est là, et voici les claims signés : `sub`, `tid`, `svc`. »
4. **Login classique** — se déconnecter, se reconnecter via **Connexion**.
5. **Détail technique** — page `/demo` : dérouler les 9 étapes, insister sur le
   **token-exchange** (`context_id` + `service_code`) → access token de service.
6. **Preuve en ligne de commande** *(optionnel)* :
   ```bash
   BASE_URL=https://yowauth-poc.onrender.com API_KEY=poc-secret-key bash poc/smoke.sh
   ```

---

## 7. Questions probables du jury (et mes réponses)

**« Pourquoi RS256 et pas HS256 ? »**
HS256 est symétrique : il faudrait partager le même secret avec chaque service qui vérifie.
RS256 distribue seulement la **clé publique** → vérification décentralisée, pas de secret
qui fuit.

**« Vos données d'organisation sont fausses, donc ce n'est pas représentatif ? »**
La **logique** d'auth est réelle (signature, contextes, multi-tenant). J'ai seulement
remplacé la **source de données** d'organisation par un adaptateur in-memory — ce que
l'architecture hexagonale autorise par conception. En prod, on rebranche `organization-core`
sans toucher au métier.

**« Pourquoi pas un écran de consentement comme Google ? »**
Parce que YowAuth est l'IdP des apps **first-party** de Yowyob. Il implémente le
**token-exchange** (RFC 8693), pas le flux `authorization_code` avec consentement tiers.
Ajouter un `authorization_endpoint` + une page de consentement serait une évolution possible.

**« Les jetons survivent-ils à un redémarrage ? »**
Non : tout est en mémoire et la **paire RSA est régénérée** au démarrage. C'est volontaire
pour un POC gratuit. En prod, la clé serait persistée/rotée (le projet prévoit la rotation
JWKS).

**« Comment garantissez-vous que ça marche vraiment ? »**
Par des **preuves** : tests unitaires, test de chargement du contexte Spring, smoke-test
exécuté en local **et** contre la prod, validation de l'image Docker. Je montre les sorties.

**« Pourquoi un dépôt séparé ? »**
Pour **isoler** mon travail du fork de base (pas de pollution, pas de conflit de merge avec
l'amont) et pour avoir un artefact **déployable seul**.

---

## 8. Limites assumées
- Données **en mémoire** (réinitialisées à chaque redémarrage / cold start).
- **Clé RSA régénérée** au démarrage (jetons non persistants).
- Organisation **simulée** (pas de `organization-core`).
- Flux secondaires (MFA, OTP, mot de passe oublié) présents dans le code mais hors démo.
- Plan gratuit Render : cold start + 512 Mo RAM.

## 9. Conclusion
> « J'ai démontré que **mon module d'authentification est autonome, déployable et conforme** :
> un utilisateur s'inscrit, se connecte, obtient un **JWT RS256 vérifiable**, voit ses
> **organisations**, et échange son jeton via **OIDC**. Le tout extrait proprement d'un
> monolithe grâce à l'**architecture hexagonale**, déployé **gratuitement**, et **prouvé**
> de bout en bout — en local, en conteneur, et en production. »
