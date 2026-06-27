# YowAuth POC Deployment — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extraire `auth-core` (YowAuth) et ses dépendances dans un repo autonome `yowauth-poc`, le faire booter en mémoire, le déployer gratuitement sur Render, et prouver les endpoints via un POC Next.js déployé sur Vercel.

**Architecture:** Slice hexagonal = 5 cores copiés (`common`, `kernel`, `actor`, `roles`, `auth`) + un runner `auth-poc-app`. Les 3 ports liés à l'organisation (normalement servis par organization-core via bootstrap) sont remplacés par des adaptateurs de sortie in-memory seedés. Aucune base de données externe : profil in-memory, JWT RSA auto-généré.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring WebFlux, Maven (multi-module), Docker, Render, Next.js (App Router), Vercel.

## Global Constraints

- Java version : **21** (`<java.version>21</java.version>`, `maven.compiler.release=21`).
- Spring Boot : **4.0.6** (`spring-boot.version`), géré par le `dependencyManagement` du pom parent — ne pas changer.
- groupId/version des cores : **`yowyob.comops.api` / `0.1.0-SNAPSHOT`** — conservés à l'identique.
- Package de base scanné : **`yowyob.comops.api`**.
- **Isolation absolue** : tout le code vit dans `c:\Users\Tchoutzine\Documents\GitHub\yowauth-poc\`. **Rien** n'est écrit dans le fork `KSM_Kernel_Layer` (il sert uniquement de source de copie en lecture seule).
- Source de copie des cores : `c:\Users\Tchoutzine\Documents\GitHub\KSM_Kernel_Layer\`.
- Profil runtime : **in-memory par défaut** (`iwm.persistence.mode=memory`, pas de R2DBC/Redis/ES/Kafka actif).
- Client bootstrap POC : `client-id=poc-client`, secret via env.
- Management API key : ≥ 16 caractères, hors liste interdite (`change-me`, `changeme`, `default`, `admin`, `password`, `secret`, `dev-management-key`, `dev-management-api-key`).

---

### Task 1: Scaffold du repo — copie des 5 cores + pom parent trimé

**Files:**
- Copy: `RT-comops-common-core/`, `RT-comops-kernel-core/`, `RT-comops-actor-core/`, `RT-comops-roles-core/`, `RT-comops-auth-core/` (depuis le fork)
- Create: `pom.xml` (parent trimé, basé sur celui du fork)

**Interfaces:**
- Produces: les 5 modules Maven `yowyob.comops.api:RT-comops-{common,kernel,actor,roles,auth}-core:0.1.0-SNAPSHOT` disponibles localement.

- [ ] **Step 1: Copier les 5 cores depuis le fork**

```bash
cd "c:/Users/Tchoutzine/Documents/GitHub/yowauth-poc"
SRC="c:/Users/Tchoutzine/Documents/GitHub/KSM_Kernel_Layer"
for m in common kernel actor roles auth; do
  cp -r "$SRC/RT-comops-${m}-core" "./RT-comops-${m}-core"
done
# Retirer les target/ éventuellement copiés
find . -type d -name target -prune -exec rm -rf {} +
ls -d RT-comops-*-core
```
Expected: liste des 5 dossiers `RT-comops-*-core`.

- [ ] **Step 2: Copier puis trimer le pom parent**

Copier `pom.xml` du fork vers la racine, puis remplacer le bloc `<modules>…</modules>` par exactement :

```xml
  <modules>
    <module>RT-comops-common-core</module>
    <module>RT-comops-kernel-core</module>
    <module>RT-comops-actor-core</module>
    <module>RT-comops-roles-core</module>
    <module>RT-comops-auth-core</module>
    <module>auth-poc-app</module>
  </modules>
```

Tout le reste du pom parent (`properties`, `dependencyManagement`, `dependencies` de test, `build/pluginManagement`) est **conservé tel quel**.

- [ ] **Step 3: Vérifier que les cores compilent (sans auth-poc-app encore)**

Commenter temporairement la ligne `<module>auth-poc-app</module>` (pas encore créé), puis :

```bash
mvn -q -DskipTests -pl RT-comops-auth-core,RT-comops-roles-core -am compile
```
Expected: `BUILD SUCCESS`. Si échec sur une dépendance manquante (core non copié), noter le module manquant — il faudra l'ajouter au slice. Décommenter `auth-poc-app` après cette vérif.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: scaffold slice (5 cores copies + pom parent trime)"
```

---

### Task 2: Module `auth-poc-app` — runner Spring Boot

**Files:**
- Create: `auth-poc-app/pom.xml`
- Create: `auth-poc-app/src/main/java/yowyob/comops/api/pocapp/AuthPocApplication.java`
- Create: `auth-poc-app/src/main/resources/application.yml`

**Interfaces:**
- Consumes: les 5 cores de Task 1.
- Produces: artefact exécutable `auth-poc-app-0.1.0-SNAPSHOT.jar` (main class `yowyob.comops.api.pocapp.AuthPocApplication`).

- [ ] **Step 1: Créer `auth-poc-app/pom.xml`**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>yowyob.comops.api</groupId>
    <artifactId>RT-Comops</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>auth-poc-app</artifactId>
  <packaging>jar</packaging>
  <name>yowauth-poc-app</name>

  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webflux</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-mail</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
      <version>3.0.3</version>
    </dependency>
    <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>

    <dependency><groupId>yowyob.comops.api</groupId><artifactId>RT-comops-common-core</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>yowyob.comops.api</groupId><artifactId>RT-comops-kernel-core</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>yowyob.comops.api</groupId><artifactId>RT-comops-actor-core</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>yowyob.comops.api</groupId><artifactId>RT-comops-roles-core</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>yowyob.comops.api</groupId><artifactId>RT-comops-auth-core</artifactId><version>${project.version}</version></dependency>

    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>io.projectreactor</groupId><artifactId>reactor-test</artifactId><scope>test</scope></dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <mainClass>yowyob.comops.api.pocapp.AuthPocApplication</mainClass>
        </configuration>
        <executions>
          <execution><goals><goal>repackage</goal></goals></execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

> Note : `data-r2dbc`, `data-redis-reactive`, `data-elasticsearch`, `r2dbc-postgresql`, `liquibase` sont **volontairement absents**. Les libs Kafka/ES/Redis arrivent peut-être transitivement via kernel-core ; elles sont neutralisées par `application.yml` (Task 2, Step 3).

- [ ] **Step 2: Créer `AuthPocApplication.java`**

```java
package yowyob.comops.api.pocapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "yowyob.comops.api")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthPocApplication.class, args);
    }
}
```

- [ ] **Step 3: Créer `application.yml` (profil in-memory par défaut)**

```yaml
server:
  port: ${PORT:8080}

spring:
  liquibase:
    enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration
      - org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration
      - org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration
      - org.springframework.boot.r2dbc.autoconfigure.health.ConnectionFactoryHealthContributorAutoConfiguration
      - org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
      - org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration
      - org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration

management:
  health:
    elasticsearch:
      enabled: false
    redis:
      enabled: false
  endpoint:
    health:
      show-details: always
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

iwm:
  persistence:
    mode: memory
  outbox:
    replay-on-startup:
      enabled: false
    delivery:
      type: recording
    consumers:
      mode: inline
    relay:
      enabled: false
    allow-manual-relay-only: true
  redis:
    permission-cache:
      enabled: false
  search:
    elasticsearch:
      enabled: false
  management:
    security:
      api-key: ${IWM_MANAGEMENT_API_KEY:poc-management-key-please-rotate}
  security:
    cors:
      allowed-origins: ${IWM_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://127.0.0.1:3000}
    client-applications:
      bootstrap:
        enabled: true
        client-id: poc-client
        name: YowAuth POC Client
        description: Bootstrap client for the YowAuth POC.
        secret: ${IWM_POC_CLIENT_SECRET:poc-secret-key}
    jwt:
      auto-generate-key-pair: true
  organization:
    service-subscriptions:
      provision-subscribable-services-on-create: true
```

> Si le boot révèle qu'une clé `iwm.*` exigée manque (ex. `allowed-services` du client bootstrap), l'ajouter ici lors de Task 4.

- [ ] **Step 4: Vérifier la compilation du module**

```bash
mvn -q -DskipTests -pl auth-poc-app -am compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: module auth-poc-app (runner Spring Boot in-memory)"
```

---

### Task 3: Adaptateurs in-memory pour les 3 ports organisation

**Files:**
- Create: `auth-poc-app/src/main/java/yowyob/comops/api/pocapp/config/InMemoryOrganizationAdaptersConfig.java`
- Test: `auth-poc-app/src/test/java/yowyob/comops/api/pocapp/config/InMemoryOrganizationAdaptersConfigTest.java`

**Interfaces:**
- Consumes (ports, déjà définis dans les cores) :
  - `kernel … OrganizationServiceRuntimeEntitlementDirectory.resolveRuntimeEntitlement(UUID tenantId, UUID organizationId, String serviceCode) : Mono<OrganizationServiceRuntimeEntitlement>`
  - record `OrganizationServiceRuntimeEntitlement(String serviceCode, boolean effective, Long requestQuotaLimit, Long requestQuotaWindowSeconds)`
  - `auth … UserOrganizationAccessDirectory.listUserOrganizations(UUID tenantId, UUID userId) : Flux<UserOrganizationAccess>`
  - record `UserOrganizationAccess(UUID organizationId, String organizationCode, String shortName, String longName, List<String> services)`
  - `auth … SignUpContextDirectory.findByOrganizationCode(String organizationCode) : Flux<SignUpContext>`
  - record `SignUpContextDirectory.SignUpContext(String contextId, UUID tenantId, UUID organizationId, String organizationCode, String organizationName, String organizationType)`
- Produces: constantes publiques `DEMO_TENANT_ID`, `DEMO_ORG_ID`, `DEMO_ORG_CODE` (réutilisées par smoke + Next.js).

- [ ] **Step 1: Écrire le test (échoue à la compilation tant que la config n'existe pas)**

```java
package yowyob.comops.api.pocapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import yowyob.comops.api.auth.application.port.out.SignUpContextDirectory;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlement;

class InMemoryOrganizationAdaptersConfigTest {

    private final InMemoryOrganizationAdaptersConfig config = new InMemoryOrganizationAdaptersConfig();

    @Test
    void entitlementDirectoryAlwaysGrantsService() {
        StepVerifier.create(config.inMemoryEntitlementDirectory()
                        .resolveRuntimeEntitlement(UUID.randomUUID(), UUID.randomUUID(), "SALES"))
                .assertNext(e -> {
                    assertThat(e.serviceCode()).isEqualTo("SALES");
                    assertThat(e.effective()).isTrue();
                    assertThat(e.requestQuotaLimit()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void userOrganizationAccessReturnsSeededOrg() {
        StepVerifier.create(config.inMemoryUserOrganizationAccessDirectory()
                        .listUserOrganizations(InMemoryOrganizationAdaptersConfig.DEMO_TENANT_ID, UUID.randomUUID()))
                .assertNext(a -> {
                    assertThat(a.organizationId()).isEqualTo(InMemoryOrganizationAdaptersConfig.DEMO_ORG_ID);
                    assertThat(a.organizationCode()).isEqualTo(InMemoryOrganizationAdaptersConfig.DEMO_ORG_CODE);
                    assertThat(a.services()).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    void signUpContextMatchesDemoCodeCaseInsensitive() {
        StepVerifier.create(config.inMemorySignUpContextDirectory()
                        .findByOrganizationCode(InMemoryOrganizationAdaptersConfig.DEMO_ORG_CODE.toLowerCase()))
                .assertNext(c -> assertThat(c.organizationCode())
                        .isEqualTo(InMemoryOrganizationAdaptersConfig.DEMO_ORG_CODE))
                .verifyComplete();
    }

    @Test
    void signUpContextEmptyForUnknownCode() {
        StepVerifier.create(config.inMemorySignUpContextDirectory().findByOrganizationCode("UNKNOWN"))
                .verifyComplete();
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue (compilation)**

```bash
mvn -q -pl auth-poc-app -Dtest=InMemoryOrganizationAdaptersConfigTest test
```
Expected: échec de compilation (`InMemoryOrganizationAdaptersConfig` n'existe pas).

- [ ] **Step 3: Écrire la config**

```java
package yowyob.comops.api.pocapp.config;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.SignUpContextDirectory;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccess;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccessDirectory;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlement;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlementDirectory;

/**
 * Adaptateurs de sortie in-memory remplaçant, pour le slice POC, les adaptateurs
 * réels (bootstrap -> organization-core). La logique d'auth reste réelle ; seules
 * les données d'organisation sont simulées et seedées.
 */
@Configuration
public class InMemoryOrganizationAdaptersConfig {

    public static final UUID DEMO_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DEMO_ORG_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final String DEMO_ORG_CODE = "YOWAUTH-DEMO";

    @Bean
    OrganizationServiceRuntimeEntitlementDirectory inMemoryEntitlementDirectory() {
        return (tenantId, organizationId, serviceCode) ->
                Mono.just(new OrganizationServiceRuntimeEntitlement(serviceCode, true, null, null));
    }

    @Bean
    UserOrganizationAccessDirectory inMemoryUserOrganizationAccessDirectory() {
        return (tenantId, userId) -> Flux.just(new UserOrganizationAccess(
                DEMO_ORG_ID, DEMO_ORG_CODE, "YowAuth Demo", "YowAuth Demo Organization",
                List.of("SALES", "IAM")));
    }

    @Bean
    SignUpContextDirectory inMemorySignUpContextDirectory() {
        return organizationCode -> {
            if (organizationCode == null || !DEMO_ORG_CODE.equalsIgnoreCase(organizationCode)) {
                return Flux.empty();
            }
            return Flux.just(new SignUpContextDirectory.SignUpContext(
                    DEMO_ORG_ID.toString(), DEMO_TENANT_ID, DEMO_ORG_ID, DEMO_ORG_CODE,
                    "YowAuth Demo Organization", "COMPANY"));
        };
    }
}
```

- [ ] **Step 4: Lancer le test pour vérifier qu'il passe**

```bash
mvn -q -pl auth-poc-app -Dtest=InMemoryOrganizationAdaptersConfigTest test
```
Expected: `BUILD SUCCESS`, 4 tests verts.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: adaptateurs in-memory seedes pour les ports organisation"
```

---

### Task 4: Boot du contexte — résolution itérative des beans manquants

**Files:**
- Modify (si besoin) : `auth-poc-app/src/main/java/yowyob/comops/api/pocapp/config/` (ajout de stubs au fur et à mesure)
- Modify (si besoin) : `auth-poc-app/src/main/resources/application.yml`
- Test: `auth-poc-app/src/test/java/yowyob/comops/api/pocapp/AuthPocApplicationContextTest.java`

**Interfaces:**
- Produces: contexte Spring qui démarre proprement ; `/actuator/health` = `UP`.

- [ ] **Step 1: Écrire un test de chargement de contexte**

```java
package yowyob.comops.api.pocapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthPocApplicationContextTest {
    @Test
    void contextLoads() {
        // Réussit si tous les beans se câblent sans erreur.
    }
}
```

- [ ] **Step 2: Lancer le test — il révèle le premier bean manquant**

```bash
mvn -q -pl auth-poc-app -Dtest=AuthPocApplicationContextTest test
```
Expected (probable au 1er passage) : `UnsatisfiedDependencyException` / `NoSuchBeanDefinitionException` nommant un port sans implémentation dans le slice.

- [ ] **Step 3: Résoudre le bean manquant (boucle)**

Pour chaque bean manquant signalé :
1. Identifier le port (`…application.port.out.XxxDirectory` / `…port.out.XxxPort`).
2. Localiser sa **vraie** implémentation dans le fork :
   ```bash
   grep -rl "implements .*<NomDuPort>" "c:/Users/Tchoutzine/Documents/GitHub/KSM_Kernel_Layer"
   ```
3. Si l'implémentation est dans un core déjà du slice → vérifier pourquoi elle n'est pas scannée (package). Si elle est dans **bootstrap** ou un autre core → ajouter un **bean stub in-memory** dans un nouveau fichier `…/pocapp/config/InMemory<Nom>Config.java` reproduisant le comportement minimal (retour neutre/vide ou seedé).
4. Relancer le test (Step 2). Répéter jusqu'à `BUILD SUCCESS`.

> Beans déjà connus et traités en Task 3 : `OrganizationServiceRuntimeEntitlementDirectory`, `UserOrganizationAccessDirectory`, `SignUpContextDirectory`. `ReactivePermissionResolver` est fourni par roles-core (présent). Tout autre port révélé ici suit la même recette.

- [ ] **Step 4: Vérifier le démarrage réel + health**

```bash
mvn -q -pl auth-poc-app -am -DskipTests spring-boot:run
```
Dans un second terminal :
```bash
curl -s http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"...}`. Arrêter l'app (Ctrl+C) ensuite.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: contexte auth-poc-app demarre proprement (beans resolus)"
```

---

### Task 5: Smoke-test local du flux auth + gel des chemins/headers

**Files:**
- Create: `poc/smoke.sh`
- Create: `poc/smoke.ps1`

**Interfaces:**
- Consumes: app locale sur `http://localhost:8080`, client `poc-client` / secret `poc-secret-key`.
- Produces: scripts figeant les chemins exacts, en-têtes et service autorisé.

- [ ] **Step 1: Démarrer l'app**

```bash
mvn -q -pl auth-poc-app -am -DskipTests spring-boot:run
```

- [ ] **Step 2: Sonder les endpoints publics pour figer les chemins**

```bash
curl -s http://localhost:8080/v3/api-docs | python -c "import sys,json;d=json.load(sys.stdin);print('\n'.join(sorted(d['paths'])))"
curl -s http://localhost:8080/.well-known/jwks.json
```
Expected: liste des routes (`/api/auth/...`, etc.) + un JWKS contenant une clé RSA. Repérer les chemins exacts de register, login, token exchange, userinfo, et le préfixe OIDC.

- [ ] **Step 3: Écrire `poc/smoke.sh`**

```bash
#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
CLIENT_ID="${CLIENT_ID:-poc-client}"
API_KEY="${API_KEY:-poc-secret-key}"
H=(-H "Content-Type: application/json" -H "X-Client-Id: ${CLIENT_ID}" -H "X-Api-Key: ${API_KEY}")
TENANT="00000000-0000-0000-0000-000000000001"
U="poc_$(date +%s)"

echo "== 1. register =="
curl -s "${H[@]}" -H "X-Tenant-Id: ${TENANT}" -X POST "${BASE_URL}/api/auth/register" \
  -d "{\"username\":\"${U}\",\"email\":\"${U}@demo.io\",\"password\":\"P@ssw0rd!2024\"}" | tee /tmp/reg.json; echo

echo "== 2. login =="
curl -s "${H[@]}" -H "X-Tenant-Id: ${TENANT}" -X POST "${BASE_URL}/api/auth/login" \
  -d "{\"identifier\":\"${U}\",\"password\":\"P@ssw0rd!2024\"}" | tee /tmp/login.json; echo

echo "== 3. JWKS =="
curl -s "${BASE_URL}/.well-known/jwks.json"; echo

echo "== 4. users/me (bearer issu du login) =="
TOKEN=$(python -c "import json;print(json.load(open('/tmp/login.json')).get('data',{}).get('accessToken') or json.load(open('/tmp/login.json')).get('data',{}).get('sessionToken',''))")
curl -s "${H[@]}" -H "X-Tenant-Id: ${TENANT}" -H "Authorization: Bearer ${TOKEN}" \
  "${BASE_URL}/api/users/me"; echo
```

> Les chemins/champs (`identifier`, `accessToken`/`sessionToken`, route token exchange OIDC) sont ajustés selon ce qu'a révélé le Step 2 avant de figer le script.

- [ ] **Step 4: Écrire `poc/smoke.ps1` (équivalent PowerShell)**

```powershell
param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$ClientId = "poc-client",
  [string]$ApiKey = "poc-secret-key"
)
$tenant = "00000000-0000-0000-0000-000000000001"
$u = "poc_$([int][double]::Parse((Get-Date -UFormat %s)))"
$headers = @{ "Content-Type"="application/json"; "X-Client-Id"=$ClientId; "X-Api-Key"=$ApiKey; "X-Tenant-Id"=$tenant }

Write-Host "== 1. register =="
$reg = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method Post -Headers $headers `
  -Body (@{ username=$u; email="$u@demo.io"; password="P@ssw0rd!2024" } | ConvertTo-Json)
$reg | ConvertTo-Json -Depth 6

Write-Host "== 2. login =="
$login = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Headers $headers `
  -Body (@{ identifier=$u; password="P@ssw0rd!2024" } | ConvertTo-Json)
$login | ConvertTo-Json -Depth 6

Write-Host "== 3. JWKS =="
Invoke-RestMethod -Uri "$BaseUrl/.well-known/jwks.json" | ConvertTo-Json -Depth 6

Write-Host "== 4. users/me =="
$token = $login.data.accessToken; if (-not $token) { $token = $login.data.sessionToken }
$h2 = $headers.Clone(); $h2["Authorization"] = "Bearer $token"
Invoke-RestMethod -Uri "$BaseUrl/api/users/me" -Headers $h2 | ConvertTo-Json -Depth 6
```

- [ ] **Step 5: Exécuter le smoke et vérifier**

```bash
bash poc/smoke.sh
```
Expected: étapes 1→4 renvoient du JSON de succès (compte créé, token émis, JWKS avec clé RSA, profil + organisation seedée). Si un appel `/api/**` renvoie 401/403 « service non autorisé », ajouter le `serviceCode` requis à `iwm.security.client-applications.bootstrap.allowed-services` dans `application.yml`, relancer, re-tester.

- [ ] **Step 6: Commit**

```bash
git add -A poc/ auth-poc-app/src/main/resources/application.yml
git commit -m "feat: scripts smoke + gel des chemins/headers/services auth"
```

---

### Task 6: Dockerisation + run conteneur local

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

**Interfaces:**
- Produces: image Docker exposant le port 8080, health `UP`.

- [ ] **Step 1: Créer `.dockerignore`**

```
**/target
**/node_modules
**/.next
.git
poc/web/.env*
```

- [ ] **Step 2: Créer `Dockerfile`**

```dockerfile
# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl auth-poc-app -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home app
COPY --from=build /workspace/auth-poc-app/target/auth-poc-app-0.1.0-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
ENTRYPOINT ["sh","-c","java -jar /app/app.jar"]
```

- [ ] **Step 3: Build + run local**

```bash
docker build -t yowauth-poc:local .
docker run --rm -p 8080:8080 \
  -e IWM_MANAGEMENT_API_KEY=local-management-key-123456 \
  -e IWM_POC_CLIENT_SECRET=poc-secret-key \
  yowauth-poc:local
```
Dans un autre terminal :
```bash
curl -s http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"...}`. Puis `bash poc/smoke.sh` doit passer contre le conteneur. Arrêter le conteneur.

- [ ] **Step 4: Commit**

```bash
git add Dockerfile .dockerignore
git commit -m "feat: Dockerfile du slice auth (build + run in-memory)"
```

---

### Task 7: Déploiement Render

**Files:**
- Create: `render.yaml`

**Interfaces:**
- Consumes: repo GitHub `yowauth-poc` (remote `origin`), image Docker (Task 6).
- Produces: URL HTTPS publique Render avec `/actuator/health` = `UP`.

- [ ] **Step 1: Créer `render.yaml`**

```yaml
services:
  - type: web
    name: yowauth-poc
    runtime: docker
    plan: free
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health
    envVars:
      - key: IWM_MANAGEMENT_API_KEY
        generateValue: true
      - key: IWM_POC_CLIENT_SECRET
        sync: false
      - key: IWM_CORS_ALLOWED_ORIGINS
        sync: false
      - key: JAVA_TOOL_OPTIONS
        value: "-XX:MaxRAMPercentage=65"
```

- [ ] **Step 2: Pousser sur GitHub**

```bash
git push -u origin main
```
(Le remote `origin` aura été ajouté par l'utilisateur : `https://github.com/<user>/yowauth-poc.git`.)

- [ ] **Step 3: Créer le service sur Render**

Sur dashboard.render.com → **New** → **Blueprint** → sélectionner le repo `yowauth-poc` → Render lit `render.yaml`. Renseigner les env `sync:false` : `IWM_POC_CLIENT_SECRET` (= `poc-secret-key` ou un secret choisi), `IWM_CORS_ALLOWED_ORIGINS` (URL Vercel, ajoutée à Task 8). Lancer le déploiement.

- [ ] **Step 4: Vérifier en ligne**

```bash
curl -s https://<nom-render>.onrender.com/actuator/health
BASE_URL=https://<nom-render>.onrender.com API_KEY=<secret> bash poc/smoke.sh
```
Expected: health `UP` (après ~quelques min de build + cold start), smoke 1→4 verts.

> Risque RAM (free tier = 512 Mo) : si le service crash (OOM), baisser `MaxRAMPercentage` à 60, ou basculer sur **Koyeb** (même fichier Docker). Documenter le choix retenu.

- [ ] **Step 5: Commit**

```bash
git add render.yaml
git commit -m "feat: blueprint Render (deploiement gratuit du slice auth)"
git push
```

---

### Task 8: Frontend POC Next.js + déploiement Vercel

**Files:**
- Create: `poc/web/` (app Next.js)
- Create/Modify: `poc/web/app/page.tsx`, `poc/web/.env.example`

**Interfaces:**
- Consumes: API Render (`NEXT_PUBLIC_API_URL`), client `poc-client` + secret (côté démo, exposé volontairement pour le POC).
- Produces: URL Vercel publique appelant l'API en live.

- [ ] **Step 1: Initialiser l'app Next.js**

```bash
cd "c:/Users/Tchoutzine/Documents/GitHub/yowauth-poc/poc"
npx create-next-app@latest web --ts --app --eslint --no-tailwind --no-src-dir --import-alias "@/*" --use-npm
```
Expected: dossier `poc/web` créé, `npm run dev` démarre sur :3000.

- [ ] **Step 2: Créer `poc/web/.env.example`**

```
NEXT_PUBLIC_API_URL=https://<nom-render>.onrender.com
NEXT_PUBLIC_CLIENT_ID=poc-client
NEXT_PUBLIC_API_KEY=poc-secret-key
NEXT_PUBLIC_TENANT_ID=00000000-0000-0000-0000-000000000001
```

- [ ] **Step 3: Remplacer `poc/web/app/page.tsx` par la page de démo**

```tsx
"use client";
import { useState } from "react";

const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const CLIENT_ID = process.env.NEXT_PUBLIC_CLIENT_ID ?? "poc-client";
const API_KEY = process.env.NEXT_PUBLIC_API_KEY ?? "poc-secret-key";
const TENANT = process.env.NEXT_PUBLIC_TENANT_ID ?? "00000000-0000-0000-0000-000000000001";

const headers = {
  "Content-Type": "application/json",
  "X-Client-Id": CLIENT_ID,
  "X-Api-Key": API_KEY,
  "X-Tenant-Id": TENANT,
};

function decodeJwt(token: string) {
  try {
    const [, payload] = token.split(".");
    return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
  } catch {
    return null;
  }
}

export default function Home() {
  const [user] = useState(() => `poc_${Date.now()}`);
  const [log, setLog] = useState<string>("");
  const [token, setToken] = useState<string>("");

  const print = (label: string, data: unknown) =>
    setLog((p) => `${p}\n# ${label}\n${JSON.stringify(data, null, 2)}\n`);

  async function run() {
    setLog("");
    const reg = await fetch(`${API}/api/auth/register`, {
      method: "POST", headers,
      body: JSON.stringify({ username: user, email: `${user}@demo.io`, password: "P@ssw0rd!2024" }),
    }).then((r) => r.json());
    print("register", reg);

    const login = await fetch(`${API}/api/auth/login`, {
      method: "POST", headers,
      body: JSON.stringify({ identifier: user, password: "P@ssw0rd!2024" }),
    }).then((r) => r.json());
    print("login", login);

    const t = login?.data?.accessToken ?? login?.data?.sessionToken ?? "";
    setToken(t);

    const jwks = await fetch(`${API}/.well-known/jwks.json`).then((r) => r.json());
    print("jwks", jwks);

    const me = await fetch(`${API}/api/users/me`, {
      headers: { ...headers, Authorization: `Bearer ${t}` },
    }).then((r) => r.json());
    print("users/me", me);
  }

  return (
    <main style={{ fontFamily: "system-ui", maxWidth: 900, margin: "2rem auto", padding: "0 1rem" }}>
      <h1>YowAuth POC</h1>
      <p>API: <code>{API}</code></p>
      <button onClick={run} style={{ padding: "0.6rem 1.2rem", fontSize: 16 }}>
        Lancer le flux register → login → JWKS → users/me
      </button>
      {token && (
        <section>
          <h3>JWT décodé</h3>
          <pre>{JSON.stringify(decodeJwt(token), null, 2)}</pre>
        </section>
      )}
      <h3>Réponses</h3>
      <pre style={{ background: "#111", color: "#0f0", padding: "1rem", overflow: "auto" }}>{log}</pre>
    </main>
  );
}
```

> Ajuster les chemins/champs pour coller à ce que le smoke (Task 5) a figé.

- [ ] **Step 4: Tester en local contre l'API Render**

```bash
cd poc/web
cp .env.example .env.local   # éditer NEXT_PUBLIC_API_URL = URL Render
npm run dev
```
Ouvrir http://localhost:3000, cliquer le bouton. Expected: les 4 réponses + le JWT décodé s'affichent. Si CORS bloque, ajouter `http://localhost:3000` (déjà par défaut) et l'URL Vercel à `IWM_CORS_ALLOWED_ORIGINS` côté Render.

- [ ] **Step 5: Déployer sur Vercel**

Sur vercel.com → **Add New Project** → importer le repo `yowauth-poc` → **Root Directory** = `poc/web` → renseigner les variables `NEXT_PUBLIC_*` → Deploy. Récupérer l'URL Vercel et l'ajouter à `IWM_CORS_ALLOWED_ORIGINS` sur Render (redeploy Render).

- [ ] **Step 6: Vérifier la démo en ligne**

Ouvrir l'URL Vercel, cliquer le bouton. Expected: flux complet vert contre l'API Render.

- [ ] **Step 7: Commit**

```bash
cd "c:/Users/Tchoutzine/Documents/GitHub/yowauth-poc"
git add poc/web
git commit -m "feat: POC Next.js (demo live du flux auth) + config Vercel"
git push
```

---

### Task 9: README de démo

**Files:**
- Create: `README.md`

**Interfaces:**
- Produces: documentation de soutenance.

- [ ] **Step 1: Écrire `README.md`**

Contenu (sections, avec les commandes réelles des tâches précédentes) :
- **Quoi** : POC du module YowAuth (auth-core) extrait en slice isolé.
- **Périmètre & isolation** : repo séparé du fork `KSM_Kernel_Layer` ; 5 cores copiés + `auth-poc-app` ; organisation simulée par adaptateurs in-memory.
- **Lancer en local** : `mvn -pl auth-poc-app -am -DskipTests spring-boot:run` puis `bash poc/smoke.sh`.
- **Frontend** : `cd poc/web && npm run dev`.
- **Déploiement** : Render (`render.yaml`) + Vercel (`poc/web`).
- **Scénario de soutenance** : pas-à-pas (ouvrir l'URL Vercel → cliquer → commenter register/login/JWT RS256/JWKS/organisations).
- **Limites connues** : données en mémoire (reset au redémarrage), clé RSA régénérée au cold start, flux MFA/OTP/reset hors POC.

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: README de demo du POC YowAuth"
git push
```

---

## Self-Review

**Spec coverage :**
- §0 Isolation → Global Constraints + Task 1/7 (repo séparé, push origin) ✓
- §1 Critères de succès → Task 5 (smoke local), Task 7 (smoke en ligne), Task 8 (démo Vercel) ✓
- §2 Beans requis → Task 3 (3 adaptateurs) + Task 4 (résolution itérative, roles-core inclus en Task 1) ✓
- §3 Périmètre slice → Task 1 (copie 5 cores + pom trimé) ✓
- §4 auth-poc-app → Task 2 + Task 3 ✓
- §5 application.yml → Task 2 Step 3 ✓
- §6 Render → Task 6 (Docker) + Task 7 (render.yaml) ✓
- §7 POC → Task 5 (scripts) + Task 8 (Next.js/Vercel) ✓
- §8 README → Task 9 ✓

**Placeholder scan :** les `<nom-render>` / `<user>` / `<secret>` sont des valeurs d'environnement à fournir par l'utilisateur au moment du déploiement (pas du code à coder) ; les `> Note`/`> Si` sont des instructions d'ajustement conditionnel explicites, pas des TODO ouverts.

**Type consistency :** types des records (`OrganizationServiceRuntimeEntitlement(String,boolean,Long,Long)`, `UserOrganizationAccess(UUID,String,String,String,List<String>)`, `SignUpContext(String,UUID,UUID,String,String,String)`) et signatures des ports (`resolveRuntimeEntitlement`, `listUserOrganizations`, `findByOrganizationCode`) cohérents entre Task 3 et les usages. Constantes `DEMO_TENANT_ID/DEMO_ORG_ID/DEMO_ORG_CODE` réutilisées en Task 5 et 8.
