"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { resetPassword, toUserMessage } from "../../lib/yowauth";

/**
 * Page atterrissage du lien de réinitialisation reçu par email
 * (…/auth/reset-password?token=…). Le kernel doit pointer la base d'URL des
 * emails vers CE chemin sur le domaine du front. On lit le token, on demande
 * le nouveau mot de passe, puis on appelle POST /api/auth/reset-password.
 */
function ResetPasswordInner() {
  const router = useRouter();
  const params = useSearchParams();
  const token = params.get("token") ?? "";

  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    if (newPassword !== confirm) {
      setError("Les deux mots de passe ne correspondent pas.");
      return;
    }
    setBusy(true);
    try {
      await resetPassword(token, newPassword);
      setDone(true);
      setTimeout(() => router.push("/login"), 2200);
    } catch (err) {
      setError(toUserMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="authwrap">
      <div className="authcard">
        <Link href="/" className="logo" style={{ textDecoration: "none", color: "inherit" }}>
          <span className="logo__mark">Y</span>
          <span className="logo__name">Yowyob<small>One ID</small></span>
        </Link>

        <h2>Nouveau mot de passe</h2>

        {!token ? (
          <>
            <p className="sub">Ce lien de réinitialisation est invalide ou incomplet.</p>
            <p className="alert alert--err" style={{ marginTop: "8px" }}>
              Jeton manquant. Merci de relancer une demande depuis « Mot de passe oublié ».
            </p>
            <p className="foot" style={{ marginTop: "20px" }}>
              <Link href="/login">← Retour à la connexion</Link>
            </p>
          </>
        ) : done ? (
          <>
            <p className="alert alert--ok" style={{ marginTop: "8px" }}>
              Votre mot de passe a été réinitialisé avec succès. Redirection vers la connexion…
            </p>
            <p className="foot" style={{ marginTop: "20px" }}>
              <Link href="/login">Se connecter maintenant</Link>
            </p>
          </>
        ) : (
          <>
            <p className="sub">Choisissez un nouveau mot de passe pour votre compte Yowyob.</p>
            <form onSubmit={onSubmit}>
              <div className="field">
                <label htmlFor="newPassword">Nouveau mot de passe</label>
                <input
                  id="newPassword"
                  className="input"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  autoComplete="new-password"
                  required
                  disabled={busy}
                />
              </div>
              <div className="field">
                <label htmlFor="confirm">Confirmer le mot de passe</label>
                <input
                  id="confirm"
                  className="input"
                  type="password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="••••••••"
                  autoComplete="new-password"
                  required
                  disabled={busy}
                />
              </div>
              <button type="submit" className="btn btn-submit" disabled={busy}>
                {busy ? "Enregistrement…" : "Enregistrer le mot de passe"}
              </button>
            </form>
            {error && <p className="alert alert--err" style={{ marginTop: "16px" }}>{error}</p>}
            <p className="foot" style={{ marginTop: "20px" }}>
              <Link href="/login">← Retour à la connexion</Link>
            </p>
          </>
        )}
      </div>
    </main>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<main className="authwrap"><div className="authcard"><p className="sub">Chargement…</p></div></main>}>
      <ResetPasswordInner />
    </Suspense>
  );
}
