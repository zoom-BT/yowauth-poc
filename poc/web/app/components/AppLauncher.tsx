"use client";

import { useEffect, useRef, useState } from "react";
import { ECOSYSTEM, KSM_MODULES, type Platform } from "../lib/platforms";

function Icon({ p }: { p: Platform }) {
  if (p.icon.startsWith("http")) {
    return <img src={p.icon} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} />;
  }
  return <span style={{ fontSize: "20px" }}>{p.icon}</span>;
}

/** App-launcher « 9 points » à la Google : ouvre une grille des services de l'écosystème.
 *  `light` : points blancs, pour les en-têtes sombres (docs, démo). */
export default function AppLauncher({ light = false }: { light?: boolean }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const apps = [...ECOSYSTEM, ...KSM_MODULES];

  return (
    <div className={`applauncher${light ? " applauncher--light" : ""}`} ref={ref}>
      <button
        type="button"
        className="applauncher__btn"
        aria-label="Applications Yowyob"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="applauncher__dots">
          {Array.from({ length: 9 }).map((_, i) => <i key={i} />)}
        </span>
      </button>

      {open && (
        <div className="applauncher__pop" role="menu">
          <div className="applauncher__head">Écosystème Yowyob</div>
          <div className="applauncher__apps">
            {apps.map((p) => (
              <a
                key={p.code}
                href={p.url}
                target="_blank"
                rel="noreferrer"
                className="applauncher__app"
                role="menuitem"
                title={p.name}
              >
                <span className="applauncher__ic"><Icon p={p} /></span>
                <span className="applauncher__nm">{p.name}</span>
              </a>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
