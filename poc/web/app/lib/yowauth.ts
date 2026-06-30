// Client API YowAuth — encapsule les appels à l'IdP (sign-up, vérif, login, profil).

export const config = {
  API: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  CLIENT_ID: process.env.NEXT_PUBLIC_CLIENT_ID ?? "poc-client",
  API_KEY: process.env.NEXT_PUBLIC_API_KEY ?? "poc-secret-key",
  TENANT: process.env.NEXT_PUBLIC_TENANT_ID ?? "00000000-0000-0000-0000-000000000001",
  SERVICE_CODE: process.env.NEXT_PUBLIC_SERVICE_CODE ?? "SALES",
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

type ApiResponse<T> = { success: boolean; data: T; message?: string; errorCode?: string };

async function api<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const res = await fetch(`${config.API}${path}`, init);
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
export async function signUpAndVerify(username: string, email: string, password: string): Promise<void> {
  await api(`/api/auth/sign-up`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ tenantId: config.TENANT, username, email, password, firstName: username, lastName: "User" }),
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

/** Connexion par identifiant + mot de passe → session (token + profil + organisations). */
export async function login(principal: string, password: string): Promise<Session> {
  const res = await api<{
    accessToken: string;
    sharedSession?: { token: string };
    organizations?: Organization[];
  } & Profile>(`/api/auth/login`, {
    method: "POST",
    headers: baseHeaders,
    body: JSON.stringify({ principal, password }),
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
