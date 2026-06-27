# RT-comops-kernel-core : Le Cœur du Système (KSM Kernel)

Ce module constitue la colonne vertébrale (Shared Kernel) de l'ERP. Il définit les protocoles de communication, les mécanismes de multi-tenancy et la fiabilité des échanges entre les différents modules fonctionnels (Accounting, Billing, Inventory, etc.).

## 🏗️ Responsabilités Architecturales

### 1. Communication Événementielle (Event-Driven)
Le Kernel centralise le modèle **`BusinessEvent`**. C'est le contrat universel qui permet aux modules de communiquer sans se connaître (couplage faible).
*   **Reliability** : Implémentation du pattern **Transactional Outbox** (`OutboxEvent`) pour garantir qu'un événement n'est jamais perdu en cas de crash après une transaction.
*   **Audit** : Enregistrement centralisé de toutes les actions critiques via `SystemAuditEntry`.

### 2. Isolation Multi-Tenant
Le Kernel gère la sécurité et l'isolation des données via le **`TenantContext`**.
*   Propagation automatique du `tenantId` et `organizationId` à travers le contexte réactif (Project Reactor).
*   Garantie que chaque requête SQL ou appel API est filtré par le contexte de l'organisation.

### 3. Modèles de Projection (Event Sourcing)
Fournit les structures de base pour les **`DomainEventProjection`**, permettant de construire des vues de lecture optimisées à partir des flux d'événements.

---

## 📁 Organisation des Paquetages (Hexagonal)

*   **`domain`** : Entités racines (BusinessEvent, Outbox, TenantContext) et invariants système.
*   **`application.port`** : Interfaces de services (In) et d'infrastructure comme la publication d'événements (Out).
*   **`application.service`** : Orchestration de la persistance Outbox et de la propagation du contexte.
*   **`adapter.in.web`** : Contrôleurs pour la gestion technique du Kernel (Audit, Status).
*   **`adapter.out.persistence`** : Adaptateurs R2DBC réactifs pour l'Outbox et l'Audit.
*   **`config`** : Configuration Spring Boot (Auto-configurations du Kernel).

## 🚀 Utilisation par les autres modules
Tous les modules métiers (ex: `RT-comops-accounting-core`) dépendent de ce module pour :
1. Publier des événements métier.
2. Accéder aux informations de l'utilisateur courant et de son organisation.
3. Garantir la consistance transactionnelle de leurs événements.

