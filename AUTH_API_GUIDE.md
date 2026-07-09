# Guide d'Intégration API YowAuth (`auth-core`)

Ce guide détaille les endpoints d'authentification exposés par le Kernel Yowyob et explique comment les consommer pour intégrer YowAuth dans une application cliente.

- **Base URL** : `https://kernel-core.yowyob.com`
- **Swagger UI** : [https://kernel-core.yowyob.com/swagger-ui/index.html](https://kernel-core.yowyob.com/swagger-ui/index.html)
- **OpenAPI Spec (JSON)** : [https://kernel-core.yowyob.com/v3/api-docs](https://kernel-core.yowyob.com/v3/api-docs)

---

## 1. Modèle de Sécurité (Headers requis)

Le Kernel apporte des couches de sécurité sur les routes `/api/**`. Selon l'endpoint, il faut fournir tout ou partie de ces en-têtes :

| En-tête | Type | Rôle |
|---|---|---|
| `X-Client-Id` | Chaîne | Identifiant unique de l'application cliente backend (ex: `prod-platform-backend`). |
| `X-Api-Key` | Chaîne | Clé secrète associée au client backend. |
| `X-Tenant-Id` | UUID | Identifiant unique du Tenant cible (ex: `11111111-1111-1111-1111-111111111111`). |
| `Authorization` | Bearer JWT | `Bearer <access_token>` émis par YowAuth après connexion. |

---

## 2. Découverte OIDC (Public)

Ces routes permettent d'obtenir les configurations et clés publiques nécessaires à la validation locale des jetons.

| Méthode | Chemin | Description |
|---|---|---|
| `GET` | `/.well-known/openid-configuration` | Document de découverte OIDC : `issuer`, `jwks_uri`, etc. |
| `GET` | `/.well-known/jwks.json` | Clé publique RSA pour vérifier la signature RS256 des tokens JWT. |

---

## 3. Authentification & Connexion (`/api/auth/**`)

### 3.1 Détection du compte & Connexion en 2 étapes
Pour une expérience utilisateur fluide (IHM), il est recommandé de séparer l'identification de la saisie des credentials.

1. **Vérifier l'identifiant :**
   * **Endpoint :** `POST /api/auth/identify`
   * **Corps :** `{"principal": "alice"}`
   * **Utilité :** Renvoie `accountExists: true` et le `nextStep` (ex: `SIGN_IN_PASSWORD`).

2. **Connexion par mot de passe :**
   * **Endpoint :** `POST /api/auth/login`
   * **Corps :** `{"principal": "alice", "password": "..."}`
   * **Réponse Standard (200 OK) :** Retourne les jetons (`accessToken`, `ssoToken`) et le profil.
   * **Réponse Défi MFA (202 Accepted) :** Retourne `nextStep: "CONFIRM_MFA"`, `mfaToken` et le canal (ex: `EMAIL`).

3. **Validation MFA (si exigée) :**
   * **Endpoint :** `POST /api/auth/login/mfa/confirm`
   * **Corps :** `{"mfaToken": "...", "code": "..."}` (code reçu par e-mail).

---

### 3.2 Inscription publique
* **Endpoint :** `POST /api/auth/sign-up`
* **Corps :**
  ```json
  {
    "tenantId": "11111111-1111-1111-1111-111111111111",
    "username": "alice",
    "email": "alice@exemple.io",
    "password": "...",
    "firstName": "Alice",
    "lastName": "N"
  }
  ```

---

### 3.3 Sélection du contexte d'organisation
Si l'utilisateur est rattaché à plusieurs organisations, il doit choisir son espace actif.

1. **Lister les contextes :**
   * **Endpoint :** `POST /api/auth/discover-contexts`
   * **Corps :** `{"principal": "...", "password": "..."}` (renvoie un `selectionToken`).

2. **Sélectionner l'organisation active :**
   * **Endpoint :** `POST /api/auth/select-context`
   * **Corps :** `{"selectionToken": "...", "contextId": "...", "organizationId": "..."}` (renvoie le jeton d'accès mis à jour).

---

## 4. Single Sign-On (SSO) & Token Exchange (OIDC RFC 8693)

Le flux de SSO permet à un utilisateur connecté sur le portail d'identité YowAuth d'accéder directement à un autre service (ex: `SALES`, `ACCOUNTING`) sans avoir à ressaisir son mot de passe.

1. **Échanger le jeton SSO (Token Exchange) :**
   * **Endpoint :** `POST /oauth2/token`
   * **Format :** `application/x-www-form-urlencoded`
   * **Paramètres :**
     * `grant_type=urn:ietf:params:oauth:grant-type:token-exchange`
     * `subject_token=<ssoToken>`
     * `subject_token_type=urn:ietf:params:oauth:token-type:jwt`
     * `context_id=<contextId>`
     * `service_code=<SALES|ACCOUNTING|...>`
     * `client_id=<clientId>`
     * `client_secret=<clientSecret>`
   * **Réponse :** Retourne un `access_token` spécifique pour la plateforme demandée.

2. **Valider l'identité (UserInfo) :**
   * **Endpoint :** `GET /oauth2/userinfo`
   * **Header :** `Authorization: Bearer <access_token_de_service>`
   * **Réponse :** Informations sur l'utilisateur connecté (id, username, sub).

---

## 5. Gestion du Profil & MFA (Self-Service)

Ces actions nécessitent d'être authentifié (présence du header `Authorization: Bearer <accessToken>`).

### 5.1 Gestion de l'abonnement
* **Modifier le plan :** `PUT /api/users/me/plan`
* **Corps :** `"PREMIUM"` ou `"ENTERPRISE"` (chaîne brute ou JSON selon configuration).

### 5.2 Enrôlement / Désactivation du MFA
1. **Initier l'activation :**
   * **Endpoint :** `POST /api/auth/mfa/enable` (renvoie un `challengeToken`).
2. **Confirmer l'activation :**
   * **Endpoint :** `POST /api/auth/mfa/confirm`
   * **Corps :** `{"challengeToken": "...", "code": "..."}` (le code de vérification reçu).
3. **Désactiver le MFA :**
   * **Endpoint :** `POST /api/auth/mfa/disable`
