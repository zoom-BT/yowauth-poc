# Codes PlantUML pour les Diagrammes du Rapport YowAuth

Ces diagrammes correspondent à ceux référencés dans le document LaTeX `main.tex`. Vous pouvez copier les codes ci-dessous pour les coller directement dans **draw.io** (via le menu *Organiser -> Insérer -> Avancé -> PlantUML*) ou les compiler sur [PlantText](https://www.planttext.com/) / [PlantUML Web](http://www.plantuml.com/plantuml).

---

## 1. Diagramme de classes (`auth-core-class.puml`)

```plantuml
@startuml
skinparam style strictuml
skinparam classAttributeIconSize 0

package domain.model {
  class UserAccount <<Aggregate Root>> {
    - id: UUID
    - tenantId: UUID
    - actorId: UUID
    - username: String
    - email: String
    - phoneNumber: String
    - passwordHash: String
    - authProvider: String
    - status: String
    - plan: String
    - onboardingStatus: String
    - onboardingStep: int
    - mfaEnabled: boolean
    - mfaChannel: String
    + register()
    + enableMfa()
    + disableMfa()
    + updatePlan()
    + updateOnboarding()
  }
}

package application.port.in {
  interface LoginUseCase {
    + login(command: LoginCommand): Mono<LoginResponse>
  }
  interface RegisterUserUseCase {
    + register(command: RegisterUserCommand): Mono<UserAccount>
  }
  interface DiscoverLoginContextsUseCase {
    + discover(principal: String): Flux<LoginContext>
  }
}

package application.port.out {
  interface UserAccountRepository {
    + save(user: UserAccount): Mono<UserAccount>
    + findById(id: UUID): Mono<UserAccount>
    + findByPrincipal(tenantId: UUID, principal: String): Mono<UserAccount>
  }
  interface UserOrganizationAccessDirectory {
    + getAccessibleOrganizations(userId: UUID): Flux<OrganizationAccess>
  }
}

package application.service {
  class AuthApplicationService {
    - userAccountRepository: UserAccountRepository
    - passwordEncoder: BCryptPasswordEncoder
    + register()
    + login()
  }
}

package adapter.in.web {
  class AuthController {
    - loginUseCase: LoginUseCase
    - registerUserUseCase: RegisterUserUseCase
    + login()
    + register()
  }
}

AuthApplicationService ..|> LoginUseCase
AuthApplicationService ..|> RegisterUserUseCase
AuthApplicationService --> UserAccountRepository
AuthController --> LoginUseCase
AuthController --> RegisterUserUseCase
UserAccountRepository --> UserAccount
@endum
```

---

## 2. Diagramme de cas d'utilisation (`auth-core-usecase.puml`)

```plantuml
@startuml
left to right direction
skinparam packageStyle rect

actor "Utilisateur final" as User
actor "Administrateur IAM" as Admin
actor "Application Cliente" as App

rectangle "YowAuth (auth-core)" {
  usecase "S'inscrire (Public Sign-Up)" as UC_SignUp
  usecase "S'authentifier (Login 2 étapes & MFA)" as UC_Login
  usecase "Découvrir les contextes d'organisation" as UC_Discover
  usecase "Sélectionner le contexte actif" as UC_Select
  usecase "Gérer son profil (Plan, MFA, Onboarding)" as UC_Profile
  usecase "Échanger un jeton SSO (Token Exchange)" as UC_SSO
  usecase "Créer un compte utilisateur" as UC_Register
}

User --> UC_SignUp
User --> UC_Login
User --> UC_Profile
User --> UC_Select

Admin --> UC_Register

App --> UC_Discover
App --> UC_SSO

UC_Login ..> UC_Select : <<include>>
@endum
```

---

## 3. DS-AU-01 — Création d'un compte par un administrateur IAM

```plantuml
@startuml
actor "Admin IAM" as Admin
participant AuthController as Ctrl
participant RegisterUserUseCase as UseCase
database UserAccountRepository as Repo
participant AuditLogService as Audit

Admin -> Ctrl: POST /api/auth/register (Bearer Token, X-Client-Id)
activate Ctrl
Ctrl -> UseCase: register(command)
activate UseCase
UseCase -> Repo: findByPrincipal(tenantId, username)
UseCase -> Repo: findByPrincipal(tenantId, email)
Note over UseCase: Validation unicité
UseCase -> UseCase: passwordEncoder.encode(password)
UseCase -> Repo: save(newUserAccount)
Repo --> UseCase: savedAccount
UseCase -> Audit: log("USER_REGISTERED", userId)
UseCase --> Ctrl: savedAccount
deactivate UseCase
Ctrl --> Admin: 200 OK (UserAccountResponse)
deactivate Ctrl
@endum
```

---

## 4. DS-AU-02/03 — Authentification tenant-scoped et émission du JWT

```plantuml
@startuml
actor "Utilisateur" as User
participant AuthController as Ctrl
participant LoginUseCase as UseCase
database UserAccountRepository as Repo
participant ReactivePermissionResolver as Perm
participant UserSessionTokenService as Token

User -> Ctrl: POST /api/auth/login (X-Tenant-Id, X-Client-Id, credentials)
activate Ctrl
Ctrl -> UseCase: login(command)
activate UseCase
UseCase -> Repo: findByPrincipal(tenantId, principal)
Repo --> UseCase: userAccount
UseCase -> UseCase: passwordEncoder.matches(password, hash)
Note over UseCase: Validation statut ACTIVE
UseCase -> Perm: resolvePermissions(userId)
Perm --> UseCase: permissionsList
UseCase -> Token: issueSessionToken(userId, permissions)
Token --> UseCase: jwtRS256
UseCase --> Ctrl: loginResponse (jwt, profile, organizations)
deactivate UseCase
Ctrl --> User: 200 OK (Session & JWT)
deactivate Ctrl
@endum
```

---

## 5. DS-AU-07/08 — Découverte et sélection de contexte (Login Global)

```plantuml
@startuml
actor "Utilisateur" as User
participant AuthController as Ctrl
participant AuthContextApplicationService as Service
database UserAccountRepository as Repo
participant UserOrganizationAccessDirectory as OrgDir
participant AuthContextSelectionTokenService as SelToken
participant UserSessionTokenService as JWTService

%% Étape 1 : Découverte
User -> Ctrl: POST /api/auth/discover-contexts { principal, password }
activate Ctrl
Ctrl -> Service: discoverContexts(principal, password)
activate Service
Service -> Repo: findAllByPrincipal(principal)
Repo --> Service: list<UserAccount> (sur différents tenants)
Note over Service: Vérifie le mot de passe sur chaque compte
Service -> OrgDir: getAccessibleOrganizations(userId)
OrgDir --> Service: list<Organization>
Service -> SelToken: issueSelectionToken(principal, contexts)
SelToken --> Service: selectionToken (court, signé)
Service --> Ctrl: selectionToken + contextsList
deactivate Service
Ctrl --> User: 200 OK (selectionToken, contexts)
deactivate Ctrl

%% Étape 2 : Sélection
User -> Ctrl: POST /api/auth/select-context { selectionToken, contextId }
activate Ctrl
Ctrl -> Service: selectContext(selectionToken, contextId)
activate Service
Service -> SelToken: validateSelectionToken(selectionToken)
Service -> JWTService: issueSessionToken(userId, contextId)
JWTService --> Service: jwtRS256
Service --> Ctrl: loginResponse (accessToken, ssoToken)
deactivate Service
Ctrl --> User: 200 OK (Session & JWT contextuel)
deactivate Ctrl
@endum
```
