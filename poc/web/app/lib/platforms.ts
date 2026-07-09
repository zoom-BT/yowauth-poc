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
  { code: "COMMERCIAL", name: "Tiers & CRM", tagline: "Clients, fournisseurs, prospects", icon: "https://images.unsplash.com/photo-1552581230-c01591d3c99a?w=150&auto=format&fit=crop&q=80", url: "https://crm.yowyob.com", hue: "#7c3aed" },
  { code: "SALES", name: "Ventes", tagline: "Devis, commandes, facturation", icon: "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=150&auto=format&fit=crop&q=80", url: "https://sales.yowyob.com", hue: "#f97316" },
  { code: "INVENTORY", name: "Stock", tagline: "Entrepôts & mouvements", icon: "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=150&auto=format&fit=crop&q=80", url: "https://inventory.yowyob.com", hue: "#0ea5e9" },
  { code: "PRODUCT", name: "Catalogue", tagline: "Produits, variantes, prix", icon: "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=150&auto=format&fit=crop&q=80", url: "https://catalog.yowyob.com", hue: "#16a34a" },
  { code: "HRM", name: "Ressources humaines", tagline: "Employés, contrats, congés", icon: "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=150&auto=format&fit=crop&q=80", url: "https://hr.yowyob.com", hue: "#db2777" },
  { code: "PAYROLL", name: "Paie", tagline: "Bulletins & cotisations", icon: "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=150&auto=format&fit=crop&q=80", url: "https://payroll.yowyob.com", hue: "#ca8a04" },
  { code: "ACCOUNTING", name: "Comptabilité", tagline: "Écritures & grand livre", icon: "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=150&auto=format&fit=crop&q=80", url: "https://accounting.yowyob.com", hue: "#4f46e5" },
  { code: "TREASURY", name: "Trésorerie", tagline: "Encaissements & règlements", icon: "https://images.unsplash.com/photo-1501167786227-4cba60f6d58f?w=150&auto=format&fit=crop&q=80", url: "https://treasury.yowyob.com", hue: "#0891b2" },
  { code: "BILLING", name: "Facturation", tagline: "Factures & abonnements", icon: "https://images.unsplash.com/photo-1450133064473-71024230f91b?w=150&auto=format&fit=crop&q=80", url: "https://billing.yowyob.com", hue: "#e11d48" },
  { code: "CASHIER", name: "Caisse", tagline: "Point de vente", icon: "https://images.unsplash.com/photo-1556740758-90de374c12ad?w=150&auto=format&fit=crop&q=80", url: "https://cashier.yowyob.com", hue: "#ea580c" },
  { code: "BANKING", name: "Banque", tagline: "Comptes & virements", icon: "https://images.unsplash.com/photo-1601597111158-2fceff292cdc?w=150&auto=format&fit=crop&q=80", url: "https://banking.yowyob.com", hue: "#2563eb" },
  { code: "RESOURCE", name: "Ressources", tagline: "Matériel & affectations", icon: "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=150&auto=format&fit=crop&q=80", url: "https://resources.yowyob.com", hue: "#65a30d" },
  { code: "ORGANIZATION", name: "Organisations", tagline: "Structures & agences", icon: "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=150&auto=format&fit=crop&q=80", url: "https://org.yowyob.com", hue: "#7c3aed" },
  { code: "SETTINGS", name: "Paramètres", tagline: "Configuration plateforme", icon: "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=150&auto=format&fit=crop&q=80", url: "https://settings.yowyob.com", hue: "#64748b" },
  { code: "NOTIFICATION", name: "Notifications", tagline: "Email, SMS, push", icon: "https://images.unsplash.com/photo-1557200134-90327ee9fafa?w=150&auto=format&fit=crop&q=80", url: "https://notify.yowyob.com", hue: "#f59e0b" },
  { code: "KYC", name: "Vérification (KYC)", tagline: "Identité & conformité", icon: "https://images.unsplash.com/photo-1557683316-973673baf926?w=150&auto=format&fit=crop&q=80", url: "https://kyc.yowyob.com", hue: "#0d9488" },
  { code: "BLOCKCHAIN", name: "Blockchain", tagline: "Registre distribué", icon: "https://images.unsplash.com/photo-1639762681485-074b7f938ba0?w=150&auto=format&fit=crop&q=80", url: "https://chain.yowyob.com", hue: "#334155" },
  { code: "YOWPAINTER", name: "YowPainter", tagline: "Design & création", icon: "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=150&auto=format&fit=crop&q=80", url: "https://painter.yowyob.com", hue: "#c026d3" },
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
