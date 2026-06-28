#!/usr/bin/env bash
#
# YowAuth POC — smoke test du flux d'authentification complet.
# Prouve, contre une instance live, l'enchaînement :
#   sign-up -> verification email (PREVIEW) -> confirm -> login
#   -> users/me (bearer) -> oauth2/token (token-exchange) -> oauth2/userinfo
#   + endpoints publics OIDC (jwks, openid-configuration).
#
# Usage :
#   BASE_URL=http://localhost:8080 bash poc/smoke.sh
#   BASE_URL=https://<app>.onrender.com CLIENT_ID=poc-client API_KEY=*** bash poc/smoke.sh
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CLIENT_ID="${CLIENT_ID:-poc-client}"
API_KEY="${API_KEY:-poc-secret-key}"
TENANT="${TENANT_ID:-00000000-0000-0000-0000-000000000001}"
SERVICE_CODE="${SERVICE_CODE:-SALES}"
PW="P@ssw0rd!2024"
U="poc_$(date +%s)"

H=(-H "Content-Type: application/json" -H "X-Client-Id: ${CLIENT_ID}" -H "X-Api-Key: ${API_KEY}" -H "X-Tenant-Id: ${TENANT}")

jq_field() { python -c "import sys,json;d=json.load(sys.stdin);print(d$1)"; }
section() { printf '\n\033[1;36m== %s ==\033[0m\n' "$1"; }

echo "BASE_URL=${BASE_URL}  user=${U}"

section "0. health"
curl -s -o /dev/null -w "HTTP %{http_code}\n" "${BASE_URL}/actuator/health"

section "1. openid-configuration (découverte OIDC)"
curl -s "${BASE_URL}/.well-known/openid-configuration" | python -m json.tool

section "2. JWKS (clé publique RS256)"
curl -s "${BASE_URL}/.well-known/jwks.json" | python -m json.tool

section "3. sign-up"
curl -s "${H[@]}" -X POST "${BASE_URL}/api/auth/sign-up" \
  -d "{\"tenantId\":\"${TENANT}\",\"username\":\"${U}\",\"email\":\"${U}@demo.io\",\"password\":\"${PW}\",\"firstName\":\"Demo\",\"lastName\":\"User\"}" \
  -w "\n[HTTP %{http_code}]\n"

section "4. email-verification/resend (mode PREVIEW -> token en clair)"
RESEND=$(curl -s "${H[@]}" -X POST "${BASE_URL}/api/auth/email-verification/resend" -d "{\"principal\":\"${U}\"}")
echo "$RESEND" | python -m json.tool
VTOKEN=$(echo "$RESEND" | jq_field "['data']['challengeTokenPreview']")

section "5. email-verification/confirm"
curl -s "${H[@]}" -X POST "${BASE_URL}/api/auth/email-verification/confirm" \
  -d "{\"verificationToken\":\"${VTOKEN}\"}" -w "\n[HTTP %{http_code}]\n" >/dev/null && echo "email vérifié"

section "6. login (-> accessToken + SSO + organisations)"
LOGIN=$(curl -s "${H[@]}" -X POST "${BASE_URL}/api/auth/login" -d "{\"principal\":\"${U}\",\"password\":\"${PW}\"}")
echo "$LOGIN" | python -m json.tool
ACCESS=$(echo "$LOGIN" | jq_field "['data']['accessToken']")
SSO=$(echo "$LOGIN" | jq_field "['data']['sharedSession']['token']")

section "7. users/me (bearer accessToken)"
curl -s "${H[@]}" -H "Authorization: Bearer ${ACCESS}" "${BASE_URL}/api/users/me" -w "\n[HTTP %{http_code}]\n" | head -c 800; echo

section "8. oauth2/token (token-exchange SSO -> access token de service)"
CTX=$(echo "$SSO" | python -c "import sys,base64,json;p=sys.stdin.read().split('.')[1];p+='='*(-len(p)%4);print(json.loads(base64.urlsafe_b64decode(p))['contexts'][0]['contextId'])")
XCHG=$(curl -s -X POST "${BASE_URL}/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \
  --data-urlencode "subject_token=${SSO}" \
  --data-urlencode "subject_token_type=urn:ietf:params:oauth:token-type:jwt" \
  --data-urlencode "context_id=${CTX}" \
  --data-urlencode "service_code=${SERVICE_CODE}" \
  --data-urlencode "client_id=${CLIENT_ID}" \
  --data-urlencode "client_secret=${API_KEY}")
echo "$XCHG" | python -m json.tool
SVC_ACCESS=$(echo "$XCHG" | jq_field "['access_token']")

section "9. oauth2/userinfo (bearer access token de service)"
curl -s "${BASE_URL}/oauth2/userinfo" -H "Authorization: Bearer ${SVC_ACCESS}" | python -m json.tool

printf '\n\033[1;32m✓ Flux YowAuth complet validé contre %s\033[0m\n' "${BASE_URL}"
