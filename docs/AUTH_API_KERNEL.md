# YowAuth (`auth-core`) — Guide d'utilisation des endpoints sur le serveur réel

Documentation des endpoints d'**authentification** exposés par le kernel Yowyob en
production.

- **Base URL** : `https://kernel-core.yowyob.com`
- **Swagger UI** : https://kernel-core.yowyob.com/swagger-ui/index.html
- **OpenAPI JSON** : https://kernel-core.yowyob.com/v3/api-docs

> ⚠️ Ce module est le **même `auth-core`** que celui démontré dans ce POC. La différence :
> sur le serveur réel, **toutes les couches de sécurité sont actives** (vraies
> `ClientApplication`, vraies souscriptions d'organisation, vraies permissions) et les emails
> de vérification sont **réellement envoyés** (pas de mode `PREVIEW_ONLY`).

---

## 1. Modèle de sécurité (à comprendre avant tout appel)

Le kernel applique jusqu'à **5 couches** sur les routes `/api/**`. Selon l'endpoint, il faut
fournir tout ou partie de ces en-têtes :

| En-tête | Quand | Rôle |
|---|---|---|
| `X-Client-Id` + `X-Api-Key` | tous les appels backend | identifie le **backend appelant** (une `ClientApplication` enregistrée et autorisée sur le service) |
| `X-Tenant-Id` | requêtes multi-tenant | espace client cible (UUID) |
| `X-Organization-Id` | endpoints scopés organisation | organisation cible (abonnée au service requis) |
| `Authorization: Bearer <jwt>` | endpoints exigeant un utilisateur | jeton de session/access émis par YowAuth |

Conséquences pratiques :
- `POST /api/auth/sign-up` et `POST /api/auth/login` sont accessibles à un **client backend**
  (couple `X-Client-Id`/`X-Api-Key`) — pas besoin d'utilisateur connecté.
- `POST /api/auth/register` est **réservé à un administrateur** (`system:admin` / `iam:admin`) :
  il faut un `Authorization: Bearer` d'un user admin. Pour l'inscription publique, utiliser
  **`/api/auth/sign-up`**.
- `GET /api/users/me` et la plupart des `/api/...` métier exigent un **bearer** utilisateur.

### Authentification dans Swagger
Cliquer **Authorize** (en haut à droite de Swagger). Renseigner selon le schéma proposé :
le couple client (`X-Client-Id` / `X-Api-Key`) et/ou un `Bearer` utilisateur. Les endpoints
verrouillés (🔒) deviennent appelables.

---

## 2. Découverte & OIDC (public, sans authentification)

| Méthode | Chemin | Description |
|---|---|---|
| `GET` | `/.well-known/openid-configuration` | Document de découverte OIDC : `issuer`, `jwks_uri`, endpoints, grants supportés. |
| `GET` | `/.well-known/oauth-authorization-server` | Métadonnées OAuth2 (équivalent serveur d'autorisation). |
| `GET` | `/.well-known/jwks.json` | **Clé publique RSA** (JWKS) pour vérifier la signature RS256 des jetons. |

```bash
curl https://kernel-core.yowyob.com/.well-known/openid-configuration
curl https://kernel-core.yowyob.com/.well-known/jwks.json
```

---

## 3. Échange et inspection de jetons (OIDC)

| Méthode | Chemin | Description |
|---|---|---|
| `POST` | `/oauth2/token` | **Token-exchange** (RFC 8693) : échange un jeton de session SSO contre un **access token de service**. `application/x-www-form-urlencoded`. |
| `GET`/`POST` | `/oauth2/userinfo` | Renvoie les infos de l'utilisateur à partir d'un access token (bearer). |
| `POST` | `/oauth2/introspect` | Valide/inspecte un jeton (actif, claims). |

**Paramètres de `/oauth2/token`** (form-urlencoded) :
`grant_type=urn:ietf:params:oauth:grant-type:token-exchange`,
`subject_token=<jeton SSO>`,
`subject_token_type=urn:ietf:params:oauth:token-type:jwt`,
`context_id=<id du contexte/organisation choisi>`,
`service_code=<service visé, ex. SALES>`,
`client_id=<client>`, `client_secret=<secret>`.

```bash
curl -X POST https://kernel-core.yowyob.com/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \
  --data-urlencode "subject_token=$SSO_TOKEN" \
  --data-urlencode "subject_token_type=urn:ietf:params:oauth:token-type:jwt" \
  --data-urlencode "context_id=$CONTEXT_ID" \
  --data-urlencode "service_code=SALES" \
  --data-urlencode "client_id=$CLIENT_ID" \
  --data-urlencode "client_secret=$CLIENT_SECRET"
```

---

## 4. Authentification (`/api/auth/**`)

Toutes ces routes demandent au minimum `X-Client-Id` + `X-Api-Key` (+ `X-Tenant-Id` selon
le cas). Les réponses suivent l'enveloppe standard `{ success, data, message, errorCode }`.

### 4.1 Inscription et connexion

| Méthode | Chemin | Description | Corps (JSON) |
|---|---|---|---|
| `POST` | `/api/auth/sign-up` | **Inscription publique** (self-service). | `tenantId`, `firstName`, `lastName`, `username`, `email`, `password`, et options : `signUpSelectionToken`, `contextId`, `phoneNumber`, `socialProvider`, `externalSubject`, `accountType`, `businessType`, `onboardingData` |
| `POST` | `/api/auth/login` | **Connexion** par identifiant + mot de passe. Renvoie `accessToken`, `sessionToken`, `sharedSession` (SSO) et **organisations accessibles**. | `{ "principal": "...", "password": "..." }` |
| `POST` | `/api/auth/identify` | Détermine si un identifiant mène à une **inscription** ou une **connexion** (et compte les tenants). | `{ "principal": "..." }` |
| `POST` | `/api/auth/register` | Création de compte **par un admin** (protégé). | `actorId?`, `username`, `email`, `phoneNumber?`, `password?`, `authProvider`, `externalSubject?` |

```bash
# Inscription publique
curl -X POST https://kernel-core.yowyob.com/api/auth/sign-up \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $API_KEY" \
  -H "X-Tenant-Id: $TENANT" \
  -d '{"tenantId":"'$TENANT'","firstName":"Alice","lastName":"N","username":"alice","email":"alice@exemple.io","password":"P@ssw0rd!2024"}'

# Connexion
curl -X POST https://kernel-core.yowyob.com/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $API_KEY" \
  -H "X-Tenant-Id: $TENANT" \
  -d '{"principal":"alice","password":"P@ssw0rd!2024"}'
```

> **Mode strict :** un compte `LOCAL` dont l'email n'est pas vérifié **ne peut pas** se
> connecter tant que la vérification n'est pas faite (voir 4.3).

### 4.2 Connexion multi-organisation (contextes)

| Méthode | Chemin | Description | Corps (JSON) |
|---|---|---|---|
| `POST` | `/api/auth/discover-contexts` | Liste les **contextes/organisations** disponibles après authentification ; renvoie un `selectionToken`. | `{ "principal": "...", "password": "..." }` |
| `POST` | `/api/auth/select-context` | Choisit un contexte → connexion finalisée pour cette organisation. | `{ "selectionToken": "...", "contextId": "...", "organizationId": "..."? }` |
| `POST` | `/api/auth/discover-sign-up-contexts` | Liste les contextes d'inscription pour un **code organisation** (ex. employé rejoignant son entreprise). | `{ "organizationCode": "..." }` |
| `POST` | `/api/auth/refresh` | Renouvelle la session. | `{ "refreshToken": "..." }` |
| `POST` | `/api/auth/logout` | Invalide la session courante. | — |

### 4.3 Vérification d'email & de téléphone

| Méthode | Chemin | Description |
|---|---|---|
| `POST` | `/api/auth/email-verification/request` | Émet un jeton de vérification pour l'utilisateur courant (bearer). |
| `POST` | `/api/auth/email-verification/resend` | Renvoie un jeton de vérification pour un `principal` donné. Corps : `{ "principal": "..." }`. |
| `POST` | `/api/auth/email-verification/confirm` | Confirme l'email. Corps : `{ "verificationToken": "..." }`. |
| `POST` | `/api/auth/phone-verification/request` | Émet un code OTP de vérification du téléphone. |
| `POST` | `/api/auth/phone-verification/confirm` | Confirme le téléphone via le code. |

> Sur le serveur réel, le jeton/code est **envoyé par email/SMS** (le mode `PREVIEW_ONLY`
> qui renvoie le token en clair n'est actif qu'en dev/POC sans serveur mail).

### 4.4 Mot de passe oublié / réinitialisation

| Méthode | Chemin | Description | Corps (JSON) |
|---|---|---|---|
| `POST` | `/api/auth/forgot-password` | Démarre la procédure ; renvoie un `selectionToken` (multi-tenant possible). | `{ "principal": "..." }` |
| `POST` | `/api/auth/password-reset/issue` | Émet le jeton de réinitialisation pour le compte choisi. | `{ "selectionToken": "...", "contextId": "..." }` |
| `POST` | `/api/auth/reset-password` | Définit le nouveau mot de passe. | `{ "resetToken": "...", "newPassword": "..." }` |

### 4.5 MFA, OTP, Captcha

| Méthode | Chemin | Description |
|---|---|---|
| `POST` | `/api/auth/mfa/enable` | Active le MFA pour l'utilisateur courant. |
| `POST` | `/api/auth/mfa/confirm` | Confirme l'activation du MFA. |
| `POST` | `/api/auth/mfa/disable` | Désactive le MFA. |
| `POST` | `/api/auth/login/mfa/confirm` | Finalise une connexion exigeant le MFA. Corps : `{ "mfaToken": "...", "code": "..." }`. |
| `POST` | `/api/auth/otp` / `/api/auth/otp/verify` | Émet / vérifie un OTP. |
| `POST` | `/api/auth/captcha` / `/api/auth/captcha/verify` | Émet / vérifie un captcha. |

---

## 5. Self-service utilisateur (`/api/users/me`) — bearer requis

| Méthode | Chemin | Description |
|---|---|---|
| `GET` | `/api/users/me` | Profil de l'utilisateur connecté + organisations accessibles. |
| `PUT` | `/api/users/me/plan` | Met à jour le plan de l'utilisateur. |
| `PUT` | `/api/users/me/onboarding` | Met à jour l'état d'onboarding. |
| `PUT` | `/api/users/me/identity-onboarding` | Onboarding lié à l'identité. |

```bash
curl https://kernel-core.yowyob.com/api/users/me \
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $API_KEY" \
  -H "X-Tenant-Id: $TENANT" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## 6. Applications clientes (`/api/client-applications`) — admin

| Méthode | Chemin | Description |
|---|---|---|
| `GET` | `/api/client-applications` | Liste les `ClientApplication`. |
| `POST` | `/api/client-applications` | Crée un client backend (renvoie `clientId` + secret). |
| `PATCH` | `/api/client-applications/{id}` | Modifie un client. |
| `POST` | `/api/client-applications/{id}/rotate-secret` | Régénère le secret. |
| `POST` | `/api/client-applications/{id}/revoke` | Révoque un client. |
| `POST` | `/api/client-applications/me/authorize` | Autorise le client courant sur un service. |

> Un `clientId` + `apiKey` obtenu ici est ce qu'on place dans `X-Client-Id` / `X-Api-Key`
> pour appeler le reste de l'API.

---

## 7. Flux complet de bout en bout (serveur réel)

```
1. (admin) créer une ClientApplication            -> X-Client-Id / X-Api-Key
2. POST /api/auth/sign-up                          -> compte créé
3. (email réel) cliquer le lien / récupérer le token
   POST /api/auth/email-verification/confirm       -> email vérifié
4. POST /api/auth/login                            -> accessToken + sharedSession + organisations
5. POST /oauth2/token  (context_id + service_code) -> access token de service
6. GET  /oauth2/userinfo  | GET /api/users/me      -> identité / profil
7. (vérif) GET /.well-known/jwks.json              -> valider la signature RS256
```

---

## 8. Codes d'erreur fréquents

| Statut | Signification probable |
|---|---|
| `401 UNAUTHORIZED` | `X-Client-Id`/`X-Api-Key` absents ou invalides, ou bearer manquant/expiré. |
| `403 FORBIDDEN` | Client non autorisé sur le service, organisation non abonnée, ou permission utilisateur insuffisante. |
| `400 AUTH_INVALID_REQUEST` | Corps invalide (champ requis manquant, ex. `principal`, `verificationToken`). |
| `invalid_grant` / `invalid_client` (OIDC) | `subject_token`, `context_id`, `service_code` ou identifiants client invalides à `/oauth2/token`. |

---

## 9. Pour aller plus loin
La référence vivante reste le **Swagger** du serveur :
https://kernel-core.yowyob.com/swagger-ui/index.html — les schémas exacts des corps de
requête/réponse y sont générés depuis le code. Ce guide en donne la lecture côté
authentification (`auth-core`) avec le contexte sécurité du kernel.
