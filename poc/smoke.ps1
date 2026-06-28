<#
  YowAuth POC - smoke test du flux d'authentification complet (PowerShell).
  Prouve, contre une instance live, l'enchainement :
    sign-up -> verification email (PREVIEW) -> confirm -> login
    -> users/me (bearer) -> oauth2/token (token-exchange) -> oauth2/userinfo
    + endpoints publics OIDC (jwks, openid-configuration).

  Usage :
    ./poc/smoke.ps1
    ./poc/smoke.ps1 -BaseUrl "https://<app>.onrender.com" -ApiKey "***"
#>
param(
  [string]$BaseUrl     = "http://localhost:8080",
  [string]$ClientId    = "poc-client",
  [string]$ApiKey      = "poc-secret-key",
  [string]$TenantId    = "00000000-0000-0000-0000-000000000001",
  [string]$ServiceCode = "SALES"
)

$ErrorActionPreference = "Stop"
$pw = "P@ssw0rd!2024"
$u  = "poc_$([int][double]::Parse((Get-Date -UFormat %s)))"
$headers = @{
  "Content-Type" = "application/json"
  "X-Client-Id"  = $ClientId
  "X-Api-Key"    = $ApiKey
  "X-Tenant-Id"  = $TenantId
}
function Section($t) { Write-Host "`n== $t ==" -ForegroundColor Cyan }
function Show($o) { $o | ConvertTo-Json -Depth 8 }
function Decode-JwtPayload($jwt) {
  $p = $jwt.Split(".")[1].Replace('-', '+').Replace('_', '/')
  switch ($p.Length % 4) { 2 { $p += "==" } 3 { $p += "=" } }
  [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p)) | ConvertFrom-Json
}

Write-Host "BaseUrl=$BaseUrl  user=$u"

Section "0. health"
(Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing).StatusCode

Section "1. openid-configuration (decouverte OIDC)"
Show (Invoke-RestMethod -Uri "$BaseUrl/.well-known/openid-configuration")

Section "2. JWKS (cle publique RS256)"
Show (Invoke-RestMethod -Uri "$BaseUrl/.well-known/jwks.json")

Section "3. sign-up"
$signup = Invoke-RestMethod -Uri "$BaseUrl/api/auth/sign-up" -Method Post -Headers $headers `
  -Body (@{ tenantId=$TenantId; username=$u; email="$u@demo.io"; password=$pw; firstName="Demo"; lastName="User" } | ConvertTo-Json)
Show $signup

Section "4. email-verification/resend (mode PREVIEW -> token en clair)"
$resend = Invoke-RestMethod -Uri "$BaseUrl/api/auth/email-verification/resend" -Method Post -Headers $headers `
  -Body (@{ principal=$u } | ConvertTo-Json)
Show $resend
$vtoken = $resend.data.challengeTokenPreview

Section "5. email-verification/confirm"
Invoke-RestMethod -Uri "$BaseUrl/api/auth/email-verification/confirm" -Method Post -Headers $headers `
  -Body (@{ verificationToken=$vtoken } | ConvertTo-Json) | Out-Null
Write-Host "email verifie"

Section "6. login (-> accessToken + SSO + organisations)"
$login = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Headers $headers `
  -Body (@{ principal=$u; password=$pw } | ConvertTo-Json)
Show $login
$access = $login.data.accessToken
$sso    = $login.data.sharedSession.token

Section "7. users/me (bearer accessToken)"
$h2 = $headers.Clone(); $h2["Authorization"] = "Bearer $access"
Show (Invoke-RestMethod -Uri "$BaseUrl/api/users/me" -Headers $h2)

Section "8. oauth2/token (token-exchange SSO -> access token de service)"
$ctx = (Decode-JwtPayload $sso).contexts[0].contextId
$form = @{
  grant_type         = "urn:ietf:params:oauth:grant-type:token-exchange"
  subject_token      = $sso
  subject_token_type = "urn:ietf:params:oauth:token-type:jwt"
  context_id         = $ctx
  service_code       = $ServiceCode
  client_id          = $ClientId
  client_secret      = $ApiKey
}
$xchg = Invoke-RestMethod -Uri "$BaseUrl/oauth2/token" -Method Post -Body $form `
  -ContentType "application/x-www-form-urlencoded"
Show $xchg

Section "9. oauth2/userinfo (bearer access token de service)"
Show (Invoke-RestMethod -Uri "$BaseUrl/oauth2/userinfo" -Headers @{ Authorization = "Bearer $($xchg.access_token)" })

Write-Host "`n[OK] Flux YowAuth complet valide contre $BaseUrl" -ForegroundColor Green
