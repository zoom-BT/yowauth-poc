// Catalogue des plateformes Yowyob (mappe un code service -> une plateforme).
// URLs = placeholders yowyob.com, à remplacer par les vraies quand elles existent.

export type Platform = {
  code: string;
  name: string;
  tagline: string;
  icon: string;
  url: string;
  hue: string; // couleur d'accent de la carte
};

export const PLATFORMS: Platform[] = [
  { code: "COMMERCIAL", name: "Tiers & CRM", tagline: "Clients, fournisseurs, prospects", icon: "🤝", url: "https://crm.yowyob.com", hue: "#7c3aed" },
  { code: "SALES", name: "Ventes", tagline: "Devis, commandes, facturation", icon: "🛒", url: "https://sales.yowyob.com", hue: "#f97316" },
  { code: "INVENTORY", name: "Stock", tagline: "Entrepôts & mouvements", icon: "📦", url: "https://inventory.yowyob.com", hue: "#0ea5e9" },
  { code: "PRODUCT", name: "Catalogue", tagline: "Produits, variantes, prix", icon: "🏷️", url: "https://catalog.yowyob.com", hue: "#16a34a" },
  { code: "HRM", name: "Ressources humaines", tagline: "Employés, contrats, congés", icon: "👥", url: "https://hr.yowyob.com", hue: "#db2777" },
  { code: "PAYROLL", name: "Paie", tagline: "Bulletins & cotisations", icon: "💶", url: "https://payroll.yowyob.com", hue: "#ca8a04" },
  { code: "ACCOUNTING", name: "Comptabilité", tagline: "Écritures & grand livre", icon: "📊", url: "https://accounting.yowyob.com", hue: "#4f46e5" },
  { code: "TREASURY", name: "Trésorerie", tagline: "Encaissements & règlements", icon: "🏦", url: "https://treasury.yowyob.com", hue: "#0891b2" },
  { code: "BILLING", name: "Facturation", tagline: "Factures & abonnements", icon: "🧾", url: "https://billing.yowyob.com", hue: "#e11d48" },
  { code: "CASHIER", name: "Caisse", tagline: "Point de vente", icon: "💳", url: "https://cashier.yowyob.com", hue: "#ea580c" },
  { code: "BANKING", name: "Banque", tagline: "Comptes & virements", icon: "🏛️", url: "https://banking.yowyob.com", hue: "#2563eb" },
  { code: "RESOURCE", name: "Ressources", tagline: "Matériel & affectations", icon: "🔧", url: "https://resources.yowyob.com", hue: "#65a30d" },
  { code: "ORGANIZATION", name: "Organisations", tagline: "Structures & agences", icon: "🏢", url: "https://org.yowyob.com", hue: "#7c3aed" },
  { code: "SETTINGS", name: "Paramètres", tagline: "Configuration plateforme", icon: "⚙️", url: "https://settings.yowyob.com", hue: "#64748b" },
  { code: "NOTIFICATION", name: "Notifications", tagline: "Email, SMS, push", icon: "🔔", url: "https://notify.yowyob.com", hue: "#f59e0b" },
  { code: "KYC", name: "Vérification (KYC)", tagline: "Identité & conformité", icon: "🪪", url: "https://kyc.yowyob.com", hue: "#0d9488" },
  { code: "BLOCKCHAIN", name: "Blockchain", tagline: "Registre distribué", icon: "⛓️", url: "https://chain.yowyob.com", hue: "#334155" },
  { code: "YOWPAINTER", name: "YowPainter", tagline: "Design & création", icon: "🎨", url: "https://painter.yowyob.com", hue: "#c026d3" },
];

const BY_CODE = new Map(PLATFORMS.map((p) => [p.code.toUpperCase(), p]));

export function platformFor(code: string): Platform | undefined {
  return BY_CODE.get(code.toUpperCase());
}

/** Plateformes accessibles = union des services des organisations, dédupliquées, mappées au catalogue. */
export function accessiblePlatforms(services: string[]): Platform[] {
  const seen = new Set<string>();
  const out: Platform[] = [];
  for (const s of services) {
    const p = platformFor(s);
    if (p && !seen.has(p.code)) {
      seen.add(p.code);
      out.push(p);
    }
  }
  return out;
}

/** Les autres plateformes du catalogue (celles non accessibles) — pour la section « Découvrir ». */
export function otherPlatforms(accessibleCodes: string[]): Platform[] {
  const have = new Set(accessibleCodes.map((c) => c.toUpperCase()));
  return PLATFORMS.filter((p) => !have.has(p.code));
}
