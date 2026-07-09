"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { decodeJwt, type Session } from "./lib/yowauth";
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

/* ---------------- Launcher (connecté) ---------------- */
function Launcher({ session }: { session: Session }) {
  const orgs = session.organizations;
  const [orgIdx, setOrgIdx] = useState(0);
  const org = orgs[orgIdx];
  const mine = useMemo(() => accessiblePlatforms(org?.services ?? []), [org]);
  const discover = useMemo(() => otherPlatforms(org?.services ?? []), [org]);
  const [showSession, setShowSession] = useState(false);
  const claims = decodeJwt(session.accessToken);
  const p = session.profile;
  const initial = (p.username || "?").charAt(0).toUpperCase();

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
          <p>Voici les plateformes auxquelles vous avez accès.</p>
        </div>

        {orgs.length > 1 && (
          <div className="orgbar">
            <span style={{ color: "var(--muted)", fontSize: 13 }}>Organisation :</span>
            {orgs.map((o, i) => (
              <button key={o.organizationId} className={`orgtab ${i === orgIdx ? "orgtab--on" : ""}`} onClick={() => setOrgIdx(i)}>{o.shortName}</button>
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
              <a key={pl.code} className="tile" href={pl.url} target="_blank" rel="noreferrer" style={{ ["--hue" as string]: pl.hue }}>
                <div className="tile__ic">{pl.icon}</div>
                <div className="tile__nm">{pl.name}</div>
                <div className="tile__tg">{pl.tagline}</div>
                <div className="tile__go">Accéder →</div>
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

        <div className="sess">
          <h3 style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            Ma session
            <button className="btn btn-ghost" style={{ fontSize: 12, padding: "4px 10px" }} onClick={() => setShowSession((v) => !v)}>
              {showSession ? "masquer" : "détails du jeton"}
            </button>
          </h3>
          <dl className="kv">
            <dt>Email</dt><dd>{p.email}</dd>
            <dt>Statut</dt><dd>{p.status}</dd>
            <dt>Plan</dt><dd>{p.plan}</dd>
            <dt>Tenant</dt><dd>{p.tenantId}</dd>
          </dl>
          {showSession && <pre className="jwt">{JSON.stringify(claims, null, 2)}</pre>}
        </div>
      </section>

      <footer className="wrap footer">
        <span>© Yowyob — Portail d&apos;identité (POC).</span>
        <span>Connecté via YowAuth · JWT RS256</span>
      </footer>
    </>
  );
}
