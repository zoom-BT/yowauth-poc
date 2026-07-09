"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { decodeJwt, type Session, selectContext, updatePlan, enableMfa, confirmMfa, disableMfa, exchangeToken, fetchUserInfo } from "./lib/yowauth";
import { useSession, saveSession } from "./lib/session";
import { PLATFORMS, accessiblePlatforms, otherPlatforms, type Platform } from "./lib/platforms";

function Logo() {
  return (
    <Link href="/" className="logo" style={{ textDecoration: "none", color: "inherit" }}>
      <span className="logo__mark">Y</span>
      <span className="logo__name">Yowyob<small>Compte unique</small></span>
    </Link>
  );
}

export default function Home() {
  const [session, ready] = useSession();
  if (!ready) return <main style={{ minHeight: "100vh" }} />;
  return session ? <Launcher session={session} /> : <Landing />;
}

/* ---------------- Landing (public) ---------------- */
function Landing() {
  return (
    <>
      <nav className="nav">
        <div className="wrap nav__in">
          <Logo />
          <div className="nav__links">
            <a href="#plateformes">Plateformes</a>
            <a href="#pourquoi">Pourquoi Yowyob</a>
            <Link href="/login">Se connecter</Link>
          </div>
          <div className="nav__actions">
            <Link href="/login" className="btn btn-ghost">Se connecter</Link>
            <Link href="/login?mode=signup" className="btn btn-orange">Créer un compte</Link>
          </div>
        </div>
      </nav>

      <section className="wrap hero">
        <div>
          <span className="pill"><span className="dot" /> Portail unique · SSO</span>
          <h1>Un seul compte,<br /><span className="grad">tout l&apos;écosystème Yowyob</span>.</h1>
          <p className="lead">
            Connectez-vous une fois et accédez à toutes vos plateformes métier — ventes, stock,
            RH, comptabilité et plus — avec une identité sécurisée, multi-organisation et vérifiable.
          </p>
          <div className="hero__cta">
            <Link href="/login?mode=signup" className="btn btn-orange btn-lg">Créer un compte →</Link>
            <Link href="/login" className="btn btn-outline btn-lg">Se connecter</Link>
          </div>
          <div className="hero__feats">
            <span>✅ <b>Mise en route &lt; 1 min</b></span>
            <span>🏢 <b>Multi-organisations</b></span>
            <span>🔐 <b>JWT RS256</b></span>
          </div>
        </div>

        <div className="preview" aria-hidden>
          <div className="preview__bar"><i className="r" /><i className="y" /><i className="g" /><span>mes plateformes</span></div>
          <div className="preview__grid">
            {PLATFORMS.slice(0, 9).map((p) => (
              <div key={p.code} className="ptile"><div className="ic">{p.icon}</div><div className="nm">{p.name}</div></div>
            ))}
          </div>
        </div>
      </section>

      <section id="plateformes" className="wrap section">
        <div className="section__head">
          <h2>Toutes vos plateformes, au même endroit</h2>
          <p>Un catalogue de services métier, accessibles selon les droits de votre organisation.</p>
        </div>
        <div className="grid">
          {PLATFORMS.map((p) => (
            <div key={p.code} className="scard">
              <div className="scard__ic" style={{ background: `color-mix(in srgb, ${p.hue} 14%, #fff)` }}>{p.icon}</div>
              <div className="scard__nm">{p.name}</div>
              <div className="scard__tg">{p.tagline}</div>
            </div>
          ))}
        </div>
      </section>

      <section id="pourquoi" className="wrap section">
        <div className="section__head">
          <h2>Pourquoi un compte unique&nbsp;?</h2>
          <p>Yowyob centralise l&apos;identité de toutes vos applications métier.</p>
        </div>
        <div className="grid">
          <div className="scard"><div className="scard__ic">🔑</div><div className="scard__nm">Une identité</div><div className="scard__tg">Un seul compte pour toutes les plateformes, fini les mots de passe multiples.</div></div>
          <div className="scard"><div className="scard__ic">🏢</div><div className="scard__nm">Multi-organisation</div><div className="scard__tg">Accédez à plusieurs entreprises et leurs services depuis le même compte.</div></div>
          <div className="scard"><div className="scard__ic">🛡️</div><div className="scard__nm">Sécurisé &amp; conforme</div><div className="scard__tg">Jetons signés RS256, vérifiables par chaque service, standard OIDC.</div></div>
        </div>
        <div style={{ textAlign: "center", marginTop: 28 }}>
          <Link href="/login?mode=signup" className="btn btn-orange btn-lg">Commencer gratuitement →</Link>
        </div>
      </section>

      <footer className="wrap footer">
        <span>© Yowyob — Portail d&apos;identité (POC).</span>
        <span>Propulsé par YowAuth · JWT RS256 · OIDC</span>
      </footer>
    </>
  );
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/* ---------------- Launcher (connecté) ---------------- */
function Launcher({ session }: { session: Session }) {
  const orgs = session.organizations;
  const [orgIdx, setOrgIdx] = useState(0);
  const org = orgs[orgIdx];
  const mine = useMemo(() => accessiblePlatforms(org?.services ?? []), [org]);
  const discover = useMemo(() => otherPlatforms(org?.services ?? []), [org]);
  const [showSession, setShowSession] = useState(false);
  
  // Session decoding & states
  const claims = decodeJwt(session.accessToken);
  const ssoClaims = decodeJwt(session.ssoToken) as { contexts?: { contextId: string; username: string; email: string; organizationId?: string }[] } | null;
  const p = session.profile;
  const initial = (p.username || "?").charAt(0).toUpperCase();

  // Interactive Plan states
  const [currentPlan, setCurrentPlan] = useState(p.plan || "FREE");
  const [updatingPlan, setUpdatingPlan] = useState(false);

  // Interactive MFA states
  const [mfaActive, setMfaActive] = useState(!!claims?.mfa);
  const [mfaSetupStep, setMfaSetupStep] = useState<"idle" | "verify_otp">("idle");
  const [mfaSetupToken, setMfaSetupToken] = useState("");
  const [mfaSetupCode, setMfaSetupCode] = useState("");
  const [mfaSetupCodePreview, setMfaSetupCodePreview] = useState<string | null>(null);
  const [mfaBusy, setMfaBusy] = useState(false);
  const [mfaError, setMfaError] = useState("");

  // SSO Simulation states
  const [ssoRunning, setSsoRunning] = useState(false);
  const [ssoPlatform, setSsoPlatform] = useState<Platform | null>(null);
  const [ssoLogs, setSsoLogs] = useState<string[]>([]);

  // Function to switch Organization using select-context
  async function handleOrgChange(index: number) {
    const selectedOrg = orgs[index];
    try {
      const contexts = ssoClaims?.contexts ?? [];
      // Attempt to find a context matching organizationId or fall back to the index
      const ctxId = contexts.find(c => c.organizationId === selectedOrg.organizationId)?.contextId 
                    || contexts[index]?.contextId 
                    || contexts[0]?.contextId;

      if (!ctxId) {
        setOrgIdx(index);
        return;
      }

      const selectRes = await selectContext(session.ssoToken, ctxId, selectedOrg.organizationId);
      
      // Update session in memory & local storage
      const newSession: Session = {
        accessToken: selectRes.loginResponse.accessToken,
        ssoToken: selectRes.loginResponse.sharedSession?.token ?? session.ssoToken,
        organizations: selectRes.loginResponse.organizations ?? session.organizations,
        profile: {
          id: selectRes.loginResponse.id,
          username: selectRes.loginResponse.username,
          email: selectRes.loginResponse.email,
          status: selectRes.loginResponse.status,
          plan: selectRes.loginResponse.plan,
          accountType: selectRes.loginResponse.accountType,
          emailVerified: selectRes.loginResponse.emailVerified,
          actorId: selectRes.loginResponse.actorId,
          tenantId: selectRes.loginResponse.tenantId,
        }
      };
      saveSession(newSession);
      setOrgIdx(index);
      // Update plan and MFA states according to new token/profile
      const newClaims = decodeJwt(newSession.accessToken);
      setCurrentPlan(newSession.profile.plan || "FREE");
      setMfaActive(!!newClaims?.mfa);
    } catch (err) {
      console.error("Failed to select context contextually:", err);
      // Fallback
      setOrgIdx(index);
    }
  }

  // Update Plan
  async function handlePlanChange(newPlan: string) {
    setUpdatingPlan(true);
    try {
      const updatedProfile = await updatePlan(session.accessToken, newPlan);
      setCurrentPlan(updatedProfile.plan);
      session.profile.plan = updatedProfile.plan; // update in-memory instance
    } catch (err) {
      alert("Erreur de mise à jour du plan : " + (err instanceof Error ? err.message : String(err)));
    } finally {
      setUpdatingPlan(false);
    }
  }

  // Start MFA Enable Challenge
  async function handleStartEnableMfa() {
    setMfaBusy(true);
    setMfaError("");
    try {
      const res = await enableMfa(session.accessToken, "EMAIL");
      setMfaSetupToken(res.token);
      setMfaSetupCodePreview(res.codePreview || null);
      setMfaSetupStep("verify_otp");
    } catch (err) {
      setMfaError("Échec d'activation : " + (err instanceof Error ? err.message : String(err)));
    } finally {
      setMfaBusy(false);
    }
  }

  // Confirm MFA Activation
  async function handleConfirmMfa() {
    setMfaBusy(true);
    setMfaError("");
    try {
      await confirmMfa(session.accessToken, mfaSetupToken, mfaSetupCode.trim());
      setMfaActive(true);
      setMfaSetupStep("idle");
      setMfaSetupCode("");
      setMfaSetupCodePreview(null);
      alert("Double authentification activée avec succès !");
    } catch (err) {
      setMfaError("Code incorrect ou expiré. Veuillez réessayer.");
    } finally {
      setMfaBusy(false);
    }
  }

  // Disable MFA
  async function handleDisableMfa() {
    if (!confirm("Voulez-vous vraiment désactiver la double authentification (MFA) ?")) return;
    setMfaBusy(true);
    setMfaError("");
    try {
      await disableMfa(session.accessToken);
      setMfaActive(false);
      alert("MFA désactivé avec succès !");
    } catch (err) {
      setMfaError("Échec de désactivation : " + (err instanceof Error ? err.message : String(err)));
    } finally {
      setMfaBusy(false);
    }
  }

  // Intercept Platform click to simulate OIDC Token Exchange
  const handlePlatformClick = async (e: React.MouseEvent, pl: Platform) => {
    e.preventDefault();
    setSsoPlatform(pl);
    setSsoRunning(true);
    setSsoLogs(["[SSO] Démarrage de l'authentification unique (SSO) Yowyob..."]);

    try {
      await sleep(350);
      setSsoLogs((l) => [...l, `[OIDC] Récupération de /.well-known/openid-configuration...`]);
      await sleep(400);

      setSsoLogs((l) => [...l, `[RFC 8693] Envoi de la requête de Token-Exchange au serveur d'autorisation...`]);
      const contexts = ssoClaims?.contexts ?? [];
      const ctxId = contexts[orgIdx]?.contextId || contexts[0]?.contextId || "00000000-0000-0000-0000-000000000001";
      
      const tokenRes = await exchangeToken(session.ssoToken, ctxId, pl.code);
      setSsoLogs((l) => [
        ...l,
        `[RFC 8693] Succès ! Jeton d'accès de service reçu pour le scope: "${tokenRes.scope}"`,
        `[JWT de Service] access_token = "${tokenRes.access_token.slice(0, 40)}..."`
      ]);
      await sleep(550);

      setSsoLogs((l) => [...l, `[OIDC] Validation de l'identité via GET /oauth2/userinfo...`]);
      const userInfo = await fetchUserInfo(tokenRes.access_token);
      setSsoLogs((l) => [
        ...l,
        `[OIDC] Profil validé : username = "${userInfo.username}", sub = "${userInfo.sub}"`,
        `[SSO] Connexion autorisée. Redirection vers ${pl.url}...`
      ]);
      await sleep(650);

      // Open the actual platform URL in a new tab
      window.open(pl.url, "_blank");
      setSsoRunning(false);
      setSsoPlatform(null);
    } catch (err) {
      setSsoLogs((l) => [
        ...l,
        `[ERREUR SSO] Échec de l'échange de jeton : ${err instanceof Error ? err.message : String(err)}`
      ]);
    }
  };

  return (
    <>
      <nav className="nav">
        <div className="wrap nav__in">
          <Logo />
          <div className="nav__actions">
            <Link href="/docs" className="btn btn-ghost">Guide API</Link>
            <Link href="/demo" className="btn btn-ghost">Démo</Link>
            <span className="chip"><span className="av">{initial}</span>{p.username}</span>
            <button className="btn btn-outline" onClick={() => saveSession(null)}>Déconnexion</button>
          </div>
        </div>
      </nav>

      <section className="wrap">
        <div className="lhead">
          <h1>Bonjour, {p.username} 👋</h1>
          <p>Voici les plateformes auxquelles vous avez accès dans votre espace Yowyob.</p>
        </div>

        {orgs.length > 1 && (
          <div className="orgbar">
            <span style={{ color: "var(--muted)", fontSize: 13 }}>Organisation active :</span>
            {orgs.map((o, i) => (
              <button 
                key={o.organizationId} 
                className={`orgtab ${i === orgIdx ? "orgtab--on" : ""}`} 
                onClick={() => handleOrgChange(i)}
              >
                🏢 {o.shortName}
              </button>
            ))}
          </div>
        )}
        {orgs.length === 1 && (
          <div className="orgbar"><span className="orgtab orgtab--on">🏢 {org.shortName}</span></div>
        )}

        <h2 style={{ fontSize: 18, margin: "18px 0 12px" }}>Mes plateformes</h2>
        {mine.length === 0 ? (
          <p style={{ color: "var(--muted)" }}>Aucune plateforme active pour cette organisation.</p>
        ) : (
          <div className="grid">
            {mine.map((pl: Platform) => (
              <a 
                key={pl.code} 
                className="tile" 
                href={pl.url} 
                onClick={(e) => handlePlatformClick(e, pl)}
                style={{ ["--hue" as string]: pl.hue }}
              >
                <div className="tile__ic">{pl.icon}</div>
                <div className="tile__nm">{pl.name}</div>
                <div className="tile__tg">{pl.tagline}</div>
                <div className="tile__go">Lancer en SSO →</div>
              </a>
            ))}
          </div>
        )}

        {discover.length > 0 && (
          <>
            <h2 style={{ fontSize: 18, margin: "34px 0 12px" }}>Découvrir</h2>
            <p style={{ color: "var(--muted)", marginTop: -6, marginBottom: 14, fontSize: 14 }}>Services disponibles sur Yowyob (non activés pour cette organisation).</p>
            <div className="grid">
              {discover.map((pl) => (
                <div key={pl.code} className="scard scard--muted">
                  <div className="scard__ic" style={{ background: `color-mix(in srgb, ${pl.hue} 12%, #fff)` }}>{pl.icon}</div>
                  <div className="scard__nm">{pl.name}</div>
                  <div className="scard__tg">{pl.tagline}</div>
                </div>
              ))}
            </div>
          </>
        )}

        {/* PROFILE & SECURITY CONTROL CENTER */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px", marginTop: "34px" }}>
          
          {/* SECURITY & MFA PANEL */}
          <div className="sess">
            <h3>🔑 Sécurité & MFA</h3>
            <div style={{ margin: "10px 0" }}>
              <span className={`pill ${mfaActive ? "pill--ok" : ""}`} style={{ 
                background: mfaActive ? "#ecfdf5" : "#fef2f2", 
                color: mfaActive ? "#065f46" : "#991b1b",
                borderColor: mfaActive ? "#a7f3d0" : "#fecaca" 
              }}>
                <span className="dot" style={{ background: mfaActive ? "#10b981" : "#ef4444" }} />
                Double Authentification (MFA) : {mfaActive ? "ACTIVÉ" : "DÉSACTIVÉ"}
              </span>
            </div>

            {mfaSetupStep === "idle" ? (
              <div style={{ marginTop: "14px" }}>
                {!mfaActive ? (
                  <button 
                    onClick={handleStartEnableMfa} 
                    className="btn btn-orange" 
                    disabled={mfaBusy}
                    style={{ fontSize: "13px", padding: "8px 14px" }}
                  >
                    {mfaBusy ? "Chargement..." : "Activer la double authentification"}
                  </button>
                ) : (
                  <button 
                    onClick={handleDisableMfa} 
                    className="btn btn-outline" 
                    disabled={mfaBusy}
                    style={{ fontSize: "13px", padding: "8px 14px", color: "#b91c1c", borderColor: "#fecaca" }}
                  >
                    {mfaBusy ? "Chargement..." : "Désactiver la double authentification"}
                  </button>
                )}
              </div>
            ) : (
              <div style={{ marginTop: "14px", borderTop: "1px solid var(--line)", paddingTop: "14px" }}>
                <p style={{ fontSize: "13px", color: "var(--ink-soft)", margin: "0 0 10px" }}>
                  📧 Un code OTP a été généré pour valider l&apos;activation.
                </p>
                {mfaSetupCodePreview && (
                  <div className="alert alert--ok" style={{ marginBottom: "12px", padding: "8px 10px", fontSize: "12px" }}>
                    <strong>Mode Démo :</strong> Code OTP reçu : <code>{mfaSetupCodePreview}</code>
                  </div>
                )}
                <div style={{ display: "flex", gap: "8px" }}>
                  <input 
                    type="text" 
                    className="input" 
                    value={mfaSetupCode} 
                    onChange={(e) => setMfaSetupCode(e.target.value)} 
                    placeholder="Entrez le code"
                    style={{ padding: "8px 10px", fontSize: "13.5px", width: "150px" }}
                  />
                  <button onClick={handleConfirmMfa} className="btn btn-orange" style={{ padding: "8px 14px", fontSize: "13px" }} disabled={mfaBusy}>
                    Valider
                  </button>
                  <button onClick={() => { setMfaSetupStep("idle"); setMfaSetupCode(""); }} className="btn btn-ghost" style={{ padding: "8px 14px", fontSize: "13px" }}>
                    Annuler
                  </button>
                </div>
              </div>
            )}
            {mfaError && <p className="alert alert--err" style={{ marginTop: "12px", fontSize: "12.5px", padding: "8px 10px" }}>{mfaError}</p>}
          </div>

          {/* PROFILE PLAN PANEL */}
          <div className="sess">
            <h3>📊 Abonnement & Profil</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "10px" }}>
              <div>
                <span style={{ fontSize: "13px", color: "var(--muted)" }}>Plan actuel :</span>
                <span className="pill" style={{ marginLeft: "8px", textTransform: "uppercase", fontWeight: 700 }}>
                  {currentPlan}
                </span>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginTop: "8px" }}>
                <label style={{ fontSize: "13px", color: "var(--ink-soft)" }} htmlFor="plan-select">Changer de plan :</label>
                <select 
                  id="plan-select"
                  className="input" 
                  value={currentPlan} 
                  onChange={(e) => handlePlanChange(e.target.value)}
                  disabled={updatingPlan}
                  style={{ width: "auto", padding: "6px 12px", borderRadius: "8px", fontSize: "13.5px" }}
                >
                  <option value="FREE">Gratuit (FREE)</option>
                  <option value="PREMIUM">Professionnel (PREMIUM)</option>
                  <option value="ENTERPRISE">Grande Entreprise (ENTERPRISE)</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* SESSION INFORMATION */}
        <div className="sess" style={{ marginTop: "20px" }}>
          <h3 style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            Ma session (Profil & Claims JWT)
            <button className="btn btn-ghost" style={{ fontSize: 12, padding: "4px 10px" }} onClick={() => setShowSession((v) => !v)}>
              {showSession ? "masquer les détails" : "afficher les détails"}
            </button>
          </h3>
          <dl className="kv">
            <dt>Email</dt><dd>{p.email}</dd>
            <dt>Statut</dt><dd>{p.status}</dd>
            <dt>Tenant</dt><dd>{p.tenantId}</dd>
            {claims && Array.isArray(claims.permissions) && (
              <>
                <dt>Privilèges / Rôles</dt>
                <dd style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                  {(claims.permissions as string[]).map((perm) => (
                    <span key={perm} className="pill" style={{ textTransform: "none", fontSize: "11px", padding: "3px 8px", background: "var(--violet-soft)", color: "var(--brand-ink)", borderColor: "#e6ddff" }}>
                      {perm}
                    </span>
                  ))}
                </dd>
              </>
            )}
          </dl>
          {showSession && <pre className="jwt">{JSON.stringify(claims, null, 2)}</pre>}
        </div>
      </section>

      {/* SSO SIMULATION MODAL */}
      {ssoRunning && ssoPlatform && (
        <div style={{
          position: "fixed",
          inset: 0,
          background: "rgba(30, 27, 46, 0.7)",
          backdropFilter: "blur(4px)",
          display: "grid",
          placeItems: "center",
          zIndex: 9999
        }}>
          <div className="authcard" style={{ maxWidth: "550px", width: "90%" }}>
            <div style={{ textAlign: "center", marginBottom: "16px" }}>
              <div style={{ fontSize: "40px", marginBottom: "10px" }}>{ssoPlatform.icon}</div>
              <h3>SSO : Lancement de {ssoPlatform.name}</h3>
              <p style={{ color: "var(--muted)", fontSize: "13.5px", margin: "4px 0 0" }}>
                Simulation d&apos;authentification unique (OIDC Token-Exchange)
              </p>
            </div>
            
            <div style={{
              background: "#0b1021",
              borderRadius: "10px",
              padding: "14px",
              fontFamily: "var(--mono)",
              fontSize: "12px",
              color: "#fff",
              height: "200px",
              overflowY: "auto",
              border: "1px solid var(--line)",
              display: "flex",
              flexDirection: "column",
              gap: "6px",
              textAlign: "left"
            }}>
              {ssoLogs.map((logStr, i) => {
                let color = "#4ade80"; // green for success / standard info
                if (logStr.startsWith("[OIDC]")) color = "#38bdf8"; // blue
                if (logStr.startsWith("[RFC 8693]")) color = "#f97316"; // orange
                if (logStr.startsWith("[ERREUR SSO]")) color = "#f87171"; // red
                return (
                  <div key={i} style={{ color }}>
                    {logStr}
                  </div>
                );
              })}
            </div>
            
            <div style={{ marginTop: "16px", display: "flex", justifyContent: "flex-end" }}>
              <button 
                onClick={() => { setSsoRunning(false); setSsoPlatform(null); }} 
                className="btn btn-outline"
                style={{ fontSize: "13px", padding: "8px 16px" }}
              >
                Annuler
              </button>
            </div>
          </div>
        </div>
      )}

      <footer className="wrap footer">
        <span>© Yowyob — Portail d&apos;identité (POC).</span>
        <span>Connecté via YowAuth · JWT RS256</span>
      </footer>
    </>
  );
}
