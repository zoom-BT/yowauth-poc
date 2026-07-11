import Link from "next/link";
import type { Metadata } from "next";
import SiteFooter from "../../components/SiteFooter";

export const metadata: Metadata = {
  title: "Yowyob — Conditions générales d'utilisation",
  description: "Conditions générales d'utilisation du portail d'identité Yowyob (YowAuth, service du core KSM).",
};

const SECTIONS: { title: string; content: string }[] = [
  {
    title: "1. Objet",
    content:
      "Les présentes Conditions Générales d'Utilisation (CGU) régissent l'accès et l'utilisation du portail d'identité Yowyob et des services de l'écosystème opérés sur le core KSM, édités par KSM Kernel Solutions.",
  },
  {
    title: "2. Accès au service",
    content:
      "L'accès au portail est conditionné à la création d'un compte utilisateur et à l'acceptation des présentes CGU. L'utilisateur s'engage à fournir des informations exactes lors de son inscription et à maintenir la confidentialité de ses identifiants.",
  },
  {
    title: "3. Description du service",
    content:
      "Yowyob fournit une identité unique (authentification centralisée SSO / OIDC, jetons signés RS256) donnant accès à un écosystème de services métier : gestion de la relation client, ventes, stocks, comptabilité, trésorerie, caisse, ressources humaines et gestion documentaire.",
  },
  {
    title: "4. Obligations de l'utilisateur",
    content:
      "L'utilisateur s'engage à utiliser le service conformément à sa destination, à respecter les lois en vigueur, à ne pas tenter de contourner les mesures de sécurité, et à ne pas l'utiliser à des fins frauduleuses ou illicites.",
  },
  {
    title: "5. Données et confidentialité",
    content:
      "Les données saisies par l'utilisateur lui appartiennent. Yowyob s'engage à ne pas les céder à des tiers sans consentement explicite. Les détails du traitement des données personnelles sont précisés dans la Politique de confidentialité.",
  },
  {
    title: "6. Disponibilité et maintenance",
    content:
      "Yowyob s'efforce de maintenir le service disponible 99,5 % du temps. Des maintenances planifiées peuvent entraîner des interruptions temporaires, annoncées avec un préavis de 48 heures minimum.",
  },
  {
    title: "7. Tarification et facturation",
    content:
      "L'accès à certains services peut être soumis à un abonnement. Les conditions tarifaires applicables sont communiquées lors de la souscription. Toute modification tarifaire fait l'objet d'un préavis de 30 jours ; en cas de désaccord, l'utilisateur peut résilier son abonnement.",
  },
  {
    title: "8. Résiliation",
    content:
      "L'utilisateur peut résilier son compte à tout moment depuis ses paramètres. En cas de résiliation, les données sont conservées 30 jours puis supprimées définitivement, sauf obligation légale contraire.",
  },
  {
    title: "9. Limitation de responsabilité",
    content:
      "KSM Kernel Solutions ne peut être tenu responsable des pertes de données liées à des erreurs de saisie de l'utilisateur, ni des dommages indirects ou manques à gagner résultant de l'utilisation du service.",
  },
  {
    title: "10. Droit applicable",
    content:
      "Les présentes CGU sont régies par le droit camerounais. Tout litige sera soumis aux tribunaux compétents de Douala, Cameroun.",
  },
];

export default function ConditionsUtilisationPage() {
  return (
    <>
      <main className="doc">
        <header className="doc__top">
          <div className="doc__top-inner">
            <nav className="doc__nav">
              <Link href="/">← Accueil</Link>
              <Link href="/docs">Guide API</Link>
            </nav>
            <h1 className="doc__title">Conditions générales d&apos;utilisation</h1>
            <p className="doc__lead">
              Portail d&apos;identité Yowyob — YowAuth, service du core KSM.
            </p>
          </div>
        </header>

        <div className="doc__wrap">
          <div className="callout">
            Document de démonstration (POC) — à faire valider juridiquement avant toute
            mise en production.
          </div>

          {SECTIONS.map((s) => (
            <section key={s.title}>
              <h2>{s.title}</h2>
              <p>{s.content}</p>
            </section>
          ))}

          <p style={{ marginTop: 28, color: "var(--muted)", fontSize: 13 }}>
            Dernière mise à jour : {new Date().getFullYear()}.
          </p>
        </div>
      </main>
      <SiteFooter />
    </>
  );
}
