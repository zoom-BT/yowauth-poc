// Client API YowAuth — encapsule les appels à l'IdP (sign-up, vérif, login, profil).

export const config = {
  PRIMARY_API: process.env.NEXT_PUBLIC_PRIMARY_API_URL ?? "https://kernel-core.yowyob.com",
  SECONDARY_API: process.env.NEXT_PUBLIC_SECONDARY_API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  CLIENT_ID: process.env.NEXT_PUBLIC_CLIENT_ID ?? "poc-client",
  API_KEY: process.env.NEXT_PUBLIC_API_KEY ?? "poc-secret-key",
  TENANT: process.env.NEXT_PUBLIC_TENANT_ID ?? "00000000-0000-0000-0000-000000000001",
  SERVICE_CODE: process.env.NEXT_PUBLIC_SERVICE_CODE ?? "SALES",
  get API() { return this.PRIMARY_API; }
};

const baseHeaders: Record<string, string> = {
  "Content-Type": "application/json",
  "X-Client-Id": config.CLIENT_ID,
  "X-Api-Key": config.API_KEY,
  "X-Tenant-Id": config.TENANT,
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
 * Effectue un appel HTTP fetch résilient. Tente d'abord de contacter l'API principale.
 * En cas d'erreur réseau (ex: serveur indisponible), d'erreur de proxy (502, 503, 504), ou
 * si l'endpoint n'est pas trouvé/implémenté sur l'API principale (404, 501),
 * la requête est automatiquement re-soumise sur l'API de repli (POC).
 */
export async function resilientFetch(path: string, init?: RequestInit): Promise<Response> {
  const primaryUrl = `${config.PRIMARY_API}${path}`;
  const secondaryUrl = `${config.SECONDARY_API}${path}`;

  try {
    const res = await fetch(primaryUrl, init);
    // Bascule sur le serveur secondaire si le service est indisponible ou si l'endpoint n'est pas implémenté
    if (
      res.status === 404 ||
      res.status === 501 ||
      res.status === 502 ||
      res.status === 503 ||
      res.status === 504
    ) {
      console.warn(`[Failover YowAuth] L'API principale (${config.PRIMARY_API}) a retourné le statut ${res.status} pour ${path}. Bascule sur l'API de repli (${config.SECONDARY_API}).`);
      return await fetch(secondaryUrl, init);
    }
    return res;
  } catch (error) {
    console.warn(`[Failover YowAuth] L'API principale (${config.PRIMARY_API}) est injoignable (${error}). Bascule sur l'API de repli (${config.SECONDARY_API}).`);
    return await fetch(secondaryUrl, init);
  }
}

async function api<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const res = await resilientFetch(path, init);
  const text = await res.text();
  let body: ApiResponse<T>;
  try {
    body = JSON.parse(text) as ApiResponse<T>;
  } catch {
    throw new Error(`Réponse non-JSON (HTTP ${res.status}) : ${text.slice(0, 200)}`);
  }
  if (!res.ok || body.success === false) {
    throw new Error(body?.message || `Échec (HTTP ${res.status})`);
  }
  return body;
}

/** Inscription + vérification email auto (mode PREVIEW du backend) → compte prêt à se connecter. */
export async function signUpAndVerify(
  username: string,
  email: string,
  password: string,
  captchaVerificationToken?: string
): Promise<void> {
  await api(`/api/auth/sign-up`, {
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
  const resend = await api<{ challengeTokenPreview: string }>(`/api/auth/email-verification/resend`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ principal: username }),
  });
  await api(`/api/auth/email-verification/confirm`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ verificationToken: resend.data.challengeTokenPreview }),
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
  const form = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:token-exchange",
    subject_token: ssoToken,
    subject_token_type: "urn:ietf:params:oauth:token-type:jwt",
    context_id: contextId,
    service_code: serviceCode,
    client_id: config.CLIENT_ID,
    client_secret: config.API_KEY,
  });

  const res = await resilientFetch(`/oauth2/token`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: form.toString(),
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`Token Exchange failed (HTTP ${res.status}): ${text}`);
  }
  return JSON.parse(text);
}

/** Récupère les infos OIDC userinfo via le jeton de service. */
export async function fetchUserInfo(serviceToken: string): Promise<Record<string, unknown>> {
  const res = await resilientFetch(`/oauth2/userinfo`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${serviceToken}`
    }
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`UserInfo failed (HTTP ${res.status}): ${text}`);
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
