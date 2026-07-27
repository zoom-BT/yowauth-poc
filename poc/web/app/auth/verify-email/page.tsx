"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { confirmEmailVerification, toUserMessage } from "../../lib/yowauth";

/**
 * Page atterrissage du lien de vérification d'email (…/auth/verify-email?token=…).
 * Le kernel doit pointer la base d'URL des emails vers CE chemin sur le front.
 * On lit le token et on confirme via POST /api/auth/email-verification/confirm.
 */
function VerifyEmailInner() {
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const [state, setState] = useState<"loading" | "ok" | "error" | "notoken">(
    token ? "loading" : "notoken"
  );
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    confirmEmailVerification(token)
      .then(() => { if (!cancelled) setState("ok"); })
      .catch((err) => { if (!cancelled) { setError(toUserMessage(err)); setState("error"); } });
    return () => { cancelled = true; };
  }, [token]);

  return (
    <main className="authwrap">
      <div className="authcard">
        <Link href="/" className="logo" style={{ textDecoration: "none", color: "inherit" }}>
          <span className="logo__mark">Y</span>
          <span className="logo__name">Yowyob<small>One ID</small></span>
        </Link>

        <h2>Vérification de l&apos;email</h2>

        {state === "loading" && <p className="sub">Validation de votre email en cours…</p>}

        {state === "ok" && (
          <p className="alert alert--ok" style={{ marginTop: "8px" }}>
            Votre email est vérifié. Votre compte Yowyob est actif — vous pouvez vous connecter.
          </p>
        )}

        {state === "notoken" && (
          <p className="alert alert--err" style={{ marginTop: "8px" }}>
            Lien de vérification invalide ou incomplet. Relancez une inscription si besoin.
          </p>
        )}

        {state === "error" && (
          <p className="alert alert--err" style={{ marginTop: "8px" }}>
            {error || "La vérification a échoué. Le lien a peut-être expiré."}
          </p>
        )}

        <p className="foot" style={{ marginTop: "20px" }}>
          <Link href="/login">← Aller à la connexion</Link>
        </p>
      </div>
    </main>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<main className="authwrap"><div className="authcard"><p className="sub">Chargement…</p></div></main>}>
      <VerifyEmailInner />
    </Suspense>
  );
}
