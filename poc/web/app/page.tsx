"use client";

import { useState } from "react";

const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const CLIENT_ID = process.env.NEXT_PUBLIC_CLIENT_ID ?? "poc-client";
const API_KEY = process.env.NEXT_PUBLIC_API_KEY ?? "poc-secret-key";
const TENANT = process.env.NEXT_PUBLIC_TENANT_ID ?? "00000000-0000-0000-0000-000000000001";
const SERVICE_CODE = process.env.NEXT_PUBLIC_SERVICE_CODE ?? "SALES";

const baseHeaders: Record<string, string> = {
  "Content-Type": "application/json",
  "X-Client-Id": CLIENT_ID,
  "X-Api-Key": API_KEY,
  "X-Tenant-Id": TENANT,
};

type Step = { label: string; status: number; ok: boolean; body: unknown };

function decodeJwt(token: string): unknown {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(decodeURIComponent(escape(atob(payload))));
  } catch {
    return null;
  }
}

export default function Home() {
  const [steps, setSteps] = useState<Step[]>([]);
  const [token, setToken] = useState<string>("");
  const [orgs, setOrgs] = useState<unknown[]>([]);
  const [running, setRunning] = useState(false);

  async function call(label: string, url: string, init?: RequestInit): Promise<unknown> {
    const res = await fetch(url, init);
    let body: unknown;
    const text = await res.text();
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
    setSteps((prev) => [...prev, { label, status: res.status, ok: res.ok, body }]);
    return body;
  }

  async function run() {
    setRunning(true);
    setSteps([]);
    setToken("");
    setOrgs([]);
    try {
      const user = `poc_${Date.now()}`;
      const pw = "P@ssw0rd!2024";

      await call("1. openid-configuration", `${API}/.well-known/openid-configuration`);
      await call("2. JWKS (cle publique RS256)", `${API}/.well-known/jwks.json`);

      await call("3. sign-up", `${API}/api/auth/sign-up`, {
        method: "POST",
        headers: baseHeaders,
        body: JSON.stringify({
          tenantId: TENANT,
          username: user,
          email: `${user}@demo.io`,
          password: pw,
          firstName: "Demo",
          lastName: "User",
        }),
      });

      const resend = (await call("4. email-verification (mode PREVIEW)", `${API}/api/auth/email-verification/resend`, {
        method: "POST",
        headers: baseHeaders,
        body: JSON.stringify({ principal: user }),
      })) as { data?: { challengeTokenPreview?: string } };
      const vtoken = resend?.data?.challengeTokenPreview ?? "";

      await call("5. email-verification/confirm", `${API}/api/auth/email-verification/confirm`, {
        method: "POST",
        headers: baseHeaders,
        body: JSON.stringify({ verificationToken: vtoken }),
      });

      const login = (await call("6. login (+ organisations)", `${API}/api/auth/login`, {
        method: "POST",
        headers: baseHeaders,
        body: JSON.stringify({ principal: user, password: pw }),
      })) as {
        data?: { accessToken?: string; sharedSession?: { token?: string }; organizations?: unknown[] };
      };
      const access = login?.data?.accessToken ?? "";
      const sso = login?.data?.sharedSession?.token ?? "";
      setToken(access);
      setOrgs(login?.data?.organizations ?? []);

      await call("7. users/me (bearer)", `${API}/api/users/me`, {
        headers: { ...baseHeaders, Authorization: `Bearer ${access}` },
      });

      const ctx = (decodeJwt(sso) as { contexts?: { contextId?: string }[] })?.contexts?.[0]?.contextId ?? "";
      const form = new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:token-exchange",
        subject_token: sso,
        subject_token_type: "urn:ietf:params:oauth:token-type:jwt",
        context_id: ctx,
        service_code: SERVICE_CODE,
        client_id: CLIENT_ID,
        client_secret: API_KEY,
      });
      const xchg = (await call("8. oauth2/token (token-exchange)", `${API}/oauth2/token`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: form.toString(),
      })) as { access_token?: string };

      await call("9. oauth2/userinfo", `${API}/oauth2/userinfo`, {
        headers: { Authorization: `Bearer ${xchg?.access_token ?? ""}` },
      });
    } catch (e) {
      setSteps((prev) => [...prev, { label: "ERREUR", status: 0, ok: false, body: String(e) }]);
    } finally {
      setRunning(false);
    }
  }

  return (
    <main style={{ fontFamily: "system-ui, sans-serif", maxWidth: 920, margin: "0 auto", padding: "2rem 1rem" }}>
      <h1 style={{ marginBottom: 4 }}>YowAuth — POC</h1>
      <p style={{ color: "#666", marginTop: 0 }}>
        Fournisseur d&apos;identite (IdP) — flux complet contre <code>{API}</code>
      </p>

      <button
        onClick={run}
        disabled={running}
        style={{
          padding: "0.7rem 1.4rem",
          fontSize: 16,
          fontWeight: 600,
          color: "#fff",
          background: running ? "#888" : "#2563eb",
          border: "none",
          borderRadius: 8,
          cursor: running ? "default" : "pointer",
        }}
      >
        {running ? "Execution…" : "▶ Lancer le flux sign-up → login → JWT → userinfo"}
      </button>

      {orgs.length > 0 && (
        <section style={{ marginTop: 24 }}>
          <h3>Organisations accessibles</h3>
          <pre style={pre}>{JSON.stringify(orgs, null, 2)}</pre>
        </section>
      )}

      {token && (
        <section style={{ marginTop: 24 }}>
          <h3>JWT (access token) — decode</h3>
          <pre style={pre}>{JSON.stringify(decodeJwt(token), null, 2)}</pre>
        </section>
      )}

      <section style={{ marginTop: 24 }}>
        <h3>Etapes</h3>
        {steps.map((s, i) => (
          <details
            key={i}
            open={!s.ok}
            style={{ marginBottom: 8, border: "1px solid #e5e7eb", borderRadius: 8, padding: "8px 12px" }}
          >
            <summary style={{ cursor: "pointer", fontWeight: 600 }}>
              <span style={{ color: s.ok ? "#16a34a" : "#dc2626" }}>{s.ok ? "✓" : "✗"}</span> {s.label}{" "}
              <span style={{ color: "#888", fontWeight: 400 }}>[HTTP {s.status}]</span>
            </summary>
            <pre style={pre}>{JSON.stringify(s.body, null, 2)}</pre>
          </details>
        ))}
      </section>
    </main>
  );
}

const pre: React.CSSProperties = {
  background: "#0b1021",
  color: "#7CFC9B",
  padding: "1rem",
  borderRadius: 8,
  overflow: "auto",
  fontSize: 13,
  lineHeight: 1.5,
};
