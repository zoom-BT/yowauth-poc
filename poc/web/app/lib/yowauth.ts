// Client API YowAuth — encapsule les appels à l'IdP (sign-up, vérif, login, profil).

export const config = {
  // Le navigateur ne parle qu'au BFF (même origine) : /api/proxy. Aucun secret côté client.
  PROXY_BASE: process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/proxy",
  // Non secrets — conservés uniquement pour affichage (le proxy réinjecte les vraies valeurs).
  CLIENT_ID: process.env.NEXT_PUBLIC_CLIENT_ID ?? "",
  TENANT: process.env.NEXT_PUBLIC_TENANT_ID ?? "",
  SERVICE_CODE: process.env.NEXT_PUBLIC_SERVICE_CODE ?? "SALES",
};

// Le proxy BFF injecte X-Client-Id / X-Api-Key / X-Tenant-Id côté serveur.
const baseHeaders: Record<string, string> = {
  "Content-Type": "application/json",
};

export type Organization = {
  organizationId: string;
  organizationCode: string;
  shortName: string;
  longName: string;
  services: string[];
};

export type Profile = {
  id: string;
  username: string;
  email: string;
  status: string;
  plan: string;
  accountType: string;
  emailVerified: boolean;
  actorId: string;
  tenantId: string;
};

export type Session = {
  accessToken: string;
  ssoToken: string;
  profile: Profile;
  organizations: Organization[];
};

export type PasswordResetContextResponse = {
  contextId: string;
  tenantId: string;
  userId: string;
  actorId: string;
  username: string;
  email: string;
};

export type ForgotPasswordResponse = {
  principal: string;
  matchingAccountCount: number;
  selectionToken: string;
  selectionTokenExpiresInSeconds: number;
  contexts: PasswordResetContextResponse[];
};

export type IssuedAuthChallengeResponse = {
  deliveryMode: string;
  challengeTokenPreview: string;
  expiresInSeconds: number;
};

export type CaptchaChallengeResponse = {
  captchaToken: string;
  prompt: string;
  answerPreview: string;
  expiresInSeconds: number;
};

export type CaptchaVerificationResponse = {
  captchaVerificationToken: string;
  expiresInSeconds: number;
};

export type UserAccountResponse = {
  id: string;
  tenantId: string;
  username: string;
  email: string;
  status: string;
  accountType: string;
};

type ApiResponse<T> = { success: boolean; data: T; message?: string; errorCode?: string };

/**
 * Erreur applicative porteuse d'un message DÉJÀ prêt à afficher à l'utilisateur.
 * On ne laisse jamais fuiter de dump technique (HTML, stacktrace, log brut) vers l'UI.
 */
export class ApiError extends Error {
  status: number;
  errorCode?: string;
  constructor(status: number, message: string, errorCode?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
  }
}

// Messages FR par code d'erreur métier du kernel (les plus fréquents).
const ERROR_MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: "Identifiant ou mot de passe incorrect.",
  AUTH_ACCOUNT_LOCKED: "Votre compte est temporairement bloqué. Réessayez plus tard.",
  AUTH_MFA_INVALID_CODE: "Code de vérification incorrect. Veuillez réessayer.",
  AUTH_MFA_EXPIRED: "Le code de vérification a expiré. Demandez-en un nouveau.",
  AUTH_EMAIL_ALREADY_EXISTS: "Un compte existe déjà avec cet email.",
  AUTH_USERNAME_ALREADY_EXISTS: "Ce nom d'utilisateur est déjà pris.",
  USERNAME_DUPLICATE: "Ce nom d'utilisateur est déjà pris. Choisissez-en un autre.",
  EMAIL_DUPLICATE: "Un compte existe déjà avec cet email.",
};

/** Repli poli et neutre selon le code HTTP, quand aucun message propre n'est disponible. */
function messageForStatus(status: number): string {
  if (status === 0) return "Connexion au service impossible. Vérifiez votre connexion Internet et réessayez.";
  if (status === 400) return "Les informations saisies sont invalides. Veuillez vérifier et réessayer.";
  if (status === 401) return "Identifiant ou mot de passe incorrect.";
  if (status === 403) return "Vous n'êtes pas autorisé à effectuer cette action.";
  if (status === 404) return "Service momentanément indisponible. Veuillez réessayer dans quelques instants.";
  if (status === 409) return "Ce compte existe déjà.";
  if (status === 429) return "Trop de tentatives. Veuillez patienter un instant avant de réessayer.";
  if (status >= 500) return "Une erreur technique est survenue de notre côté. Veuillez réessayer plus tard.";
  return "Une erreur est survenue. Veuillez réessayer.";
}

/** Un message renvoyé par le kernel est-il présentable tel quel ? (phrase courte, pas de HTML) */
function isCleanMessage(m?: string): m is string {
  return !!m && m.length <= 160 && !m.includes("<") && !/doctype|<html/i.test(m);
}

/** Choisit le meilleur message affichable : code métier FR > signature connue > message kernel propre > repli HTTP. */
function friendlyMessage(status: number, body?: { message?: string; errorCode?: string }): string {
  const code = body?.errorCode;
  if (code && ERROR_MESSAGES[code]) return ERROR_MESSAGES[code];

  // Le kernel renvoie parfois une 500 avec un dump SQL brut sur violation de contrainte
  // d'unicité (au lieu d'une 409 propre). On rattrape les cas connus pour rester actionnable.
  const raw = body?.message ?? "";
  if (/recovery_email|recovery email/i.test(raw))
    return "Un compte utilise déjà cet email de contact. Utilisez-en un autre.";
  if (/duplicate|already exists|unique constraint/i.test(raw) && /email/i.test(raw))
    return "Un compte existe déjà avec cet email.";
  if (/duplicate|already exists|unique constraint/i.test(raw) && /user_?name/i.test(raw))
    return "Ce nom d'utilisateur est déjà pris. Choisissez-en un autre.";

  if (isCleanMessage(body?.message)) return body!.message!;
  return messageForStatus(status);
}

/** Convertit n'importe quelle exception en message utilisateur propre (jamais de technique brute). */
export function toUserMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error && isCleanMessage(err.message)) return err.message;
  return "Une erreur inattendue est survenue. Veuillez réessayer.";
}

/**
 * Appel HTTP vers le kernel, via le BFF proxy (même origine → aucun CORS).
 * `path` est le chemin kernel (ex. "/api/auth/login") ; il est préfixé par /api/proxy.
 * Les headers d'identité applicative sont injectés côté serveur par le proxy.
 */
export async function resilientFetch(path: string, init?: RequestInit): Promise<Response> {
  return fetch(`${config.PROXY_BASE}${path}`, init);
}

async function api<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  let res: Response;
  try {
    res = await resilientFetch(path, init);
  } catch {
    // Échec réseau (service injoignable, hors-ligne, DNS…) : message neutre, jamais l'erreur brute.
    throw new ApiError(0, messageForStatus(0));
  }
  const text = await res.text();
  let body: ApiResponse<T>;
  try {
    body = JSON.parse(text) as ApiResponse<T>;
  } catch {
    // Réponse non-JSON (page d'erreur HTML, proxy momentanément indisponible…) :
    // on ne montre JAMAIS le dump — juste un message propre selon le code HTTP.
    throw new ApiError(res.status, messageForStatus(res.status));
  }
  if (!res.ok || body.success === false) {
    throw new ApiError(res.status, friendlyMessage(res.status, body), body?.errorCode);
  }
  return body;
}

export type SignUpResult = {
  username: string;
  email: string;
  /** Identité Yowyob générée par le kernel à partir du username (ex. alice@yowyob.com). */
  yowyobEmail?: string;
  status: string;
  emailVerified: boolean;
};

/**
 * Inscription. Le kernel crée le compte, génère l'adresse `@yowyob.com` et,
 * en production (SMTP), envoie un email de vérification. On NE tente PAS de
 * confirmer automatiquement : la vérif se fait via le lien reçu par email
 * (page /auth/verify-email). Le résultat expose `yowyobEmail` pour l'afficher.
 */
export async function signUp(
  username: string,
  email: string,
  password: string,
  captchaVerificationToken?: string
): Promise<SignUpResult> {
  const res = await api<SignUpResult>(`/api/auth/sign-up`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({
      tenantId: config.TENANT,
      username,
      email,
      password,
      firstName: username,
      lastName: "User",
      captchaVerificationToken,
    }),
  });
  return res.data;
}

/** Confirme la vérification d'email à partir du token reçu par lien email. */
export async function confirmEmailVerification(verificationToken: string): Promise<void> {
  await api(`/api/auth/email-verification/confirm`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ verificationToken }),
  });
}

/** Connexion par identifiant + mot de passe → session (token + profil + organisations) ou défi MFA. */
export async function login(
  principal: string,
  password: string
): Promise<Session | { mfaRequired: true; mfaToken: string; channel: string; codePreview: string | null }> {
  const res = await api<{
    nextStep?: string;
    mfaToken?: string;
    channel?: string;
    codePreview?: string | null;
    accessToken?: string;
    sharedSession?: { token: string };
    organizations?: Organization[];
  } & Profile>(`/api/auth/login`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ principal, password }),
  });
  const d = res.data;
  if (d.nextStep === "CONFIRM_MFA") {
    return {
      mfaRequired: true,
      mfaToken: d.mfaToken!,
      channel: d.channel!,
      codePreview: d.codePreview ?? null,
    };
  }
  return {
    accessToken: d.accessToken!,
    ssoToken: d.sharedSession?.token ?? "",
    organizations: d.organizations ?? [],
    profile: {
      id: d.id,
      username: d.username,
      email: d.email,
      status: d.status,
      plan: d.plan,
      accountType: d.accountType,
      emailVerified: d.emailVerified,
      actorId: d.actorId,
      tenantId: d.tenantId,
    },
  };
}

/** Finalise la connexion par MFA. */
export async function confirmLoginMfa(mfaToken: string, code: string): Promise<Session> {
  const res = await api<{
    accessToken: string;
    sharedSession?: { token: string };
    organizations?: Organization[];
  } & Profile>(`/api/auth/login/mfa/confirm`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ mfaToken, code }),
  });
  const d = res.data;
  return {
    accessToken: d.accessToken,
    ssoToken: d.sharedSession?.token ?? "",
    organizations: d.organizations ?? [],
    profile: {
      id: d.id,
      username: d.username,
      email: d.email,
      status: d.status,
      plan: d.plan,
      accountType: d.accountType,
      emailVerified: d.emailVerified,
      actorId: d.actorId,
      tenantId: d.tenantId,
    },
  };
}

/** Identifie l'utilisateur avant connexion (2-step). */
export async function identify(principal: string): Promise<{ accountExists: boolean; nextStep: string; matchingAccountCount: number }> {
  const res = await api<{ principal: string; accountExists: boolean; nextStep: string; matchingAccountCount: number }>(
    `/api/auth/identify`,
    {
      method: "POST",
      headers: baseHeaders,
      body: JSON.stringify({ principal }),
    }
  );
  return res.data;
}

/** Sélectionne le contexte (organisation) et récupère un token contextualisé. */
export async function selectContext(sToken: string, contextId: string, organizationId: string): Promise<{ tenantId: string; organizationId: string; loginResponse: { accessToken: string; sharedSession?: { token: string }; organizations?: Organization[] } & Profile }> {
  const res = await api<{
    tenantId: string;
    organizationId: string;
    loginResponse: {
      accessToken: string;
      sharedSession?: { token: string };
      organizations?: Organization[];
    } & Profile;
  }>(`/api/auth/select-context`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ selectionToken: sToken, contextId, organizationId }),
  });
  return res.data;
}

/** Met à jour le plan d'abonnement (comptes connectés). */
export async function updatePlan(accessToken: string, plan: string): Promise<Profile> {
  const res = await api<Profile>(`/api/users/me/plan`, {
    method: "PUT",
    headers: { ...baseHeaders, Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ plan }),
  });
  return res.data;
}

/** Met à jour l'étape d'onboarding (comptes connectés). */
export async function updateOnboarding(accessToken: string, step: number, status: string): Promise<Profile> {
  const res = await api<Profile>(`/api/users/me/onboarding`, {
    method: "PUT",
    headers: { ...baseHeaders, Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ step, status }),
  });
  return res.data;
}

/** Initie l'activation du MFA (comptes connectés). */
export async function enableMfa(accessToken: string, channel: string): Promise<{ token: string; codePreview: string | null }> {
  const res = await api<{ deliveryMode: string; token: string; codePreview: string | null; expiresInSeconds: number }>(
    `/api/auth/mfa/enable`,
    {
      method: "POST",
      headers: { ...baseHeaders, Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify({ channel }),
    }
  );
  return res.data;
}

/** Valide et active le MFA. */
export async function confirmMfa(accessToken: string, challengeToken: string, code: string): Promise<Profile> {
  const res = await api<Profile>(`/api/auth/mfa/confirm`, {
    method: "POST",
    headers: { ...baseHeaders, Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ challengeToken, code }),
  });
  return res.data;
}

/** Désactive le MFA. */
export async function disableMfa(accessToken: string): Promise<Profile> {
  const res = await api<Profile>(`/api/auth/mfa/disable`, {
    method: "POST",
    headers: { ...baseHeaders, Authorization: `Bearer ${accessToken}` },
  });
  return res.data;
}

/** OIDC Token Exchange (RFC 8693) — Échange le SSO Token contre un jeton d'accès de service. */
export async function exchangeToken(ssoToken: string, contextId: string, serviceCode: string): Promise<{ access_token: string; token_type: string; expires_in: number; scope: string }> {
  // client_id / client_secret ne sont plus envoyés depuis le navigateur :
  // le BFF proxy injecte l'authentification client (Basic) pour /oauth2/token.
  const form = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:token-exchange",
    subject_token: ssoToken,
    subject_token_type: "urn:ietf:params:oauth:token-type:jwt",
    context_id: contextId,
    service_code: serviceCode,
  });

  let res: Response;
  try {
    res = await resilientFetch(`/oauth2/token`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded"
      },
      body: form.toString(),
    });
  } catch {
    throw new ApiError(0, messageForStatus(0));
  }
  const text = await res.text();
  if (!res.ok) {
    throw new ApiError(res.status, messageForStatus(res.status));
  }
  return JSON.parse(text);
}

/** Récupère les infos OIDC userinfo via le jeton de service. */
export async function fetchUserInfo(serviceToken: string): Promise<Record<string, unknown>> {
  let res: Response;
  try {
    res = await resilientFetch(`/oauth2/userinfo`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${serviceToken}`
      }
    });
  } catch {
    throw new ApiError(0, messageForStatus(0));
  }
  const text = await res.text();
  if (!res.ok) {
    throw new ApiError(res.status, messageForStatus(res.status));
  }
  return JSON.parse(text);
}

/** Récupère le profil courant à partir d'un access token (bearer). */
export async function fetchMe(accessToken: string): Promise<Profile> {
  const res = await api<Profile>(`/api/users/me`, {
    headers: { ...baseHeaders, Authorization: `Bearer ${accessToken}` },
  });
  return res.data;
}

/** Décode (sans vérifier) le payload d'un JWT pour affichage. */
export function decodeJwt(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(decodeURIComponent(escape(atob(payload))));
  } catch {
    return null;
  }
}

/** Initie la récupération du mot de passe en identifiant l'utilisateur */
export async function forgotPassword(principal: string): Promise<ForgotPasswordResponse> {
  const res = await api<ForgotPasswordResponse>(`/api/auth/forgot-password`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ principal }),
  });
  return res.data;
}

/** Demande l'émission du défi de réinitialisation de mot de passe (token de reset) pour un contexte donné */
export async function issuePasswordReset(selectionToken: string, contextId: string): Promise<IssuedAuthChallengeResponse> {
  const res = await api<IssuedAuthChallengeResponse>(`/api/auth/password-reset/issue`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ selectionToken, contextId }),
  });
  return res.data;
}

/** Finalise le changement de mot de passe */
export async function resetPassword(resetToken: string, newPassword: string): Promise<UserAccountResponse> {
  const res = await api<UserAccountResponse>(`/api/auth/reset-password`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ resetToken, newPassword }),
  });
  return res.data;
}

/** Récupère un défi de captcha */
export async function getCaptchaChallenge(): Promise<CaptchaChallengeResponse> {
  const res = await api<CaptchaChallengeResponse>(`/api/auth/captcha`, {
    method: "POST",
    headers: baseHeaders,
  });
  return res.data;
}

/** Valide la réponse au captcha et renvoie un token de vérification */
export async function verifyCaptchaChallenge(captchaToken: string, answer: string): Promise<CaptchaVerificationResponse> {
  const res = await api<CaptchaVerificationResponse>(`/api/auth/captcha/verify`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ captchaToken, answer }),
  });
  return res.data;
}
