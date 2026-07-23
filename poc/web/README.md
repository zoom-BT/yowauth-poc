# Front YowAuth — portail « Se connecter avec Yowyob »

Front-end Next.js 16 (App Router, React 19) du portail d'identité **One ID** de
l'écosystème Yowyob. Il s'adosse au kernel KSM via un **BFF proxy** same-origin :
le navigateur n'appelle que `/api/proxy`, le serveur injecte l'identité applicative
(clé API) et relaie vers le kernel. **Aucun secret n'est exposé au navigateur.**

---

## ⚠️ À lire avant de déployer

Cette application **n'est pas un site statique**. Elle contient :

- un **BFF proxy** côté serveur (`app/api/proxy/[...path]/route.ts`, runtime Node) ;
- des routes rendues côté serveur.

**Elle DOIT donc tourner comme un serveur Node** (`node server.js` via la sortie
`standalone`, ou l'image Docker fournie). Si vous la servez en **statique**
(nginx sur un dossier `out/`, `next export`…), **le proxy disparaît → le login et
tous les appels API cassent.** C'est la seule contrainte pour obtenir en prod un
rendu **strictement identique** à l'aperçu Vercel.

---

## Variables d'environnement

Deux familles, à ne pas confondre :

### 1. `KSM_*` — secrets SERVEUR, injectés au **RUNTIME**

Lues par le BFF proxy à chaque requête (`process.env`). **Ne jamais** les mettre
dans l'image ni les préfixer `NEXT_PUBLIC_`. Injectées au `docker run`.

| Variable         | Obligatoire | Exemple / défaut                              | Rôle                                                        |
|------------------|:-----------:|-----------------------------------------------|-------------------------------------------------------------|
| `KSM_BACKEND_URL`| oui         | `https://kernel-core.yowyob.com`              | Base URL du kernel. Défaut interne si absent.               |
| `KSM_CLIENT_ID`  | oui         | `prod-platform-backend`                       | Identifiant de l'application cliente (header `X-Client-Id`).|
| `KSM_CLIENT_KEY` | **oui**     | `••••••••` (**SECRET**)                        | Clé API de l'application (header `X-Api-Key`).              |
| `KSM_TENANT_ID`  | oui         | `11111111-1111-1111-1111-111111111111`        | Tenant par défaut (header `X-Tenant-Id`).                   |

### 2. `NEXT_PUBLIC_*` — publiques, figées au **BUILD**

Non secrètes. **Inlinées dans le bundle au moment du `npm run build`** : elles
doivent donc être présentes *pendant le build* (Docker : `--build-arg`).
Servent à l'affichage et au corps de la requête `sign-up`.

| Variable                  | Obligatoire | Exemple / défaut                        | Rôle                                                        |
|---------------------------|:-----------:|-----------------------------------------|-------------------------------------------------------------|
| `NEXT_PUBLIC_CLIENT_ID`   | oui         | `prod-platform-backend`                 | Client ID affiché (le proxy réinjecte la vraie identité).   |
| `NEXT_PUBLIC_TENANT_ID`   | oui         | `11111111-1111-1111-1111-111111111111`  | Tenant utilisé dans le corps du sign-up.                    |
| `NEXT_PUBLIC_SERVICE_CODE`| non         | `SALES`                                 | Code de service pour le token-exchange OIDC.                |
| `NEXT_PUBLIC_API_BASE_URL`| non         | *(vide)*                                | Laisser **vide** : le front tape `/api/proxy` sur sa propre origine. Ne définir que si le BFF est sur un autre hôte. |

Un gabarit est fourni : [`.env.example`](.env.example).

---

## Déploiement Docker (recommandé — rendu = Vercel)

### Build

```bash
docker build \
  --build-arg NEXT_PUBLIC_CLIENT_ID=prod-platform-backend \
  --build-arg NEXT_PUBLIC_TENANT_ID=11111111-1111-1111-1111-111111111111 \
  --build-arg NEXT_PUBLIC_SERVICE_CODE=SALES \
  -t yowauth-front .
```

### Fichier d'environnement runtime

Créer un `.env.runtime` (hors dépôt — contient le secret) :

```env
KSM_BACKEND_URL=https://kernel-core.yowyob.com
KSM_CLIENT_ID=prod-platform-backend
KSM_CLIENT_KEY=la-vraie-cle-secrete
KSM_TENANT_ID=11111111-1111-1111-1111-111111111111
```

### Run

```bash
docker run -d -p 3000:3000 --env-file .env.runtime --name yowauth-front yowauth-front
```

L'app écoute sur `http://<hôte>:3000`. Mettre un reverse-proxy (nginx/Traefik)
devant pour le TLS.

### Intégration docker-compose (écosystème Yowyob)

```yaml
services:
  yowauth-front:
    build:
      context: .
      args:
        NEXT_PUBLIC_CLIENT_ID: prod-platform-backend
        NEXT_PUBLIC_TENANT_ID: 11111111-1111-1111-1111-111111111111
        NEXT_PUBLIC_SERVICE_CODE: SALES
    environment:
      KSM_BACKEND_URL: https://kernel-core.yowyob.com
      KSM_CLIENT_ID: prod-platform-backend
      KSM_CLIENT_KEY: ${KSM_CLIENT_KEY}   # depuis le .env serveur, jamais commité
      KSM_TENANT_ID: 11111111-1111-1111-1111-111111111111
    ports:
      - "3000:3000"
    restart: unless-stopped
```

---

## Déploiement Node sans Docker

```bash
npm ci
# Les NEXT_PUBLIC_* doivent être dans l'environnement AVANT le build :
NEXT_PUBLIC_CLIENT_ID=prod-platform-backend \
NEXT_PUBLIC_TENANT_ID=11111111-1111-1111-1111-111111111111 \
NEXT_PUBLIC_SERVICE_CODE=SALES \
npm run build

# Puis lancer le serveur standalone (les KSM_* sont lus au runtime) :
KSM_BACKEND_URL=https://kernel-core.yowyob.com \
KSM_CLIENT_ID=prod-platform-backend \
KSM_CLIENT_KEY=la-vraie-cle-secrete \
KSM_TENANT_ID=11111111-1111-1111-1111-111111111111 \
node .next/standalone/server.js
```

> La sortie `standalone` copie automatiquement le strict nécessaire dans
> `.next/standalone`. Penser à recopier `public/` et `.next/static` à côté de
> `server.js` si vous déplacez le dossier (le Dockerfile le fait déjà).

---

## Développement local

```bash
cp .env.example .env.local   # puis renseigner les vraies valeurs KSM_*
npm install
npm run dev                  # http://localhost:3000
```

`.env.local` est **gitignoré** : les secrets n'y quittent jamais votre machine.

---

## Vérifier que « prod = Vercel »

Après déploiement, contrôler que ces flux répondent comme sur l'aperçu Vercel :

1. Page d'accueil et pages `docs` / `demo` s'affichent (rendu, images, launcher).
2. `POST /api/proxy/api/auth/identify` renvoie du JSON (le proxy tourne).
3. Login d'un compte valide → session (ou défi MFA) — **preuve que le serveur
   Node et les secrets `KSM_*` sont bien en place**.

Si (2) ou (3) échouent en 404/HTML, l'app est probablement servie en statique :
revoir la contrainte « serveur Node » ci-dessus.

---

## Stack

Next.js 16 · React 19 · Node 20 · CSS maison (pas de Tailwind) · BFF proxy same-origin.
