"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { config, decodeJwt, login, signUpAndVerify, type Session } from "./lib/yowauth";

const STORAGE_KEY = "yowauth.session";

export default function Home() {
  const [session, setSession] = useState<Session | null>(null);
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [principal, setPrincipal] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    const raw = typeof window !== "undefined" ? localStorage.getItem(STORAGE_KEY) : null;
    if (raw) {
      try {
        setSession(JSON.parse(raw) as Session);
      } catch {
        /* ignore */
      }
    }
  }, []);

  function persist(s: Session | null) {
    setSession(s);
    if (typeof window !== "undefined") {
      if (s) localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
      else localStorage.removeItem(STORAGE_KEY);
    }
  }

  async function onLogin(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      persist(await login(principal.trim(), password));
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  async function onSignup(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await signUpAndVerify(principal.trim(), email.trim(), password);
      // Email auto-vérifié en coulisse → connexion directe.
      persist(await login(principal.trim(), password));
      setNotice("Compte créé, email vérifié automatiquement, connecté ✓");
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  if (session) {
    return <Dashboard session={session} onLogout={() => persist(null)} />;
  }

  return (
    <main style={shell}>
      <div style={card}>
        <h1 style={{ margin: "0 0 2px", fontSize: 26 }}>YowAuth</h1>
        <p style={{ marginTop: 0, color: "#64748b", fontSize: 14 }}>
          Connexion via l&apos;IdP <code>{config.API.replace(/^https?:\/\//, "")}</code>
        </p>

        <div style={tabs}>
          <button onClick={() => setMode("login")} style={tab(mode === "login")}>Connexion</button>
          <button onClick={() => setMode("signup")} style={tab(mode === "signup")}>Inscription</button>
        </div>

        <form onSubmit={mode === "login" ? onLogin : onSignup}>
          <label style={lbl}>Identifiant {mode === "login" ? "(username ou email)" : "(username)"}</label>
          <input style={inp} value={principal} onChange={(e) => setPrincipal(e.target.value)} placeholder="ex: alice" required />

          {mode === "signup" && (
            <>
              <label style={lbl}>Email</label>
              <input style={inp} type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="alice@demo.io" required />
            </>
          )}

          <label style={lbl}>Mot de passe</label>
          <input style={inp} type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" required />

          <button type="submit" disabled={busy} style={primaryBtn(busy)}>
            {busy ? "…" : mode === "login" ? "Se connecter" : "Créer le compte"}
          </button>
        </form>

        {error && <p style={errorBox}>{error}</p>}
        {notice && <p style={noticeBox}>{notice}</p>}

        <p style={{ marginTop: 20, fontSize: 13, color: "#94a3b8", textAlign: "center" }}>
          <Link href="/demo" style={{ color: "#2563eb" }}>Voir la démo automatique du flux complet →</Link>
        </p>
      </div>
    </main>
  );
}

function Dashboard({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const claims = decodeJwt(session.accessToken);
  const p = session.profile;
  return (
    <main style={shell}>
      <div style={{ ...card, maxWidth: 720 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>Bienvenue, {p.username} 👋</h1>
          <button onClick={onLogout} style={logoutBtn}>Déconnexion</button>
        </div>
        <p style={{ color: "#16a34a", fontWeight: 600, marginTop: 6 }}>● Connecté à YowAuth</p>

        <h3 style={h3}>Profil</h3>
        <dl style={dl}>
          <Row k="Email" v={p.email} />
          <Row k="Statut" v={p.status} />
          <Row k="Plan" v={p.plan} />
          <Row k="Type de compte" v={p.accountType} />
          <Row k="Email vérifié" v={String(p.emailVerified)} />
          <Row k="Tenant" v={p.tenantId} />
          <Row k="Actor ID" v={p.actorId} />
        </dl>

        <h3 style={h3}>Organisations accessibles</h3>
        {session.organizations.length === 0 ? (
          <p style={{ color: "#64748b" }}>Aucune organisation.</p>
        ) : (
          session.organizations.map((o) => (
            <div key={o.organizationId} style={orgCard}>
              <strong>{o.shortName}</strong> <span style={{ color: "#64748b" }}>({o.organizationCode})</span>
              <div style={{ fontSize: 13, color: "#475569" }}>{o.longName}</div>
              <div style={{ marginTop: 4 }}>
                {o.services.map((s) => (
                  <span key={s} style={badge}>{s}</span>
                ))}
              </div>
            </div>
          ))
        )}

        <h3 style={h3}>Access token (JWT RS256) — claims décodés</h3>
        <pre style={pre}>{JSON.stringify(claims, null, 2)}</pre>

        <p style={{ marginTop: 16, fontSize: 13 }}>
          <Link href="/demo" style={{ color: "#2563eb" }}>Voir la démo automatique du flux complet →</Link>
        </p>
      </div>
    </main>
  );
}

function Row({ k, v }: { k: string; v: string }) {
  return (
    <>
      <dt style={{ color: "#64748b" }}>{k}</dt>
      <dd style={{ margin: 0, fontFamily: "ui-monospace, monospace", wordBreak: "break-all" }}>{v}</dd>
    </>
  );
}

const shell: React.CSSProperties = {
  fontFamily: "system-ui, sans-serif",
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "linear-gradient(135deg,#eef2ff,#f8fafc)",
  padding: "2rem 1rem",
};
const card: React.CSSProperties = {
  width: "100%",
  maxWidth: 400,
  background: "#fff",
  borderRadius: 16,
  boxShadow: "0 10px 40px rgba(2,6,23,.10)",
  padding: "28px 26px",
};
const tabs: React.CSSProperties = { display: "flex", gap: 8, margin: "18px 0 16px" };
const tab = (active: boolean): React.CSSProperties => ({
  flex: 1,
  padding: "8px 0",
  borderRadius: 8,
  border: "1px solid " + (active ? "#2563eb" : "#e2e8f0"),
  background: active ? "#2563eb" : "#fff",
  color: active ? "#fff" : "#334155",
  fontWeight: 600,
  cursor: "pointer",
});
const lbl: React.CSSProperties = { display: "block", fontSize: 13, color: "#475569", margin: "10px 0 4px" };
const inp: React.CSSProperties = {
  width: "100%",
  padding: "10px 12px",
  borderRadius: 8,
  border: "1px solid #cbd5e1",
  fontSize: 15,
  boxSizing: "border-box",
};
const primaryBtn = (busy: boolean): React.CSSProperties => ({
  width: "100%",
  marginTop: 18,
  padding: "11px 0",
  borderRadius: 8,
  border: "none",
  background: busy ? "#94a3b8" : "#2563eb",
  color: "#fff",
  fontSize: 15,
  fontWeight: 700,
  cursor: busy ? "default" : "pointer",
});
const logoutBtn: React.CSSProperties = {
  padding: "8px 14px",
  borderRadius: 8,
  border: "1px solid #e2e8f0",
  background: "#fff",
  color: "#334155",
  cursor: "pointer",
  fontWeight: 600,
};
const errorBox: React.CSSProperties = { marginTop: 14, color: "#b91c1c", background: "#fef2f2", padding: "10px 12px", borderRadius: 8, fontSize: 14 };
const noticeBox: React.CSSProperties = { marginTop: 14, color: "#166534", background: "#f0fdf4", padding: "10px 12px", borderRadius: 8, fontSize: 14 };
const h3: React.CSSProperties = { marginTop: 24, marginBottom: 8, fontSize: 15, color: "#0f172a" };
const dl: React.CSSProperties = { display: "grid", gridTemplateColumns: "140px 1fr", gap: "6px 12px", fontSize: 14 };
const orgCard: React.CSSProperties = { border: "1px solid #e2e8f0", borderRadius: 10, padding: "10px 12px", marginBottom: 8 };
const badge: React.CSSProperties = { display: "inline-block", background: "#eef2ff", color: "#3730a3", borderRadius: 6, padding: "2px 8px", fontSize: 12, marginRight: 6, fontWeight: 600 };
const pre: React.CSSProperties = { background: "#0b1021", color: "#7CFC9B", padding: "1rem", borderRadius: 8, overflow: "auto", fontSize: 12.5, lineHeight: 1.5 };
