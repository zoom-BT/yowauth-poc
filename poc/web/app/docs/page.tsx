import Link from "next/link";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "YowAuth — Guide API (serveur réel)",
  description: "Comment utiliser les endpoints auth-core sur https://kernel-core.yowyob.com",
};

const KERNEL = "https://kernel-core.yowyob.com";

type Row = { m: string; path: string; desc: string; body?: string };

function Method({ m }: { m: string }) {
  return <span className={`m m-${m.toLowerCase()}`}>{m}</span>;
}

function Table({ rows, withBody }: { rows: Row[]; withBody?: boolean }) {
  return (
    <table className="tbl">
      <thead>
        <tr>
          <th style={{ width: 70 }}>Méthode</th>
          <th>Chemin</th>
          <th>Description</th>
          {withBody && <th>Corps (JSON)</th>}
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.m + r.path + r.desc}>
            <td><Method m={r.m} /></td>
            <td><code>{r.path}</code></td>
            <td>{r.desc}</td>
            {withBody && <td>{r.body ? <code>{r.body}</code> : "—"}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

const discovery: Row[] = [
  { m: "GET", path: "/.well-known/openid-configuration", desc: "Document de découverte OIDC (issuer, jwks_uri, endpoints, grants)." },
  { m: "GET", path: "/.well-known/oauth-authorization-server", desc: "Métadonnées du serveur d'autorisation OAuth2." },
  { m: "GET", path: "/.well-known/jwks.json", desc: "Clé publique RSA (JWKS) pour vérifier la signature RS256." },
];

const oidc: Row[] = [
  { m: "POST", path: "/oauth2/token", desc: "Token-exchange (RFC 8693) : jeton SSO → access token de service. Form-urlencoded." },
  { m: "GET", path: "/oauth2/userinfo", desc: "Infos utilisateur à partir d'un access token (bearer)." },
  { m: "POST", path: "/oauth2/introspect", desc: "Valide/inspecte un jeton (actif, claims)." },
];

const auth: Row[] = [
  { m: "POST", path: "/api/auth/sign-up", desc: "Inscription publique (self-service).", body: "tenantId, firstName, lastName, username, email, password, …" },
  { m: "POST", path: "/api/auth/login", desc: "Connexion → accessToken, sessionToken, sharedSession (SSO) + organisations.", body: '{ "principal", "password" }' },
  { m: "POST", path: "/api/auth/identify", desc: "Détermine inscription vs connexion pour un identifiant.", body: '{ "principal" }' },
  { m: "POST", path: "/api/auth/register", desc: "Création de compte par un admin (protégé).", body: "username, email, authProvider, …" },
];

const contexts: Row[] = [
  { m: "POST", path: "/api/auth/discover-contexts", desc: "Liste les organisations disponibles ; renvoie un selectionToken.", body: '{ "principal", "password" }' },
  { m: "POST", path: "/api/auth/select-context", desc: "Choisit un contexte → connexion finalisée.", body: '{ "selectionToken", "contextId", "organizationId"? }' },
  { m: "POST", path: "/api/auth/discover-sign-up-contexts", desc: "Contextes d'inscription pour un code organisation.", body: '{ "organizationCode" }' },
  { m: "POST", path: "/api/auth/refresh", desc: "Renouvelle la session.", body: '{ "refreshToken" }' },
  { m: "POST", path: "/api/auth/logout", desc: "Invalide la session courante." },
];

const verif: Row[] = [
  { m: "POST", path: "/api/auth/email-verification/request", desc: "Émet un jeton de vérification (utilisateur courant)." },
  { m: "POST", path: "/api/auth/email-verification/resend", desc: "Renvoie un jeton pour un principal.", body: '{ "principal" }' },
  { m: "POST", path: "/api/auth/email-verification/confirm", desc: "Confirme l'email.", body: '{ "verificationToken" }' },
  { m: "POST", path: "/api/auth/phone-verification/request", desc: "Émet un code OTP de vérification du téléphone." },
  { m: "POST", path: "/api/auth/phone-verification/confirm", desc: "Confirme le téléphone via le code." },
];

const password: Row[] = [
  { m: "POST", path: "/api/auth/forgot-password", desc: "Démarre la récupération ; renvoie un selectionToken.", body: '{ "principal" }' },
  { m: "POST", path: "/api/auth/password-reset/issue", desc: "Émet le jeton de réinitialisation.", body: '{ "selectionToken", "contextId" }' },
  { m: "POST", path: "/api/auth/reset-password", desc: "Définit le nouveau mot de passe.", body: '{ "resetToken", "newPassword" }' },
];

const mfa: Row[] = [
  { m: "POST", path: "/api/auth/mfa/enable", desc: "Active le MFA (utilisateur courant)." },
  { m: "POST", path: "/api/auth/mfa/confirm", desc: "Confirme l'activation du MFA." },
  { m: "POST", path: "/api/auth/mfa/disable", desc: "Désactive le MFA." },
  { m: "POST", path: "/api/auth/login/mfa/confirm", desc: "Finalise une connexion avec MFA.", body: '{ "mfaToken", "code" }' },
  { m: "POST", path: "/api/auth/otp", desc: "Émet un OTP." },
  { m: "POST", path: "/api/auth/otp/verify", desc: "Vérifie un OTP." },
];

const me: Row[] = [
  { m: "GET", path: "/api/users/me", desc: "Profil de l'utilisateur connecté + organisations." },
  { m: "PUT", path: "/api/users/me/plan", desc: "Met à jour le plan." },
  { m: "PUT", path: "/api/users/me/onboarding", desc: "Met à jour l'onboarding." },
  { m: "PUT", path: "/api/users/me/identity-onboarding", desc: "Onboarding lié à l'identité." },
];

const clients: Row[] = [
  { m: "GET", path: "/api/client-applications", desc: "Liste les ClientApplication." },
  { m: "POST", path: "/api/client-applications", desc: "Crée un client backend (renvoie clientId + secret)." },
  { m: "PATCH", path: "/api/client-applications/{id}", desc: "Modifie un client." },
  { m: "POST", path: "/api/client-applications/{id}/rotate-secret", desc: "Régénère le secret." },
  { m: "POST", path: "/api/client-applications/{id}/revoke", desc: "Révoque un client." },
];

const headers: { h: string; when: string; role: string }[] = [
  { h: "X-Client-Id + X-Api-Key", when: "tous les appels backend", role: "identifie le backend appelant (ClientApplication autorisée sur le service)" },
  { h: "X-Tenant-Id", when: "requêtes multi-tenant", role: "espace client cible (UUID)" },
  { h: "X-Organization-Id", when: "endpoints scopés organisation", role: "organisation cible (abonnée au service)" },
  { h: "Authorization: Bearer <jwt>", when: "endpoints exigeant un utilisateur", role: "jeton de session/access émis par YowAuth" },
];

const errors: { code: string; meaning: string }[] = [
  { code: "401 UNAUTHORIZED", meaning: "X-Client-Id/X-Api-Key absents/invalides, ou bearer manquant/expiré." },
  { code: "403 FORBIDDEN", meaning: "Client non autorisé sur le service, organisation non abonnée, ou permission insuffisante." },
  { code: "400 AUTH_INVALID_REQUEST", meaning: "Corps invalide (champ requis manquant)." },
  { code: "invalid_grant / invalid_client", meaning: "Paramètres OIDC invalides à /oauth2/token." },
];

export default function DocsPage() {
  return (
    <main className="doc">
      <header className="doc__top" style={{ "--brand-img": "url(/img2.png)" } as React.CSSProperties}>
        <div className="doc__top-inner">
          <nav className="doc__nav">
            <Link href="/">← Connexion</Link>
            <Link href="/demo">Démo technique</Link>
            <a href={`${KERNEL}/swagger-ui/index.html`} target="_blank" rel="noreferrer">Swagger serveur réel ↗</a>
          </nav>
          <h1 className="doc__title">YowAuth — Guide API (serveur réel)</h1>
          <p className="doc__lead">
            Comment consommer les endpoints d&apos;<code>auth-core</code> sur <code>{KERNEL}</code>.
            C&apos;est le même module que ce POC, mais avec toutes les couches de sécurité du kernel actives.
          </p>
        </div>
      </header>

      <div className="doc__wrap">
        <h2>1. Modèle de sécurité (en-têtes)</h2>
        <p>Les routes <code>/api/**</code> appliquent jusqu&apos;à 5 couches. Selon l&apos;endpoint, fournir :</p>
        <table className="tbl">
          <thead><tr><th>En-tête</th><th>Quand</th><th>Rôle</th></tr></thead>
          <tbody>
            {headers.map((h) => (
              <tr key={h.h}><td><code>{h.h}</code></td><td>{h.when}</td><td>{h.role}</td></tr>
            ))}
          </tbody>
        </table>
        <div className="callout">
          <strong>register vs sign-up :</strong> <code>/api/auth/register</code> est réservé aux admins.
          Pour l&apos;inscription publique, utiliser <code>/api/auth/sign-up</code>. Dans Swagger,
          cliquer <strong>Authorize</strong> pour renseigner le client et/ou un bearer.
        </div>

        <h2>2. Découverte &amp; OIDC (public)</h2>
        <Table rows={discovery} />
        <div className="codeblock">{`curl ${KERNEL}/.well-known/openid-configuration
curl ${KERNEL}/.well-known/jwks.json`}</div>

        <h2>3. Jetons OIDC</h2>
        <Table rows={oidc} />
        <p>Paramètres de <code>/oauth2/token</code> (form-urlencoded) :</p>
        <div className="codeblock">{`curl -X POST ${KERNEL}/oauth2/token \\
  -H "Content-Type: application/x-www-form-urlencoded" \\
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \\
  --data-urlencode "subject_token=$SSO_TOKEN" \\
  --data-urlencode "subject_token_type=urn:ietf:params:oauth:token-type:jwt" \\
  --data-urlencode "context_id=$CONTEXT_ID" \\
  --data-urlencode "service_code=SALES" \\
  --data-urlencode "client_id=$CLIENT_ID" \\
  --data-urlencode "client_secret=$CLIENT_SECRET"`}</div>

        <h2>4. Authentification — inscription &amp; connexion</h2>
        <Table rows={auth} withBody />
        <div className="codeblock">{`# Connexion
curl -X POST ${KERNEL}/api/auth/login \\
  -H "Content-Type: application/json" \\
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $API_KEY" \\
  -H "X-Tenant-Id: $TENANT" \\
  -d '{"principal":"alice","password":"P@ssw0rd!2024"}'`}</div>
        <div className="callout">
          <strong>Mode strict :</strong> un compte <code>LOCAL</code> non vérifié ne peut pas se connecter
          tant que l&apos;email n&apos;est pas confirmé (section 5).
        </div>

        <h2>5. Connexion multi-organisation (contextes)</h2>
        <Table rows={contexts} withBody />

        <h2>6. Vérification email &amp; téléphone</h2>
        <Table rows={verif} withBody />
        <div className="callout">
          Sur le serveur réel, le jeton/code est <strong>envoyé par email/SMS</strong>. Le mode
          <code>PREVIEW_ONLY</code> (token renvoyé en clair) n&apos;est actif qu&apos;en dev/POC sans serveur mail.
        </div>

        <h2>7. Mot de passe oublié / réinitialisation</h2>
        <Table rows={password} withBody />

        <h2>8. MFA, OTP, Captcha</h2>
        <Table rows={mfa} withBody />

        <h2>9. Self-service utilisateur (bearer requis)</h2>
        <Table rows={me} />
        <div className="codeblock">{`curl ${KERNEL}/api/users/me \\
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $API_KEY" \\
  -H "X-Tenant-Id: $TENANT" \\
  -H "Authorization: Bearer $ACCESS_TOKEN"`}</div>

        <h2>10. Applications clientes (admin)</h2>
        <Table rows={clients} />
        <p>Le <code>clientId</code> + secret obtenus ici alimentent <code>X-Client-Id</code> / <code>X-Api-Key</code>.</p>

        <h2>11. Flux complet de bout en bout</h2>
        <div className="codeblock">{`1. (admin) créer une ClientApplication            -> X-Client-Id / X-Api-Key
2. POST /api/auth/sign-up                          -> compte créé
3. (email réel) POST /api/auth/email-verification/confirm  -> email vérifié
4. POST /api/auth/login                            -> accessToken + sharedSession + organisations
5. POST /oauth2/token  (context_id + service_code) -> access token de service
6. GET  /oauth2/userinfo  |  GET /api/users/me     -> identité / profil
7. GET  /.well-known/jwks.json                     -> vérifier la signature RS256`}</div>

        <h2>12. Codes d&apos;erreur fréquents</h2>
        <table className="tbl">
          <thead><tr><th>Statut</th><th>Signification probable</th></tr></thead>
          <tbody>
            {errors.map((e) => (
              <tr key={e.code}><td><code>{e.code}</code></td><td>{e.meaning}</td></tr>
            ))}
          </tbody>
        </table>

        <p style={{ marginTop: 28 }}>
          Référence vivante : <a href={`${KERNEL}/swagger-ui/index.html`} target="_blank" rel="noreferrer">Swagger du serveur réel ↗</a>
        </p>
      </div>
    </main>
  );
}
