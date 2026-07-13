import { NextRequest } from "next/server";

/**
 * BFF proxy — le navigateur n'appelle que /api/proxy/... (même origine, donc zéro CORS).
 * Ce handler s'exécute côté serveur : il injecte l'identité de l'application cliente
 * (X-Client-Id / X-Api-Key / X-Tenant-Id) qui ne quitte jamais le serveur, puis relaie
 * vers le kernel. Les secrets ne sont donc jamais exposés dans le bundle navigateur.
 *
 *   Navigateur : POST /api/proxy/api/auth/login  ->  Kernel : POST <KSM_BACKEND_URL>/api/auth/login
 */
export const runtime = "nodejs";

const BACKEND = (process.env.KSM_BACKEND_URL ?? "https://kernel-core.yowyob.com/kernel-api").replace(/\/$/, "");
const CLIENT_ID = process.env.KSM_CLIENT_ID ?? "";
const CLIENT_KEY = process.env.KSM_CLIENT_KEY ?? "";
const TENANT_ID = process.env.KSM_TENANT_ID ?? "";

// Headers du navigateur qu'on relaie tels quels vers le kernel.
const FORWARD = ["content-type", "authorization", "x-organization-id", "x-agency-id", "x-tenant-id"];

type Ctx = { params: Promise<{ path: string[] }> };

async function handle(req: NextRequest, ctx: Ctx): Promise<Response> {
  const { path } = await ctx.params;
  const backendPath = path.join("/");
  const target = `${BACKEND}/${backendPath}${req.nextUrl.search}`;

  const headers = new Headers();
  for (const h of FORWARD) {
    const v = req.headers.get(h);
    if (v) headers.set(h, v);
  }

  // Identité de l'application cliente — injectée côté serveur, jamais dans le navigateur.
  headers.set("x-client-id", CLIENT_ID);
  headers.set("x-api-key", CLIENT_KEY);
  if (TENANT_ID && !headers.has("x-tenant-id")) headers.set("x-tenant-id", TENANT_ID);

  // Token-exchange OIDC : le kernel attend les identifiants client en Basic auth.
  if (backendPath === "oauth2/token") {
    headers.set("authorization", "Basic " + Buffer.from(`${CLIENT_ID}:${CLIENT_KEY}`).toString("base64"));
  }

  const method = req.method.toUpperCase();
  const body = method === "GET" || method === "HEAD" ? undefined : await req.arrayBuffer();

  const res = await fetch(target, { method, headers, body, redirect: "manual" });

  const out = new Headers();
  const ct = res.headers.get("content-type");
  if (ct) out.set("content-type", ct);
  return new Response(res.body, { status: res.status, headers: out });
}

export async function GET(req: NextRequest, ctx: Ctx) { return handle(req, ctx); }
export async function POST(req: NextRequest, ctx: Ctx) { return handle(req, ctx); }
export async function PUT(req: NextRequest, ctx: Ctx) { return handle(req, ctx); }
export async function PATCH(req: NextRequest, ctx: Ctx) { return handle(req, ctx); }
export async function DELETE(req: NextRequest, ctx: Ctx) { return handle(req, ctx); }
export async function HEAD(req: NextRequest, ctx: Ctx) { return handle(req, ctx); }
export async function OPTIONS(req: NextRequest, ctx: Ctx) { return handle(req, ctx); }
