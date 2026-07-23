import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Serveur Node autonome (.next/standalone/server.js) : permet un déploiement
  // hors Vercel (Docker) avec un rendu strictement identique. Indispensable car
  // l'app n'est PAS statique — le BFF proxy (app/api/proxy) tourne côté serveur.
  output: "standalone",
};

export default nextConfig;
