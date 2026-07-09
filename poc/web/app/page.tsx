"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { decodeJwt, type Session, selectContext, updatePlan, enableMfa, confirmMfa, disableMfa, exchangeToken, fetchUserInfo } from "./lib/yowauth";
import { useSession, saveSession } from "./lib/session";
import { PLATFORMS, accessiblePlatforms, otherPlatforms, type Platform } from "./lib/platforms";

function PlatformIcon({ icon, name, className }: { icon: string; name: string; className?: string }) {
  if (icon.startsWith("http")) {
    return (
      <img
        src={icon}
        alt={name}
        className={className}
        style={{
          width: "100%",
          height: "100%",
          objectFit: "cover",
          borderRadius: "inherit"
        }}
      />
    );
  }
  return <span style={{ fontSize: "inherit" }}>{icon}</span>;
}

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
              <div key={p.code} className="ptile">
                <div className="ic" style={{ width: "32px", height: "32px", margin: "0 auto", borderRadius: "8px", overflow: "hidden", display: "grid", placeItems: "center" }}>
                  <PlatformIcon icon={p.icon} name={p.name} />
                </div>
                <div className="nm">{p.name}</div>
              </div>
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
              <div className="scard__ic" style={{ background: `color-mix(in srgb, ${p.hue} 14%, #fff)`, overflow: "hidden" }}>
                <PlatformIcon icon={p.icon} name={p.name} />
              </div>
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
            <span className="chip" style={{ background: "var(--violet-soft)", color: "var(--brand-ink)", fontWeight: 600 }}>
              <span className="av">{initial}</span>
              {p.username}
            </span>
            <button className="btn btn-outline" style={{ borderColor: "#fecaca", color: "#b91c1c" }} onClick={() => saveSession(null)}>Déconnexion</button>
          </div>
        </div>
      </nav>

      <section className="wrap dash">
        
        {/* LEFT COLUMN: platforms */}
        <div>
          <div className="lhead" style={{ marginBottom: "24px" }}>
            <span className="pill" style={{ textTransform: "uppercase", background: "var(--indigo-soft)", color: "var(--indigo)", padding: "4px 10px", fontSize: "11px", fontWeight: 700 }}>
              🏢 {org.shortName}
            </span>
            <h1 style={{ marginTop: "10px", marginBottom: "6px" }}>Bonjour, {p.username} 👋</h1>
            <p style={{ color: "var(--muted)", fontSize: "15px" }}>
              Bienvenue sur votre portail unique Yowyob. Choisissez un service pour y accéder.
            </p>
          </div>

          {orgs.length > 1 && (
            <div className="orgbar" style={{ marginBottom: "28px" }}>
              <span style={{ color: "var(--muted)", fontSize: "13px", fontWeight: 600, marginRight: "12px" }}>Espace d&apos;organisation :</span>
              <div style={{ display: "inline-flex", gap: "8px" }}>
                {orgs.map((o, i) => (
                  <button 
                    key={o.organizationId} 
                    className={`orgtab ${i === orgIdx ? "orgtab--on" : ""}`} 
                    onClick={() => handleOrgChange(i)}
                    style={{ fontSize: "13.5px", padding: "6px 14px", borderRadius: "10px" }}
                  >
                    {o.shortName}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div style={{ background: "#fafafa", borderRadius: "20px", padding: "24px", border: "1px solid var(--line)" }}>
            <h2 style={{ fontSize: "18px", margin: "0 0 16px", fontWeight: 700, color: "var(--brand-ink)", display: "flex", alignItems: "center", gap: "8px" }}>
              🚀 Mes Applications Actives
            </h2>
            {mine.length === 0 ? (
              <p style={{ color: "var(--muted)", fontSize: "14px" }}>Aucune plateforme active pour cette organisation.</p>
            ) : (
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "16px" }}>
                {mine.map((pl: Platform) => (
                  <a 
                    key={pl.code} 
                    className="tile" 
                    href={pl.url} 
                    onClick={(e) => handlePlatformClick(e, pl)}
                    style={{ 
                      ["--hue" as string]: pl.hue,
                      padding: "16px",
                      borderRadius: "14px",
                      background: "#fff",
                      border: "1px solid var(--line)",
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "space-between",
                      minHeight: "160px",
                      transition: "all 0.2s"
                    }}
                  >
                    <div>
                      <div className="tile__ic" style={{ width: "48px", height: "48px", borderRadius: "10px", overflow: "hidden", display: "grid", placeItems: "center" }}>
                        <PlatformIcon icon={pl.icon} name={pl.name} />
                      </div>
                      <div className="tile__nm" style={{ fontSize: "15px", fontWeight: 700, margin: "12px 0 2px" }}>{pl.name}</div>
                      <div className="tile__tg" style={{ fontSize: "12px", color: "var(--muted)" }}>{pl.tagline}</div>
                    </div>
                    <div className="tile__go" style={{ fontSize: "12px", marginTop: "12px", color: "var(--hue, var(--violet))" }}>Lancer en SSO →</div>
                  </a>
                ))}
              </div>
            )}
          </div>

          {discover.length > 0 && (
            <div style={{ marginTop: "32px", background: "#fff", borderRadius: "20px", padding: "24px", border: "1px solid var(--line)" }}>
              <h2 style={{ fontSize: "17px", margin: "0 0 4px", fontWeight: 700, color: "var(--brand-ink)" }}>
                🌟 Services Complémentaires
              </h2>
              <p style={{ color: "var(--muted)", fontSize: "13px", marginBottom: "16px" }}>
                Services disponibles sur Yowyob non activés pour {org.shortName}.
              </p>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "12px" }}>
                {discover.map((pl) => (
                  <div key={pl.code} className="scard scard--muted" style={{ padding: "14px", borderRadius: "12px", border: "1px solid var(--line)", display: "flex", gap: "12px", alignItems: "center" }}>
                    <div className="scard__ic" style={{ width: "36px", height: "36px", borderRadius: "8px", overflow: "hidden", display: "grid", placeItems: "center", flexShrink: 0 }}>
                      <PlatformIcon icon={pl.icon} name={pl.name} />
                    </div>
                    <div>
                      <div className="scard__nm" style={{ fontSize: "13.5px", fontWeight: 700, margin: 0 }}>{pl.name}</div>
                      <div className="scard__tg" style={{ fontSize: "11px", color: "var(--muted)" }}>{pl.tagline}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* RIGHT COLUMN: profile and security details */}
        <div>
          <div className="sess" style={{ padding: "20px", borderRadius: "16px", border: "1px solid var(--line)", background: "#fff", position: "sticky", top: "20px" }}>
            
            {/* PROFILE HEAD */}
            <div style={{ textAlign: "center", borderBottom: "1px solid var(--line)", paddingBottom: "20px", marginBottom: "20px" }}>
              <div style={{ width: "64px", height: "64px", borderRadius: "50%", background: "var(--indigo-soft)", color: "var(--indigo)", fontSize: "28px", fontWeight: 700, display: "grid", placeItems: "center", margin: "0 auto 12px" }}>
                {initial}
              </div>
              <h3 style={{ fontSize: "16px", margin: "0 0 4px" }}>{p.username}</h3>
              <span className="pill pill--ok" style={{ fontSize: "11px", textTransform: "none", padding: "2px 8px" }}>
                Compte {p.status}
              </span>
            </div>

            {/* ABONNEMENT SECTION */}
            <div style={{ marginBottom: "20px" }}>
              <h4 style={{ fontSize: "12px", textTransform: "uppercase", color: "var(--muted)", margin: "0 0 8px", letterSpacing: "0.05em" }}>
                Abonnement
              </h4>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", background: "var(--violet-soft)", padding: "10px 14px", borderRadius: "10px", marginBottom: "10px" }}>
                <span style={{ fontSize: "13.5px", fontWeight: 600, color: "var(--brand-ink)" }}>Plan actuel</span>
                <span className="pill" style={{ background: currentPlan === "ENTERPRISE" ? "linear-gradient(135deg, #7c3aed, #db2777)" : currentPlan === "PREMIUM" ? "var(--indigo)" : "var(--muted)", color: "#fff", border: "none", fontSize: "11px", fontWeight: 800 }}>
                  {currentPlan}
                </span>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                <label htmlFor="plan-select" style={{ fontSize: "11.5px", color: "var(--muted)" }}>Changer d&apos;abonnement :</label>
                <select 
                  id="plan-select"
                  className="input" 
                  value={currentPlan} 
                  onChange={(e) => handlePlanChange(e.target.value)}
                  disabled={updatingPlan}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: "8px", fontSize: "13px" }}
                >
                  <option value="FREE">Gratuit (FREE)</option>
                  <option value="PREMIUM">Professionnel (PREMIUM)</option>
                  <option value="ENTERPRISE">Entreprise (ENTERPRISE)</option>
                </select>
              </div>
            </div>

            {/* SECURITE & MFA SECTION */}
            <div style={{ marginBottom: "20px", borderTop: "1px solid var(--line)", paddingTop: "20px" }}>
              <h4 style={{ fontSize: "12px", textTransform: "uppercase", color: "var(--muted)", margin: "0 0 8px", letterSpacing: "0.05em" }}>
                Sécurité
              </h4>
              <div style={{ margin: "10px 0" }}>
                <span className={`pill ${mfaActive ? "pill--ok" : ""}`} style={{ 
                  width: "100%",
                  justifyContent: "center",
                  background: mfaActive ? "#ecfdf5" : "#fef2f2", 
                  color: mfaActive ? "#065f46" : "#991b1b",
                  borderColor: mfaActive ? "#a7f3d0" : "#fecaca" 
                }}>
                  <span className="dot" style={{ background: mfaActive ? "#10b981" : "#ef4444" }} />
                  Double Authentification (MFA) : {mfaActive ? "Actif" : "Inactif"}
                </span>
              </div>

              {mfaSetupStep === "idle" ? (
                <div style={{ marginTop: "10px" }}>
                  {!mfaActive ? (
                    <button 
                      onClick={handleStartEnableMfa} 
                      className="btn btn-orange" 
                      disabled={mfaBusy}
                      style={{ fontSize: "12.5px", padding: "8px 12px", width: "100%" }}
                    >
                      {mfaBusy ? "Chargement..." : "Activer le MFA"}
                    </button>
                  ) : (
                    <button 
                      onClick={handleDisableMfa} 
                      className="btn btn-outline" 
                      disabled={mfaBusy}
                      style={{ fontSize: "12.5px", padding: "8px 12px", width: "100%", color: "#b91c1c", borderColor: "#fecaca" }}
                    >
                      {mfaBusy ? "Chargement..." : "Désactiver le MFA"}
                    </button>
                  )}
                </div>
              ) : (
                <div style={{ marginTop: "12px", border: "1px solid #ffd8a8", background: "#fff9db", borderRadius: "10px", padding: "12px" }}>
                  <p style={{ fontSize: "12px", color: "#d9480f", margin: "0 0 8px", fontWeight: 600 }}>
                    📧 Validation requise :
                  </p>
                  {mfaSetupCodePreview && (
                    <div className="alert alert--ok" style={{ marginBottom: "10px", padding: "6px 8px", fontSize: "11.5px" }}>
                      Code OTP : <code>{mfaSetupCodePreview}</code>
                    </div>
                  )}
                  <div style={{ display: "flex", gap: "6px" }}>
                    <input 
                      type="text" 
                      className="input" 
                      value={mfaSetupCode} 
                      onChange={(e) => setMfaSetupCode(e.target.value)} 
                      placeholder="Code OTP"
                      style={{ padding: "6px 8px", fontSize: "12px", flexGrow: 1 }}
                    />
                    <button onClick={handleConfirmMfa} className="btn btn-orange" style={{ padding: "6px 10px", fontSize: "12px" }} disabled={mfaBusy}>
                      Ok
                    </button>
                  </div>
                  <button onClick={() => { setMfaSetupStep("idle"); setMfaSetupCode(""); }} className="btn btn-ghost" style={{ fontSize: "11px", padding: "4px 0", marginTop: "6px", width: "100%", textAlign: "center" }}>
                    Annuler
                  </button>
                </div>
              )}
              {mfaError && <p className="alert alert--err" style={{ marginTop: "10px", fontSize: "12px", padding: "6px 8px" }}>{mfaError}</p>}
            </div>

            {/* DETAILS DE LA SESSION */}
            <div style={{ borderTop: "1px solid var(--line)", paddingTop: "20px" }}>
              <h4 style={{ fontSize: "12px", textTransform: "uppercase", color: "var(--muted)", margin: "0 0 8px", letterSpacing: "0.05em" }}>
                Session & Droits
              </h4>
              <p style={{ fontSize: "12px", color: "var(--ink-soft)", margin: "0 0 10px" }}>
                <strong>Tenant :</strong> <code style={{ fontSize: "11px" }}>{p.tenantId}</code>
              </p>
              
              {claims && Array.isArray(claims.permissions) && (
                <div style={{ marginBottom: "14px" }}>
                  <div style={{ fontSize: "11.5px", fontWeight: 600, color: "var(--muted)", marginBottom: "6px" }}>Permissions (claims JWT) :</div>
                  <div style={{ display: "flex", gap: "4px", flexWrap: "wrap", maxHeight: "100px", overflowY: "auto", border: "1px solid var(--line)", padding: "8px", borderRadius: "8px", background: "#fcfcfc" }}>
                    {(claims.permissions as string[]).map((perm) => (
                      <span key={perm} className="pill" style={{ textTransform: "none", fontSize: "10px", padding: "2px 6px", background: "var(--violet-soft)", color: "var(--brand-ink)", borderColor: "#e6ddff" }}>
                        {perm}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              <button 
                className="btn btn-outline" 
                style={{ fontSize: "12px", padding: "8px 12px", width: "100%", display: "flex", justifyContent: "center", gap: "6px" }} 
                onClick={() => setShowSession((v) => !v)}
              >
                🖥️ {showSession ? "Masquer JWT" : "Inspecter le JWT"}
              </button>

              {showSession && (
                <div style={{ marginTop: "12px", overflow: "hidden", borderRadius: "10px" }}>
                  <pre className="jwt" style={{ fontSize: "10.5px", maxHeight: "160px", overflowY: "auto", margin: 0 }}>
                    {JSON.stringify(claims, null, 2)}
                  </pre>
                </div>
              )}
            </div>

          </div>
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
              <div style={{ fontSize: "40px", marginBottom: "10px", width: "64px", height: "64px", margin: "0 auto 10px", borderRadius: "14px", overflow: "hidden", display: "grid", placeItems: "center" }}>
                <PlatformIcon icon={ssoPlatform.icon} name={ssoPlatform.name} />
              </div>
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
