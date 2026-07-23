# syntax=docker/dockerfile:1
# ─────────────────────────────────────────────────────────────────────────────
# Image de production du front YowAuth (Next.js 16, sortie "standalone").
# Rendu strictement identique à Vercel : l'app tourne comme un serveur Node,
# le BFF proxy (app/api/proxy) fonctionne donc exactement pareil.
#
# Build :
#   docker build \
#     --build-arg NEXT_PUBLIC_CLIENT_ID=prod-platform-backend \
#     --build-arg NEXT_PUBLIC_TENANT_ID=11111111-1111-1111-1111-111111111111 \
#     --build-arg NEXT_PUBLIC_SERVICE_CODE=SALES \
#     -t yowauth-front .
#
# Run (les secrets serveur KSM_* sont injectés au RUNTIME, jamais dans l'image) :
#   docker run -p 3000:3000 --env-file .env.runtime yowauth-front
# ─────────────────────────────────────────────────────────────────────────────

# ── 1. Dépendances ───────────────────────────────────────────────────────────
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ── 2. Build ─────────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Variables PUBLIQUES (non secrètes) — inlinées au BUILD par Next.
# À passer en --build-arg ; valeurs d'affichage + corps de requête sign-up.
ARG NEXT_PUBLIC_CLIENT_ID
ARG NEXT_PUBLIC_TENANT_ID
ARG NEXT_PUBLIC_SERVICE_CODE=SALES
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_CLIENT_ID=$NEXT_PUBLIC_CLIENT_ID \
    NEXT_PUBLIC_TENANT_ID=$NEXT_PUBLIC_TENANT_ID \
    NEXT_PUBLIC_SERVICE_CODE=$NEXT_PUBLIC_SERVICE_CODE \
    NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL \
    NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ── 3. Runtime ───────────────────────────────────────────────────────────────
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

# Utilisateur non-root
RUN addgroup --system --gid 1001 nodejs \
 && adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

# server.js est généré par la sortie "standalone" de Next.
CMD ["node", "server.js"]
