import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Yowyob — Portail unique",
  description: "Un seul compte pour toutes vos plateformes Yowyob — portail d'identité (POC).",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr">
      <body>{children}</body>
    </html>
  );
}
