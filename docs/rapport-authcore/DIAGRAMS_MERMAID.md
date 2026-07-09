# Codes Mermaid pour les Diagrammes du Rapport YowAuth

Ces diagrammes correspondent à ceux référencés dans le document LaTeX `main.tex`. Vous pouvez copier les codes ci-dessous et les coller directement dans **draw.io** (via le menu *Organiser -> Insérer -> Avancé -> Mermaid*).

---

## 1. Diagramme de classes (`auth-core-class`)

```mermaid
classDiagram
    class UserAccount {
        -UUID id
        -UUID tenantId
        -UUID actorId
        -String username
        -String email
        -String phoneNumber
        -String passwordHash
        -String authProvider
        -String status
        -String plan
        -String onboardingStatus
        -int onboardingStep
        -boolean mfaEnabled
        -String mfaChannel
        +register()
        +enableMfa()
        +disableMfa()
        +updatePlan()
        +updateOnboarding()
    }
    
    class LoginUseCase {
        <<interface>>
        +login(command)
    }
    
    class RegisterUserUseCase {
        <<interface>>
        +register(command)
    }
    
    class DiscoverLoginContextsUseCase {
        <<interface>>
        +discover(principal)
    }
    
    class UserAccountRepository {
        <<interface>>
        +save(user)
        +findById(id)
        +findByPrincipal(tenantId, principal)
    }
    
    class UserOrganizationAccessDirectory {
        <<interface>>
        +getAccessibleOrganizations(userId)
    }
    
    class AuthApplicationService {
        -UserAccountRepository userAccountRepository
        -BCryptPasswordEncoder passwordEncoder
        +register()
        +login()
    }
    
    class AuthController {
        -LoginUseCase loginUseCase
        -RegisterUserUseCase registerUserUseCase
        +login()
        +register()
    }
    
    AuthApplicationService ..|> LoginUseCase
    AuthApplicationService ..|> RegisterUserUseCase
    AuthApplicationService --> UserAccountRepository
    AuthController --> LoginUseCase
    AuthController --> RegisterUserUseCase
    UserAccountRepository --> UserAccount
```

---

## 2. Diagramme de cas d'utilisation (`auth-core-usecase`)

```mermaid
graph TD
    User([Utilisateur final])
    Admin([Administrateur IAM])
    App([Application Cliente])
    
    subgraph YowAuth ["YowAuth (auth-core)"]
        UC1[S'inscrire (Public Sign-Up)]
        UC2[S'authentifier (Login 2 étapes & MFA)]
        UC3[Découvrir les contextes d'organisation]
        UC4[Sélectionner le contexte actif]
        UC5[Gérer son profil (Plan, MFA, Onboarding)]
        UC6[Échanger un jeton SSO (Token Exchange)]
        UC7[Créer un compte utilisateur]
    end
    
    User --> UC1
    User --> UC2
    User --> UC4
    User --> UC5
    
    Admin --> UC7
    
    App --> UC3
    App --> UC6
    
    UC2 -.->|include| UC4
```

---

## 3. DS-AU-01 — Création d'un compte par un administrateur IAM

```mermaid
sequenceDiagram
    actor Admin as Admin IAM
    participant Ctrl as AuthController
    participant UseCase as RegisterUserUseCase
    participant Repo as UserAccountRepository
    participant Audit as AuditLogService

    Admin->>Ctrl: POST /api/auth/register (Bearer, X-Client-Id)
    activate Ctrl
    Ctrl->>UseCase: register(command)
    activate UseCase
    UseCase->>Repo: findByPrincipal(tenantId, username)
    UseCase->>Repo: findByPrincipal(tenantId, email)
    Note over UseCase: Validation unicité
    UseCase->>UseCase: passwordEncoder.encode(password)
    UseCase->>Repo: save(newUserAccount)
    Repo-->>UseCase: savedAccount
    UseCase->>Audit: log("USER_REGISTERED", userId)
    UseCase-->>Ctrl: savedAccount
    deactivate UseCase
    Ctrl-->>Admin: 200 OK (UserAccountResponse)
    deactivate Ctrl
```

---

## 4. DS-AU-02/03 — Authentification tenant-scoped et émission du JWT

```mermaid
sequenceDiagram
    actor User as Utilisateur
    participant Ctrl as AuthController
    participant UseCase as LoginUseCase
    participant Repo as UserAccountRepository
    participant Perm as ReactivePermissionResolver
    participant Token as UserSessionTokenService

    User->>Ctrl: POST /api/auth/login (X-Tenant-Id, X-Client-Id, credentials)
    activate Ctrl
    Ctrl->>UseCase: login(command)
    activate UseCase
    UseCase->>Repo: findByPrincipal(tenantId, principal)
    Repo-->>UseCase: userAccount
    UseCase->>UseCase: passwordEncoder.matches(password, hash)
    Note over UseCase: Validation statut ACTIVE
    UseCase->>Perm: resolvePermissions(userId)
    Perm-->>UseCase: permissionsList
    UseCase->>Token: issueSessionToken(userId, permissions)
    Token-->>UseCase: jwtRS256
    UseCase-->>Ctrl: loginResponse (jwt, profile, organizations)
    deactivate UseCase
    Ctrl-->>User: 200 OK (Session & JWT)
    deactivate Ctrl
```

---

## 5. DS-AU-07/08 — Découverte et sélection de contexte (Login Global)

```mermaid
sequenceDiagram
    actor User as Utilisateur
    participant Ctrl as AuthController
    participant Service as AuthContextApplicationService
    participant Repo as UserAccountRepository
    participant OrgDir as UserOrganizationAccessDirectory
    participant SelToken as AuthContextSelectionTokenService
    participant JWTService as UserSessionTokenService

    %% Étape 1 : Découverte
    User->>Ctrl: POST /api/auth/discover-contexts { principal, password }
    activate Ctrl
    Ctrl->>Service: discoverContexts(principal, password)
    activate Service
    Service->>Repo: findAllByPrincipal(principal)
    Repo-->>Service: list<UserAccount> (sur différents tenants)
    Note over Service: Vérifie le mot de passe sur chaque compte
    Service->>OrgDir: getAccessibleOrganizations(userId)
    OrgDir-->>Service: list<Organization>
    Service->>SelToken: issueSelectionToken(principal, contexts)
    SelToken-->>Service: selectionToken (court, signé)
    Service-->>Ctrl: selectionToken + contextsList
    deactivate Service
    Ctrl-->>User: 200 OK (selectionToken, contexts)
    deactivate Ctrl

    %% Étape 2 : Sélection
    User->>Ctrl: POST /api/auth/select-context { selectionToken, contextId }
    activate Ctrl
    Ctrl->>Service: selectContext(selectionToken, contextId)
    activate Service
    Service->>SelToken: validateSelectionToken(selectionToken)
    Service->>JWTService: issueSessionToken(userId, contextId)
    JWTService-->>Service: jwtRS256
    Service-->>Ctrl: loginResponse (accessToken, ssoToken)
    deactivate Service
    Ctrl-->>User: 200 OK (Session & JWT contextuel)
    deactivate Ctrl
```
