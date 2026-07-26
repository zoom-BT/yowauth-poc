"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  login,
  confirmLoginMfa,
  identify,
  signUpAndVerify,
  forgotPassword,
  issuePasswordReset,
  resetPassword,
  getCaptchaChallenge,
  verifyCaptchaChallenge,
  toUserMessage,
  type PasswordResetContextResponse
} from "../lib/yowauth";
import { saveSession } from "../lib/session";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "signup" | "forgot">("login");
  const [loginSubStep, setLoginSubStep] = useState<"identify" | "password" | "mfa">("identify");
  const [principal, setPrincipal] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // MFA State
  const [mfaToken, setMfaToken] = useState("");
  const [mfaCode, setMfaCode] = useState("");
  const [mfaChannel, setMfaChannel] = useState("");
  const [mfaCodePreview, setMfaCodePreview] = useState<string | null>(null);

  // Captcha State
  const [captchaChallenge, setCaptchaChallenge] = useState<{ captchaToken: string; prompt: string; answerPreview: string } | null>(null);
  const [captchaAnswer, setCaptchaAnswer] = useState("");
  const [loadingCaptcha, setLoadingCaptcha] = useState(false);

  // Password Recovery State
  const [forgotStep, setForgotStep] = useState<1 | 2 | 3>(1);
  const [recoveryPrincipal, setRecoveryPrincipal] = useState("");
  const [resetContexts, setResetContexts] = useState<PasswordResetContextResponse[]>([]);
  const [selectionToken, setSelectionToken] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");

  useEffect(() => {
    if (typeof window !== "undefined" && new URLSearchParams(window.location.search).get("mode") === "signup") {
      setMode("signup");
    }
  }, []);

  async function loadCaptcha() {
    setLoadingCaptcha(true);
    try {
      const challenge = await getCaptchaChallenge();
      setCaptchaChallenge(challenge);
      setCaptchaAnswer("");
    } catch (err) {
      console.error("Failed to load captcha challenge:", err);
    } finally {
      setLoadingCaptcha(false);
    }
  }

  useEffect(() => {
    if (mode === "signup") {
      loadCaptcha();
    }
  }, [mode]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setSuccessMessage("");
    try {
      let captchaVerificationToken: string | undefined;

      if (mode === "signup") {
        if (!captchaChallenge) {
          throw new Error("Le Captcha n'a pas été chargé correctement.");
        }
        // Étape 1 : Valider le Captcha auprès du serveur pour obtenir le captchaVerificationToken
        try {
          const verifyRes = await verifyCaptchaChallenge(captchaChallenge.captchaToken, captchaAnswer.trim());
          captchaVerificationToken = verifyRes.captchaVerificationToken;
        } catch (err) {
          throw new Error("Réponse de Captcha invalide. Veuillez réessayer.");
        }

        // Étape 2 : Inscription avec le token de vérification
        await signUpAndVerify(principal.trim(), email.trim(), password, captchaVerificationToken);
        setSuccessMessage("Votre compte a été créé et vérifié avec succès !");
        setMode("login");
        setLoginSubStep("identify");
        setPrincipal(principal.trim());
        setBusy(false);
        return;
      }

      // Connexion : Étape 1 - Identification
      if (loginSubStep === "identify") {
        const idRes = await identify(principal.trim());
        if (!idRes.accountExists) {
          throw new Error("Identifiant inconnu. Veuillez vérifier ou vous inscrire.");
        }
        setLoginSubStep("password");
        setBusy(false);
        return;
      }

      // Connexion : Étape 2 - Saisie du mot de passe
      if (loginSubStep === "password") {
        const res = await login(principal.trim(), password);
        if ("mfaRequired" in res) {
          setMfaToken(res.mfaToken);
          setMfaChannel(res.channel);
          setMfaCodePreview(res.codePreview);
          setLoginSubStep("mfa");
          setBusy(false);
          return;
        }
        saveSession(res);
        router.push("/");
        return;
      }

      // Connexion : Étape 3 - Confirmation MFA
      if (loginSubStep === "mfa") {
        const s = await confirmLoginMfa(mfaToken, mfaCode.trim());
        saveSession(s);
        router.push("/");
        return;
      }

    } catch (err) {
      setError(toUserMessage(err));
      setBusy(false);
      if (mode === "signup") {
        loadCaptcha();
      }
    }
  }

  // Password Recovery Logic
  async function handleForgotPrincipal(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const res = await forgotPassword(recoveryPrincipal.trim());
      if (res.matchingAccountCount === 0 || !res.contexts || res.contexts.length === 0) {
        throw new Error("Aucun compte correspondant trouvé.");
      }
      setSelectionToken(res.selectionToken);
      setResetContexts(res.contexts);

      if (res.contexts.length === 1) {
        // Auto-sélectionner le seul contexte disponible
        await triggerPasswordResetIssue(res.selectionToken, res.contexts[0].contextId);
      } else {
        setForgotStep(2);
        setBusy(false);
      }
    } catch (err) {
      setError(toUserMessage(err));
      setBusy(false);
    }
  }

  async function triggerPasswordResetIssue(selToken: string, ctxId: string) {
    try {
      const issued = await issuePasswordReset(selToken, ctxId);
      setResetToken(issued.challengeTokenPreview);
      setForgotStep(3);
      setBusy(false);
    } catch (err) {
      setError(toUserMessage(err));
      setBusy(false);
    }
  }

  async function handleSelectContext(ctxId: string) {
    setBusy(true);
    setError("");
    await triggerPasswordResetIssue(selectionToken, ctxId);
  }

  async function handleResetPassword(e: React.FormEvent) {
    e.preventDefault();
    if (newPassword !== confirmNewPassword) {
      setError("Les mots de passe ne correspondent pas.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await resetPassword(resetToken, newPassword);
      setSuccessMessage("Votre mot de passe a été réinitialisé avec succès ! Connectez-vous.");
      setMode("login");
      setPrincipal(recoveryPrincipal); // Pré-remplir l'identifiant pour faciliter la reconnexion
      setForgotStep(1);
      setRecoveryPrincipal("");
      setNewPassword("");
      setConfirmNewPassword("");
      setBusy(false);
    } catch (err) {
      setError(toUserMessage(err));
      setBusy(false);
    }
  }

  return (
    <main className="authwrap">
      {mode === "forgot" ? (
        <div className="authcard">
          <Link href="/" className="logo" style={{ textDecoration: "none", color: "inherit" }}>
            <span className="logo__mark">Y</span>
            <span className="logo__name">Yowyob<small>One ID</small></span>
          </Link>

          <h2>Mot de passe oublié</h2>
          <p className="sub">Suivez les étapes pour réinitialiser votre mot de passe.</p>

          {forgotStep === 1 && (
            <form onSubmit={handleForgotPrincipal}>
              <div className="field">
                <label htmlFor="recoveryPrincipal">Identifiant ou email</label>
                <input
                  id="recoveryPrincipal"
                  className="input"
                  value={recoveryPrincipal}
                  onChange={(e) => setRecoveryPrincipal(e.target.value)}
                  placeholder="ex : alice ou alice@exemple.io"
                  required
                  disabled={busy}
                />
              </div>
              <button type="submit" className="btn btn-submit" disabled={busy}>
                {busy ? "Recherche en cours..." : "Rechercher mon compte"}
              </button>
            </form>
          )}

          {forgotStep === 2 && (
            <div>
              <p style={{ fontSize: "14px", color: "var(--ink-soft)", marginBottom: "14px", lineHeight: "1.5" }}>
                Plusieurs comptes correspondent à cet identifiant. Veuillez sélectionner l&apos;organisation concernée :
              </p>
              <div style={{ display: "flex", flexDirection: "column", gap: "10px", margin: "16px 0" }}>
                {resetContexts.map((ctx) => (
                  <button
                    key={ctx.contextId}
                    type="button"
                    onClick={() => handleSelectContext(ctx.contextId)}
                    className="btn btn-outline"
                    style={{ justifyContent: "space-between", textAlign: "left", width: "100%", padding: "12px 16px", display: "flex", alignItems: "center" }}
                    disabled={busy}
                  >
                    <div>
                      <strong style={{ display: "block", color: "var(--ink)", fontSize: "14px" }}>{ctx.username}</strong>
                      <small style={{ color: "var(--muted)", fontSize: "11px" }}>{ctx.email}</small>
                    </div>
                    <span style={{ fontSize: "18px" }}>🏢</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {forgotStep === 3 && (
            <form onSubmit={handleResetPassword}>
              {resetToken && (
                <div className="alert alert--ok" style={{ marginBottom: "16px", fontSize: "12.5px", lineHeight: "1.4" }}>
                  <strong>Mode Démo :</strong> Jeton de récupération généré automatiquement : <code style={{ fontFamily: "var(--mono)", background: "#d1fae5", padding: "2px 4px", borderRadius: "4px" }}>{resetToken}</code>
                </div>
              )}
              <div className="field">
                <label htmlFor="newPassword">Nouveau mot de passe</label>
                <input
                  id="newPassword"
                  className="input"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  disabled={busy}
                />
              </div>
              <div className="field">
                <label htmlFor="confirmNewPassword">Confirmer le nouveau mot de passe</label>
                <input
                  id="confirmNewPassword"
                  className="input"
                  type="password"
                  value={confirmNewPassword}
                  onChange={(e) => setConfirmNewPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  disabled={busy}
                />
              </div>
              <button type="submit" className="btn btn-submit" disabled={busy}>
                {busy ? "Réinitialisation..." : "Enregistrer le mot de passe"}
              </button>
            </form>
          )}

          {error && <p className="alert alert--err" style={{ marginTop: "16px" }}>{error}</p>}

          <p className="foot" style={{ marginTop: "24px" }}>
            <button
              type="button"
              onClick={() => { setMode("login"); setError(""); setSuccessMessage(""); setForgotStep(1); }}
              style={{ background: "none", border: "none", color: "var(--muted)", cursor: "pointer", fontSize: "13.5px", textDecoration: "underline" }}
              disabled={busy}
            >
              ← Retour à la connexion
            </button>
          </p>
        </div>
      ) : (
        <div className="authcard">
          <Link href="/" className="logo" style={{ textDecoration: "none", color: "inherit" }}>
            <span className="logo__mark">Y</span>
            <span className="logo__name">Yowyob<small>One ID</small></span>
          </Link>

          <h2>{mode === "login" ? "Connexion" : "Créer un compte"}</h2>
          <p className="sub">{mode === "login" ? "Accédez à toutes vos plateformes Yowyob." : "Un compte, tout l&apos;écosystème."}</p>

          {successMessage && <p className="alert alert--ok" style={{ marginBottom: "20px" }}>{successMessage}</p>}

          <div className="tabs" role="tablist">
            <button type="button" className={`tab ${mode === "login" ? "tab--on" : ""}`} onClick={() => { setMode("login"); setLoginSubStep("identify"); setError(""); setSuccessMessage(""); }}>Connexion</button>
            <button type="button" className={`tab ${mode === "signup" ? "tab--on" : ""}`} onClick={() => { setMode("signup"); setLoginSubStep("identify"); setError(""); setSuccessMessage(""); }}>Inscription</button>
          </div>

          <form onSubmit={onSubmit}>
            {mode === "login" && loginSubStep === "identify" && (
              <div className="field">
                <label htmlFor="principal">Identifiant ou email</label>
                <input id="principal" className="input" value={principal} onChange={(e) => setPrincipal(e.target.value)} placeholder="ex : alice" autoComplete="username" required disabled={busy} />
              </div>
            )}

            {mode === "login" && loginSubStep === "password" && (
              <>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px", background: "var(--violet-soft)", padding: "10px 14px", borderRadius: "10px", fontSize: "14px" }}>
                  <span style={{ color: "var(--brand-ink)" }}>Utilisateur : <strong>{principal}</strong></span>
                  <button type="button" style={{ background: "none", border: "none", color: "var(--indigo)", cursor: "pointer", fontWeight: 600, fontSize: "12.5px" }} onClick={() => setLoginSubStep("identify")}>Modifier</button>
                </div>
                <div className="field">
                  <label htmlFor="password">Mot de passe</label>
                  <input id="password" className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" autoComplete="current-password" required disabled={busy} />
                </div>
                <div style={{ textAlign: "right", marginTop: "-6px", marginBottom: "16px" }}>
                  <button
                    type="button"
                    onClick={() => { setMode("forgot"); setForgotStep(1); setError(""); setSuccessMessage(""); }}
                    style={{ background: "none", border: "none", color: "var(--indigo)", cursor: "pointer", fontSize: "13px", padding: 0 }}
                    disabled={busy}
                  >
                    Mot de passe oublié ?
                  </button>
                </div>
              </>
            )}

            {mode === "login" && loginSubStep === "mfa" && (
              <>
                <div className="alert alert--ok" style={{ marginBottom: "16px", fontSize: "13px", lineHeight: "1.4" }}>
                  🔐 <strong>Double Authentification (MFA) :</strong> Un code temporaire a été envoyé sur votre canal <strong>{mfaChannel}</strong>.
                </div>
                {mfaCodePreview && (
                  <div className="alert alert--ok" style={{ marginBottom: "16px", fontSize: "12.5px" }}>
                    <strong>Mode Démo :</strong> Code MFA reçu : <code style={{ fontFamily: "var(--mono)", background: "#d1fae5", padding: "2px 4px", borderRadius: "4px" }}>{mfaCodePreview}</code>
                  </div>
                )}
                <div className="field">
                  <label htmlFor="mfaCode">Code de vérification (OTP)</label>
                  <input id="mfaCode" className="input" type="text" value={mfaCode} onChange={(e) => setMfaCode(e.target.value)} placeholder="ex : 123456" required disabled={busy} />
                </div>
              </>
            )}

            {mode === "signup" && (
              <>
                <div className="field">
                  <label htmlFor="principal">Nom d&apos;utilisateur</label>
                  <input id="principal" className="input" value={principal} onChange={(e) => setPrincipal(e.target.value)} placeholder="ex : alice" autoComplete="username" required disabled={busy} />
                </div>
                <div className="field">
                  <label htmlFor="email">Email</label>
                  <input id="email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="alice@exemple.io" autoComplete="email" required disabled={busy} />
                </div>
                <div className="field">
                  <label htmlFor="password">Mot de passe</label>
                  <input id="password" className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" autoComplete="new-password" required disabled={busy} />
                </div>
              </>
            )}

            {mode === "signup" && captchaChallenge && (
              <div className="field" style={{ borderTop: "1px solid var(--line)", paddingTop: "14px", marginTop: "14px" }}>
                <label htmlFor="captcha">Sécurité (Anti-robot)</label>
                <div style={{ display: "flex", gap: "10px", alignItems: "center", marginBottom: "8px" }}>
                  <span style={{
                    background: "var(--violet-soft)",
                    border: "1px solid #e6ddff",
                    padding: "10px 14px",
                    borderRadius: "10px",
                    fontFamily: "var(--mono)",
                    fontWeight: 700,
                    color: "var(--brand-ink)",
                    fontSize: "15px",
                    letterSpacing: "0.05em",
                    userSelect: "none"
                  }}>
                    {captchaChallenge.prompt}
                  </span>
                  <button
                    type="button"
                    onClick={loadCaptcha}
                    className="btn btn-outline"
                    style={{ padding: "10px 12px", minWidth: "auto", display: "inline-flex", justifyContent: "center" }}
                    title="Générer un autre captcha"
                    disabled={loadingCaptcha || busy}
                  >
                    🔄
                  </button>
                </div>
                <input
                  id="captcha"
                  className="input"
                  type="text"
                  value={captchaAnswer}
                  onChange={(e) => setCaptchaAnswer(e.target.value)}
                  placeholder={`Résultat (Démo : ${captchaChallenge.answerPreview})`}
                  required
                  disabled={busy}
                />
              </div>
            )}

            <button type="submit" className="btn btn-submit" disabled={busy}>
              {busy ? "Veuillez patienter…" : 
               mode === "login" ? 
                 (loginSubStep === "identify" ? "Continuer" : 
                  loginSubStep === "password" ? "Se connecter" : "Confirmer la connexion") : 
               "Créer le compte"}
            </button>

            {mode === "login" && loginSubStep === "mfa" && (
              <div style={{ textAlign: "center", marginTop: "14px" }}>
                <button
                  type="button"
                  onClick={() => { setLoginSubStep("password"); setError(""); setMfaCode(""); }}
                  style={{ background: "none", border: "none", color: "var(--muted)", cursor: "pointer", fontSize: "13.5px", textDecoration: "underline" }}
                  disabled={busy}
                >
                  ← Retour au mot de passe
                </button>
              </div>
            )}
          </form>

          {error && <p className="alert alert--err" style={{ marginTop: "16px" }}>{error}</p>}

          <p className="foot"><Link href="/">← Retour à l&apos;accueil</Link></p>
        </div>
      )}
    </main>
  );
}
