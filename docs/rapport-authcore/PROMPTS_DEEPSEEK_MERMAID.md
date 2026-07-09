# Prompts DeepSeek → Mermaid (diagrammes du rapport auth-core)

Colle chaque prompt dans DeepSeek. Récupère le bloc ```mermaid```, rends-le en PNG
(mermaid.live / VS Code Mermaid / `mmdc`) et dépose-le dans `figures/` avec le nom indiqué,
puis décommente le `\includegraphics{...}` correspondant dans `main.tex`.

Consigne commune (mets-la en tête de chaque prompt) :
> Tu es un expert Mermaid. Réponds UNIQUEMENT par un bloc ```mermaid``` valide, sans texte
> autour. Diagramme sobre, lisible, en français, adapté à un rapport technique noir/bleu.

---

## 1. Diagramme de classes — `figures/auth-core-class.png`
> [consigne commune] Génère un `classDiagram` Mermaid du module **auth-core** (hexagonal).
> Classe centrale immuable **UserAccount** avec attributs : id, tenantId, actorId, username,
> email, phoneNumber, passwordHash, authProvider, externalSubject, status, plan,
> onboardingStatus, onboardingStep, accountType, businessType, emailVerified, emailVerifiedAt,
> phoneVerified, phoneVerifiedAt, mfaEnabled, mfaChannel ; méthodes : register(),
> markEmailVerified(), enableMfa(), disableMfa(), updatePlan(), updateOnboarding(),
> linkExternalIdentity(), updatePassword(). Ajoute une énumération **UserAccountStatus**
> {ACTIVE, SUSPENDED, REVOKED} reliée à UserAccount. Montre les services d'application qui
> dépendent de UserAccount : AuthApplicationService, AuthContextApplicationService,
> AuthOidcService, AuthSharedSessionService. Montre les ports sortants (interfaces
> «interface») : UserAccountRepository, RefreshTokenRepository, UserOrganizationAccessDirectory,
> SignUpContextDirectory, et le service kernel JwtTokenService (signature RS256 + JWKS).
> Relie AuthApplicationService --> UserAccountRepository, --> UserOrganizationAccessDirectory ;
> AuthOidcService --> JwtTokenService.

## 2. Cas d'utilisation — `figures/auth-core-usecase.png`
> [consigne commune] Génère un diagramme de cas d'utilisation en Mermaid (utilise un `flowchart LR`
> avec acteurs à gauche = personnages, cas d'usage = ovales `(( ))` ou `([ ])`, cadre
> "auth-core"). Acteurs : "Administrateur IAM", "Utilisateur", "Backend consommateur".
> Cas : Créer un compte (register), S'inscrire en self-service (sign-up), Identifier un principal
> (email-first), Authentifier dans un tenant (login), Découvrir les contextes de login,
> Sélectionner un contexte tenant/organisation, Consulter son profil (/users/me),
> Mettre à jour son plan, Mettre à jour son onboarding, Émettre un challenge de vérification
> email, Réinitialiser le mot de passe, Émettre un JWT d'accès RS256 (inclus par les logins),
> Journaliser les opérations de sécurité (inclus par la plupart). Relie chaque acteur à ses cas.

## 3. DS-AU-01 Register — `figures/ds-au-01-register.png`
> [consigne commune] `sequenceDiagram` Mermaid : création d'un compte par un admin IAM.
> Participants : Backend admin, Security Filter, AuthController, AuthApplicationService,
> UserAccountRepo, RecordSystemAuditUseCase, PostgreSQL. Flux : POST /api/auth/register
> (X-Client-Id + X-Api-Key + Bearer admin) → requête autorisée → register(command) →
> existsByUsername(tenantId, username) → SELECT → false → encode(password) →
> save(UserAccount.register(...)) → INSERT → record(USER_REGISTERED) → HTTP 201 Created.

## 4. DS-AU-02/03 Login + JWT RS256 — `figures/ds-au-02-login.png`
> [consigne commune] `sequenceDiagram` Mermaid : login tenant-scoped puis émission du JWT.
> Participants : Backend consommateur, AuthController, AuthApplicationService, UserAccountRepo,
> ReactivePermissionResolver, UserSessionTokenService, JwtTokenService. Flux :
> POST /api/auth/login {principal,password} → findByPrincipal(tenantId,principal) →
> user account → passwordEncoder.matches() → alt [invalides] InvalidLoginCredentialsException ;
> [ok] resolvePermissions(tenantId,userId) → authorities → issue(tenantId,userId,actorId,
> authorities) → JwtTokenService.issue(jwtClaims) → accessToken RS256 → HTTP 200
> (id, username, accessToken, tokenType, expiresInSeconds, organizations[]).

## 5. DS-AU-07/08 Découverte + sélection de contexte — `figures/ds-au-07-context.png`
> [consigne commune] `sequenceDiagram` Mermaid en deux temps.
> (a) POST /api/auth/discover-contexts {principal,password} : AuthController →
> AuthContextApplicationService → UserAccountRepo.findAllByPrincipal → loop [chaque compte]
> passwordEncoder.matches → alt [mot de passe valide] UserOrganizationAccessDirectory.
> listUserOrganizations → organisations → build SelectableLoginContext → end →
> AuthContextSelectionTokenService.issue → selectionToken + contexts[].
> (b) POST /api/auth/select-context {selectionToken, contextId, organizationId?} :
> verify selection token → resolve context → validate organization access →
> resolvePermissions → UserSessionTokenService.issue → accessToken RS256 → LoginResponse.

## 6. DS-AU-10 Reset mot de passe (SMTP ou PREVIEW_ONLY) — `figures/ds-au-10-reset.png`
> [consigne commune] `sequenceDiagram` Mermaid : reset de mot de passe.
> Participants : Utilisateur, AuthController, AuthApplicationService, UserAccountRepo,
> AuthPasswordResetTokenService, AuthEmailDeliveryService, RecordSystemAuditUseCase.
> Flux : POST /forgot-password {principal} → findAllByPrincipal → issueSelection → selectionToken.
> POST /password-reset/issue {selectionToken, contextId} → verifySelection → issueResetToken →
> resetToken signé → deliverPasswordReset(email, token) → alt [SMTP configuré] deliveryMode=SMTP ;
> [fallback] deliveryMode=PREVIEW_ONLY, challengeTokenPreview=token → record(USER_PASSWORD_RESET_REQUESTED).
> POST /reset-password {resetToken, newPassword} → verifyResetToken → findById → save(passwordHash) →
> record(USER_PASSWORD_RESET_COMPLETED) → HTTP 200.

## 7. DS-AU-11 Vérification d'email — `figures/ds-au-11-emailverif.png`
> [consigne commune] `sequenceDiagram` Mermaid : vérification d'email.
> Participants : Utilisateur, AuthController, AuthApplicationService,
> AuthEmailVerificationTokenService, UserAccountRepo, AuthEmailDeliveryService,
> RecordSystemAuditUseCase. Flux : POST /email-verification/request (Bearer) →
> issueCurrentEmailVerification → issue(tenantId,userId) → verificationToken signé →
> deliverEmailVerification → alt [SMTP] SMTP ; [fallback] PREVIEW_ONLY challengeTokenPreview →
> record(USER_EMAIL_VERIFICATION_REQUESTED) → HTTP 200.
> POST /email-verification/confirm {verificationToken} → verifyToken → findById →
> save(markEmailVerified()) → record(USER_EMAIL_VERIFIED) → HTTP 200 (emailVerified=true).
