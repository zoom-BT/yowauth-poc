import Link from "next/link";

/**
 * Footer partagé du portail YowAuth.
 *
 * YowAuth est un service du core KSM dédié à l'authentification : le footer reste
 * sobre (pas de site marketing). Les liens ne pointent que vers des routes réelles
 * et existantes — aucun lien mort.
 */
export default function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="site-footer">
      <div className="wrap site-footer__in">
        {/* Bloc marque */}
        <div className="site-footer__brand">
          <Link href="/" className="logo" style={{ textDecoration: "none", color: "inherit" }}>
            <span className="logo__mark">Y</span>
            <span className="logo__name">
              Yowyob<small>Compte unique</small>
            </span>
          </Link>
          <p className="site-footer__tag">
            YowAuth — service d&apos;authentification du core KSM. Un seul compte,
            sécurisé et vérifiable, pour tout l&apos;écosystème Yowyob.
          </p>
          <div className="site-footer__badges">
            <span className="site-footer__badge">SSO</span>
            <span className="site-footer__badge">OIDC</span>
            <span className="site-footer__badge">JWT RS256</span>
          </div>
        </div>

        {/* Colonnes de liens — routes existantes uniquement */}
        <nav className="site-footer__col" aria-label="Compte">
          <h4 className="site-footer__title">Compte</h4>
          <ul>
            <li><Link href="/login">Se connecter</Link></li>
            <li><Link href="/login?mode=signup">Créer un compte</Link></li>
          </ul>
        </nav>

        <nav className="site-footer__col" aria-label="Ressources">
          <h4 className="site-footer__title">Ressources</h4>
          <ul>
            <li><Link href="/docs">Guide API</Link></li>
            <li><Link href="/demo">Démonstration</Link></li>
          </ul>
        </nav>

        <nav className="site-footer__col" aria-label="Légal">
          <h4 className="site-footer__title">Légal</h4>
          <ul>
            <li><Link href="/legal/conditions-utilisation">Conditions d&apos;utilisation</Link></li>
          </ul>
        </nav>
      </div>

      <div className="wrap site-footer__bottom">
        <span>© {year} Yowyob · Portail d&apos;identité (POC).</span>
        <span>Propulsé par YowAuth · JWT RS256 · OIDC</span>
      </div>
    </footer>
  );
}
