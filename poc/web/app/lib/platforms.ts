// Catalogue Yowyob.
//  - ECOSYSTEM : les projets de l'écosystème Yowyob (vitrine de l'accueil), cliquables vers leurs vrais domaines.
//  - KSM_MODULES : les modules internes de l'ERP KSM (sous-domaines *.ksm.yowyob.com).
//  - PLATFORMS : mapping "code service kernel -> carte" utilisé par le tableau de bord connecté.

export type Platform = {
  code: string;
  name: string;
  tagline: string;
  icon: string; // URL http(s) OU emoji
  url: string;
  hue: string; // couleur d'accent de la carte
};

// ─── Écosystème Yowyob (vitrine accueil) ─────────────────────────────────────
export const ECOSYSTEM: Platform[] = [
  { code: "KSM", name: "RT-KSM", tagline: "ERP tout-en-un (gestion commerciale)", icon: "🏢", url: "https://ksm.yowyob.com", hue: "#4f46e5" },
  { code: "BUSINESS_BOOK", name: "Business Book", tagline: "Carnet & réseau d'affaires", icon: "📗", url: "https://business-book.yowyob.com", hue: "#16a34a" },
  { code: "BUSINESS_CORE", name: "Business Core", tagline: "Socle métier des entreprises", icon: "🧩", url: "https://business-core.yowyob.com", hue: "#0ea5e9" },
  { code: "CHAT", name: "Chat", tagline: "Messagerie multi-tenant (CaaS)", icon: "💬", url: "https://chat.yowyob.com", hue: "#2563eb" },
  { code: "SEARCH", name: "Search", tagline: "Recherche, reco & web crawler", icon: "🔎", url: "https://search.yowyob.com", hue: "#7c3aed" },
  { code: "TIIBNTICK", name: "TiiBnTick", tagline: "Billetterie & ticketing", icon: "🎫", url: "https://tiibntick.yowyob.com", hue: "#db2777" },
  { code: "LOYALTY", name: "Loyalty", tagline: "Programmes de fidélité", icon: "🎁", url: "https://loyalty.yowyob.com", hue: "#e11d48" },
  { code: "DRISMAN", name: "Drisman", tagline: "Logistique & distribution", icon: "📦", url: "https://drisman.yowyob.com", hue: "#ca8a04" },
  { code: "RENTAL", name: "Rental", tagline: "Location de biens & matériel", icon: "🔑", url: "https://rental.yowyob.com", hue: "#65a30d" },
  { code: "BUS_STATION", name: "Bus Station", tagline: "Gares routières & voyages", icon: "🚌", url: "https://bus-station.yowyob.com", hue: "#0891b2" },
  { code: "FLEETMAN", name: "Fleetman", tagline: "Gestion de flotte", icon: "🚛", url: "https://fleetman.yowyob.com", hue: "#334155" },
  { code: "FLIPGO", name: "FlipGo", tagline: "Chauffeurs freelance", icon: "🧑‍✈️", url: "https://flipgo.yowyob.com", hue: "#ea580c" },
  { code: "RIDNGO", name: "RidnGo", tagline: "VTC & mobilité", icon: "🚗", url: "https://ridngo.yowyob.com", hue: "#f97316" },
  { code: "PAYMENT", name: "Payment", tagline: "Paiement as a Service", icon: "💳", url: "https://payment.yowyob.com", hue: "#2563eb" },
  { code: "EVENTAAS", name: "EventaaS", tagline: "Événementiel as a Service", icon: "🎉", url: "https://eventaas.yowyob.com", hue: "#c026d3" },
  { code: "YOWPAINTER", name: "YowPainter", tagline: "Design & création", icon: "🎨", url: "https://painter.yowyob.com", hue: "#7c3aed" },
  { code: "KYC", name: "Smart KYC", tagline: "Vérification d'identité", icon: "🛡️", url: "https://kyc.yowyob.com", hue: "#0d9488" },
];

// ─── Modules internes de l'ERP KSM (sous-domaines *.ksm.yowyob.com) ──────────
export const KSM_MODULES: Platform[] = [
  { code: "KSM_CRM", name: "CRM & Tiers", tagline: "Clients, fournisseurs, prospects", icon: "👥", url: "https://crm.ksm.yowyob.com", hue: "#7c3aed" },
  { code: "KSM_HR", name: "RH & Paie", tagline: "Employés, contrats, bulletins", icon: "🧑‍💼", url: "https://hr.ksm.yowyob.com", hue: "#db2777" },
  { code: "KSM_ACCOUNTING", name: "Comptabilité", tagline: "Comptabilité OHADA & grand livre", icon: "📒", url: "https://accounting.ksm.yowyob.com", hue: "#4f46e5" },
  { code: "KSM_BANKING", name: "Banque & Trésorerie", tagline: "Comptes, virements, rapprochements", icon: "🏦", url: "https://banking.ksm.yowyob.com", hue: "#2563eb" },
  { code: "KSM_CASHIER", name: "Caisse", tagline: "Point de vente & encaissements", icon: "🧾", url: "https://cashier.ksm.yowyob.com", hue: "#ea580c" },
];

// ─── Mapping code service kernel -> carte (tableau de bord connecté) ──────────
export const PLATFORMS: Platform[] = [
  { code: "COMMERCIAL", name: "Tiers & CRM", tagline: "Clients, fournisseurs, prospects", icon: "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=200&auto=format&fit=crop&q=80", url: "https://crm.ksm.yowyob.com", hue: "#7c3aed" },
  { code: "SALES", name: "Ventes", tagline: "Devis, commandes, facturation", icon: "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=200&auto=format&fit=crop&q=80", url: "https://ksm.yowyob.com", hue: "#f97316" },
  { code: "INVENTORY", name: "Stock", tagline: "Entrepôts & mouvements", icon: "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=200&auto=format&fit=crop&q=80", url: "https://ksm.yowyob.com", hue: "#0ea5e9" },
  { code: "PRODUCT", name: "Catalogue", tagline: "Produits, variantes, prix", icon: "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=200&auto=format&fit=crop&q=80", url: "https://ksm.yowyob.com", hue: "#16a34a" },
  { code: "HRM", name: "Ressources humaines", tagline: "Employés, contrats, congés", icon: "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200&auto=format&fit=crop&q=80", url: "https://hr.ksm.yowyob.com", hue: "#db2777" },
  { code: "PAYROLL", name: "Paie", tagline: "Bulletins & cotisations", icon: "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=200&auto=format&fit=crop&q=80", url: "https://hr.ksm.yowyob.com", hue: "#ca8a04" },
  { code: "ACCOUNTING", name: "Comptabilité", tagline: "Écritures & grand livre", icon: "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=200&auto=format&fit=crop&q=80", url: "https://accounting.ksm.yowyob.com", hue: "#4f46e5" },
  { code: "BILLING", name: "Facturation", tagline: "Factures & abonnements", icon: "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=200&auto=format&fit=crop&q=80", url: "https://ksm.yowyob.com", hue: "#e11d48" },
  { code: "CASHIER", name: "Caisse", tagline: "Point de vente", icon: "https://images.unsplash.com/photo-1556740758-90de374c12ad?w=200&auto=format&fit=crop&q=80", url: "https://cashier.ksm.yowyob.com", hue: "#ea580c" },
  { code: "BANKING", name: "Banque", tagline: "Comptes & virements", icon: "https://images.unsplash.com/photo-1501167786227-4cba60f6d58f?w=200&auto=format&fit=crop&q=80", url: "https://banking.ksm.yowyob.com", hue: "#2563eb" },
  { code: "RESOURCE", name: "Ressources", tagline: "Matériel & affectations", icon: "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=200&auto=format&fit=crop&q=80", url: "https://ksm.yowyob.com", hue: "#65a30d" },
  { code: "ORGANIZATION", name: "Organisations", tagline: "Structures & agences", icon: "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=200&auto=format&fit=crop&q=80", url: "https://business-core.yowyob.com", hue: "#7c3aed" },
  { code: "KYC", name: "Vérification (KYC)", tagline: "Identité & conformité", icon: "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=200&auto=format&fit=crop&q=80", url: "https://kyc.yowyob.com", hue: "#0d9488" },
  { code: "YOWPAINTER", name: "YowPainter", tagline: "Design & création", icon: "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=200&auto=format&fit=crop&q=80", url: "https://painter.yowyob.com", hue: "#c026d3" },
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
