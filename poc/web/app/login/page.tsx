"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { login, signUpAndVerify } from "../lib/yowauth";
import { saveSession } from "../lib/session";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [principal, setPrincipal] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (typeof window !== "undefined" && new URLSearchParams(window.location.search).get("mode") === "signup") {
      setMode("signup");
    }
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (mode === "signup") {
        await signUpAndVerify(principal.trim(), email.trim(), password);
      }
      const s = await login(principal.trim(), password);
      saveSession(s);
      router.push("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setBusy(false);
    }
  }

  return (
    <main className="authwrap">
      <div className="authcard">
        <Link href="/" className="logo" style={{ textDecoration: "none", color: "inherit" }}>
          <span className="logo__mark">Y</span>
          <span className="logo__name">Yowyob<small>Compte unique</small></span>
        </Link>

        <h2>{mode === "login" ? "Connexion" : "Créer un compte"}</h2>
        <p className="sub">{mode === "login" ? "Accédez à toutes vos plateformes Yowyob." : "Un compte, tout l'écosystème."}</p>

        <div className="tabs" role="tablist">
          <button type="button" className={`tab ${mode === "login" ? "tab--on" : ""}`} onClick={() => { setMode("login"); setError(""); }}>Connexion</button>
          <button type="button" className={`tab ${mode === "signup" ? "tab--on" : ""}`} onClick={() => { setMode("signup"); setError(""); }}>Inscription</button>
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
          <button type="submit" className="btn btn-submit" disabled={busy}>
            {busy ? "Veuillez patienter…" : mode === "login" ? "Se connecter" : "Créer le compte"}
          </button>
        </form>

        {error && <p className="alert alert--err">{error}</p>}

        <p className="foot"><Link href="/">← Retour à l&apos;accueil</Link></p>
      </div>
    </main>
  );
}
