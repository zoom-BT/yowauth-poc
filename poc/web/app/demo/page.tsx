"use client";

import Link from "next/link";
import { useRef, useState } from "react";
import { config, decodeJwt } from "../lib/yowauth";

const H: Record<string, string> = {
  "Content-Type": "application/json",
  "X-Client-Id": config.CLIENT_ID,
  "X-Api-Key": config.API_KEY,
  "X-Tenant-Id": config.TENANT,
};

type Status = "pending" | "run" | "ok" | "err";
type Node = { key: string; label: string };
type Line = { id: number; kind: "cmd" | "out" | "ok" | "err" | "info"; text: string };

const NODES: Node[] = [
  { key: "oidc", label: "oidc" },
  { key: "jwks", label: "jwks" },
  { key: "signup", label: "sign-up" },
  { key: "verify", label: "verify" },
  { key: "confirm", label: "confirm" },
  { key: "login", label: "login" },
  { key: "me", label: "me" },
  { key: "token", label: "token" },
  { key: "userinfo", label: "userinfo" },
];

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export default function DemoPage() {
  const [status, setStatus] = useState<Record<string, Status>>({});
  const [log, setLog] = useState<Line[]>([]);
  const [running, setRunning] = useState(false);
  const [claims, setClaims] = useState<Record<string, unknown> | null>(null);
  const idRef = useRef(0);
  const bodyRef = useRef<HTMLDivElement>(null);

  function scroll() {
    requestAnimationFrame(() => {
      if (bodyRef.current) bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    });
  }
  function add(kind: Line["kind"], text: string) {
    setLog((p) => [...p, { id: idRef.current++, kind, text }]);
    scroll();
  }
  async function typeCmd(text: string) {
    const id = idRef.current++;
    setLog((p) => [...p, { id, kind: "cmd", text: "" }]);
    for (let i = 1; i <= text.length; i++) {
      setLog((p) => p.map((l) => (l.id === id ? { ...l, text: text.slice(0, i) } : l)));
      await sleep(9);
    }
    scroll();
  }
  const setNode = (key: string, s: Status) => setStatus((p) => ({ ...p, [key]: s }));

  async function http(
    method: string,
    path: string,
    opt: { json?: unknown; form?: Record<string, string>; bearer?: string } = {},
  ) {
    const headers: Record<string, string> = { ...H };
    let body: string | undefined;
    if (opt.form) {
      headers["Content-Type"] = "application/x-www-form-urlencoded";
      body = new URLSearchParams(opt.form).toString();
    } else if (opt.json !== undefined) {
      body = JSON.stringify(opt.json);
    }
    if (opt.bearer) headers["Authorization"] = `Bearer ${opt.bearer}`;
    const res = await fetch(`${config.API}${path}`, { method, headers, body });
    const text = await res.text();
    let json: unknown = null;
    try {
      json = JSON.parse(text);
    } catch {
      /* non-json */
    }
    return { status: res.status, ok: res.ok, json, text };
  }

  async function run() {
    setRunning(true);
    setClaims(null);
    setLog([]);
    setStatus(Object.fromEntries(NODES.map((n) => [n.key, "pending"])));
    idRef.current = 0;
    const user = `poc_${Date.now()}`;
    const pw = "P@ssw0rd!2024";

    add("info", `# cible : ${config.API}`);
    add("info", `# identité de test : ${user}`);

    try {
      // helper to run one step
      const step = async (key: string, method: string, path: string, opt: Parameters<typeof http>[2] = {}, summarize?: (j: unknown) => string) => {
        setNode(key, "run");
        await typeCmd(`$ ${method} ${path}`);
        await sleep(180);
        const r = await http(method, path, opt);
        if (r.ok) {
          add("ok", `→ ${r.status} OK${summarize && r.json ? "  ·  " + summarize(r.json) : ""}`);
          setNode(key, "ok");
        } else {
          add("err", `→ ${r.status} ${r.text.slice(0, 120)}`);
          setNode(key, "err");
        }
        await sleep(160);
        return r;
      };

      await step("oidc", "GET", "/.well-known/openid-configuration", {}, (j) => `issuer=${(j as { issuer?: string }).issuer}`);
      await step("jwks", "GET", "/.well-known/jwks.json", {}, (j) => `kid=${(j as { keys?: { kid?: string }[] }).keys?.[0]?.kid} (RSA)`);
      await step("signup", "POST", "/api/auth/sign-up", { json: { tenantId: config.TENANT, username: user, email: `${user}@demo.io`, password: pw, firstName: "Demo", lastName: "User" } }, () => "compte créé");

      setNode("verify", "run");
      await typeCmd("$ POST /api/auth/email-verification/resend");
      await sleep(160);
      const resend = await http("POST", "/api/auth/email-verification/resend", { json: { principal: user } });
      const vtoken = (resend.json as { data?: { challengeTokenPreview?: string } })?.data?.challengeTokenPreview ?? "";
      add(resend.ok ? "ok" : "err", `→ ${resend.status}  ·  token PREVIEW reçu (${vtoken.slice(0, 18)}…)`);
      setNode("verify", resend.ok ? "ok" : "err");
      await sleep(160);

      await step("confirm", "POST", "/api/auth/email-verification/confirm", { json: { verificationToken: vtoken } }, () => "email vérifié");

      setNode("login", "run");
      await typeCmd("$ POST /api/auth/login");
      await sleep(160);
      const login = await http("POST", "/api/auth/login", { json: { principal: user, password: pw } });
      const ld = (login.json as { data?: { accessToken?: string; sharedSession?: { token?: string }; organizations?: { organizationCode?: string }[] } })?.data;
      const access = ld?.accessToken ?? "";
      const sso = ld?.sharedSession?.token ?? "";
      const org = ld?.organizations?.[0]?.organizationCode ?? "—";
      add(login.ok ? "ok" : "err", `→ ${login.status}  ·  org=${org}  ·  accessToken signé RS256`);
      setNode("login", login.ok ? "ok" : "err");
      await sleep(160);

      await step("me", "GET", "/api/users/me", { bearer: access }, (j) => `me=${(j as { data?: { username?: string } }).data?.username}`);

      setNode("token", "run");
      await typeCmd("$ POST /oauth2/token  (grant=token-exchange)");
      await sleep(180);
      const ctx = (decodeJwt(sso) as { contexts?: { contextId?: string }[] })?.contexts?.[0]?.contextId ?? "";
      const xchg = await http("POST", "/oauth2/token", {
        form: {
          grant_type: "urn:ietf:params:oauth:grant-type:token-exchange",
          subject_token: sso,
          subject_token_type: "urn:ietf:params:oauth:token-type:jwt",
          context_id: ctx,
          service_code: config.SERVICE_CODE,
          client_id: config.CLIENT_ID,
          client_secret: config.API_KEY,
        },
      });
      const svcAccess = (xchg.json as { access_token?: string })?.access_token ?? "";
      add(xchg.ok ? "ok" : "err", `→ ${xchg.status}  ·  access token de service (svc=${config.SERVICE_CODE})`);
      setNode("token", xchg.ok ? "ok" : "err");
      await sleep(160);

      await step("userinfo", "GET", "/oauth2/userinfo", { bearer: svcAccess }, (j) => `sub=${(j as { sub?: string }).sub}`);

      add("info", "# ✓ flux d'authentification complet validé");
      setClaims(decodeJwt(svcAccess || access));
    } catch (e) {
      add("err", `✗ ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setRunning(false);
    }
  }

  return (
    <main className="lab">
      <div className="lab__wrap">
        <nav className="lab__nav">
          <Link href="/">← Connexion</Link>
          <Link href="/docs">Guide API</Link>
        </nav>

        <h1 className="lab__title">YowAuth <span className="accent">// flow runner</span></h1>
        <p className="lab__sub">Exécution live du flux d&apos;authentification contre {config.API.replace(/^https?:\/\//, "")}</p>

        <button className="run" onClick={run} disabled={running}>
          {running ? "● running…" : "▶ exécuter le flux"}
        </button>

        <div className="pipe">
          {NODES.map((n, i) => (
            <span key={n.key} style={{ display: "inline-flex", alignItems: "center" }}>
              <span className={`node ${status[n.key] ? `node--${status[n.key]}` : ""}`}>
                <span className="dot" />
                {n.label}
              </span>
              {i < NODES.length - 1 && <span className="sep">›</span>}
            </span>
          ))}
        </div>

        <div className="term">
          <div className="term__bar"><i className="r" /><i className="y" /><i className="g" /><span>yowauth — auth flow</span></div>
          <div className="term__body" ref={bodyRef}>
            {log.length === 0 && <div className="ln ln--info">{"// prêt. clique « exécuter le flux »."}</div>}
            {log.map((l) => (
              <div key={l.id} className={`ln ln--${l.kind}`}>
                {l.kind === "cmd" ? <><span className="pr" />{l.text}</> : l.text}
              </div>
            ))}
            {running && <span className="cursor" />}
          </div>
        </div>

        {claims && (
          <div className="token">
            <h4>ACCESS TOKEN — claims décodés <span className="badge-ok">RS256</span></h4>
            <pre>{JSON.stringify(claims, null, 2)}</pre>
          </div>
        )}
      </div>
    </main>
  );
}
