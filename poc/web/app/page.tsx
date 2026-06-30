"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { decodeJwt, login, signUpAndVerify, type Session } from "./lib/yowauth";

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

  function switchMode(m: "login" | "signup") {
    setMode(m);
    setError("");
    setNotice("");
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      if (mode === "signup") {
        await signUpAndVerify(principal.trim(), email.trim(), password);
        persist(await login(principal.trim(), password));
        setNotice("Compte créé et email vérifié automatiquement.");
      } else {
        persist(await login(principal.trim(), password));
      }
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
    <main className="auth">
      <aside className="brand" style={{ "--brand-img": "url(/img1.png)" } as React.CSSProperties}>
        <div className="brand__top">
          <div className="logo">Y</div>
          <span className="brand__wordmark">YowAuth</span>
        </div>

        <div className="brand__hero">
          <p className="brand__eyebrow">Fournisseur d&apos;identité Yowyob</p>
          <h1 className="brand__title">Une seule identité pour tout l&apos;écosystème.</h1>
          <p className="brand__sub">
            Connectez-vous une fois et accédez à vos organisations et services Yowyob,
            avec des jetons signés et vérifiables.
          </p>
        </div>

        <div className="brand__trust">
          <div className="trust"><span className="trust__dot">🔐</span> Jetons JWT signés RS256</div>
          <div className="trust"><span className="trust__dot">🏢</span> Multi-tenant &amp; multi-organisation</div>
          <div className="trust"><span className="trust__dot">🔗</span> Endpoints OIDC standard</div>
        </div>
      </aside>

      <section className="panel">
        <div className="form">
          <div className="form__head">
            <h2 className="form__title">{mode === "login" ? "Connexion" : "Créer un compte"}</h2>
            <p className="form__hint">
              {mode === "login" ? "Accédez à votre compte YowAuth." : "Quelques secondes suffisent."}
            </p>
          </div>

          <div className="tabs" role="tablist">
            <button type="button" role="tab" aria-selected={mode === "login"} className={`tab ${mode === "login" ? "tab--on" : ""}`} onClick={() => switchMode("login")}>
              Connexion
            </button>
            <button type="button" role="tab" aria-selected={mode === "signup"} className={`tab ${mode === "signup" ? "tab--on" : ""}`} onClick={() => switchMode("signup")}>
              Inscription
            </button>
          </div>

          <form onSubmit={onSubmit}>
            <div className="field">
              <label htmlFor="principal">{mode === "login" ? "Identifiant ou email" : "Nom d'utilisateur"}</label>
              <input id="principal" className="input" value={principal} onChange={(e) => setPrincipal(e.target.value)} placeholder="ex : alice" autoComplete="username" required />
            </div>

            {mode === "signup" && (
              <div className="field">
                <label htmlFor="email">Email</label>
                <input id="email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="alice@exemple.io" autoComplete="email" required />
              </div>
            )}

            <div className="field">
              <label htmlFor="password">Mot de passe</label>
              <input id="password" className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" autoComplete={mode === "login" ? "current-password" : "new-password"} required />
            </div>

            <button type="submit" className="btn" disabled={busy}>
              {busy ? "Veuillez patienter…" : mode === "login" ? "Se connecter" : "Créer le compte"}
            </button>
          </form>

          {error && <p className="alert alert--err">{error}</p>}
          {notice && <p className="alert alert--ok">{notice}</p>}

          <p className="form__foot">
            <Link href="/demo">Voir la démo technique du flux complet →</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

function Dashboard({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const claims = decodeJwt(session.accessToken);
  const p = session.profile;
  return (
    <main className="dash">
      <header className="dash__banner" style={{ "--brand-img": "url(/img2.png)" } as React.CSSProperties}>
        <div className="dash__bar">
          <div>
            <h1 className="dash__hello">Bonjour, {p.username}</h1>
            <p className="dash__status">● Connecté à YowAuth</p>
          </div>
          <button className="btn-ghost" onClick={onLogout}>Déconnexion</button>
        </div>
      </header>

      <div className="dash__body">
        <div className="cardx">
          <h3>Profil</h3>
          <dl className="kv">
            <dt>Email</dt><dd>{p.email}</dd>
            <dt>Statut</dt><dd>{p.status}</dd>
            <dt>Plan</dt><dd>{p.plan}</dd>
            <dt>Type de compte</dt><dd>{p.accountType}</dd>
            <dt>Email vérifié</dt><dd>{String(p.emailVerified)}</dd>
            <dt>Tenant</dt><dd>{p.tenantId}</dd>
            <dt>Actor ID</dt><dd>{p.actorId}</dd>
          </dl>
        </div>

        <div className="cardx">
          <h3>Organisations accessibles</h3>
          {session.organizations.length === 0 ? (
            <p style={{ color: "var(--muted)", margin: 0 }}>Aucune organisation.</p>
          ) : (
            session.organizations.map((o) => (
              <div key={o.organizationId} className="org">
                <div>
                  <span className="org__name">{o.shortName}</span>{" "}
                  <span className="org__code">({o.organizationCode})</span>
                </div>
                <div className="org__legal">{o.longName}</div>
                <div>{o.services.map((s) => <span key={s} className="badge">{s}</span>)}</div>
              </div>
            ))
          )}
        </div>

        <div className="cardx">
          <h3>Access token (JWT RS256) — claims décodés</h3>
          <pre className="jwt">{JSON.stringify(claims, null, 2)}</pre>
        </div>

        <p style={{ textAlign: "center", fontSize: 13 }}>
          <Link href="/demo">Voir la démo technique du flux complet →</Link>
        </p>
      </div>
    </main>
  );
}
