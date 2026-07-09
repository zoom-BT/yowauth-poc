"use client";

import { useEffect, useState } from "react";
import type { Session } from "./yowauth";

const KEY = "yowauth.session";

export function saveSession(s: Session | null) {
  if (typeof window === "undefined") return;
  if (s) localStorage.setItem(KEY, JSON.stringify(s));
  else localStorage.removeItem(KEY);
  window.dispatchEvent(new Event("yowauth:session"));
}

export function readSession(): Session | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

/** Hook réactif : renvoie [session, ready]. `ready` évite le flash au premier rendu SSR. */
export function useSession(): [Session | null, boolean] {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const sync = () => setSession(readSession());
    sync();
    setReady(true);
    window.addEventListener("yowauth:session", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("yowauth:session", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  return [session, ready];
}
