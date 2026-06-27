# iwm-file-core

Module de stockage et de gouvernance documentaire. Gère l'upload, le stockage (disque local ou
S3), les métadonnées, les liens documentaires, les revues, et **l'analyse de contenu via un service
externe** avant mise à disposition des fichiers.

## Package layout
- `domain.model` : modèles et invariants (`StoredFile`, `FileAnalysisStatus`, `FileBusinessEventType`…)
- `application.port.in` : cas d'usage entrants (`StoreFileUseCase`, `GetStoredFileUseCase`, `ApplyFileAnalysisVerdictUseCase`)
- `application.port.out` : ports sortants (`StoredFileRepository`, `FileBinaryStorage`…)
- `application.service` : orchestration (`FileApplicationService`…)
- `adapter.in.web` : exposition HTTP réactive (`FileController`…)
- `adapter.in.messaging` : consommation d'événements métier (`FileAnalysisVerdictConsumer`)
- `adapter.out.persistence` : adaptateurs de persistance R2DBC + in-memory
- `adapter.out.storage` : stockage binaire (`LocalDiskFileBinaryStorage`, `S3FileBinaryStorage`)
- `config` : assemblage du module

---

## Analyse de contenu externe (quarantaine asynchrone)

Tout fichier uploadé est mis en **quarantaine** (`PENDING`) puis soumis à un **service d'analyse
externe** (hors de ce dépôt). Le fichier n'est téléchargeable qu'une fois **accepté**. Le flux est
**asynchrone et non bloquant** : il s'appuie sur l'infrastructure outbox/BusinessEvent du projet
(Kafka), comme le module notification.

### Cycle de vie d'un fichier

| Statut | Signification | Téléchargement `GET /api/files/{id}` |
|--------|---------------|--------------------------------------|
| `PENDING` | Uploadé, en attente du verdict d'analyse | Bloqué (`423 LOCKED`) |
| `ACCEPTED` | Validé par le service d'analyse | Autorisé |
| `REJECTED` | Rejeté — gardé en quarantaine pour revue admin | Bloqué (`423 LOCKED`) |

### Séquence

```
Client                file-core                         Service d'analyse (externe)
  │  POST /api/files      │                                        │
  ├──────────────────────►│  écrit le binaire + StoredFile PENDING │
  │                       │  publie FILE_ANALYSIS_REQUESTED ───────►│  (consomme, récupère via storagePath)
  │  201 (PENDING)        │                                        │  analyse…
  │◄──────────────────────┤                                        │
  │                       │◄─── FILE_ANALYSIS_COMPLETED ───────────┤  publie le verdict
  │                       │  FileAnalysisVerdictConsumer            │
  │                       │   → markAccepted() / markRejected()     │
  │  GET /api/files/{id}  │                                        │
  ├──────────────────────►│  ACCEPTED → 200 + binaire              │
  │                       │  PENDING/REJECTED → 423 LOCKED          │
```

---

## Contrat d'intégration Kafka (pour l'équipe du service externe)

Les deux échanges passent par le **même topic Kafka** que tous les événements métier du projet.

| Paramètre | Valeur par défaut | Variable d'env |
|-----------|-------------------|----------------|
| Topic | `iwm.events.business` | `IWM_OUTBOX_KAFKA_TOPIC_PREFIX` |
| Pattern consommé par iwm | `^iwm\.events\..+$` | `IWM_OUTBOX_KAFKA_TOPIC_PATTERN` |
| Clé du message (`key`) | `aggregateId` = `fileId` (UUID en string) | — |
| Sérialisation | JSON (String) | — |

> Si `IWM_OUTBOX_KAFKA_PER_AGGREGATE_TOPIC=true`, le topic devient
> `iwm.events.business.stored-file`. Par défaut c'est `false` → topic unique `iwm.events.business`.

### 1. Événement émis par file-core → à CONSOMMER par le service externe

`eventType = FILE_ANALYSIS_REQUESTED`

```json
{
  "id": "3f1a...",                       // id outbox (UUID)
  "tenantId": "8c2b...",
  "organizationId": "a91e...",
  "eventType": "FILE_ANALYSIS_REQUESTED",
  "aggregateType": "STORED_FILE",
  "aggregateId": "<fileId>",
  "occurredAt": "2026-06-07T10:15:30Z",
  "payload": {
    "fileId": "<fileId>",
    "storagePath": "8c2b.../d4e9.../facture.pdf",
    "fileName": "facture.pdf",
    "contentType": "application/pdf",
    "size": 24576
  },
  "status": "PUBLISHED",
  "attemptCount": 1,
  "publishedAt": "2026-06-07T10:15:30Z"
}
```

Le service récupère le binaire à partir de `storagePath` (même backend de stockage : disque local
partagé ou bucket S3 — voir section Configuration), l'analyse, puis répond avec l'événement ci-dessous.

### 2. Événement à PUBLIER par le service externe → consommé par file-core

`eventType = FILE_ANALYSIS_COMPLETED`, topic `iwm.events.business`, `key = fileId`.

```json
{
  "id": "b7d2c0a4-0000-4000-8000-000000000001",   // UUID quelconque, unique
  "tenantId": "8c2b...",                            // = celui reçu dans la demande
  "organizationId": "a91e...",                      // = celui reçu dans la demande
  "eventType": "FILE_ANALYSIS_COMPLETED",
  "aggregateType": "STORED_FILE",
  "aggregateId": "<fileId>",                        // = fileId du fichier analysé
  "occurredAt": "2026-06-07T10:16:05Z",
  "payload": {
    "fileId": "<fileId>",
    "verdict": "ACCEPTED",                          // ou "REJECTED"
    "reason": ""                                     // motif si REJECTED
  },
  "status": "PUBLISHED",
  "attemptCount": 1,
  "publishedAt": "2026-06-07T10:16:05Z"
}
```

**Champs obligatoires** (sinon le message part en dead-letter `iwm.events.dead-letter`) :
`tenantId`, `eventType`, `aggregateType`, `aggregateId`, `occurredAt`, `status` (valeur d'enum
valide, recommandé `PUBLISHED` avec `publishedAt` renseigné), `attemptCount` (entier ≥ 0), `payload`.
Les autres champs (`lastError`, `nextAttemptAt`, `actorUserId`, `clientApplicationId`, `requestId`…)
peuvent être `null`.

Le `payload.verdict` accepte `ACCEPTED` ou `REJECTED` (insensible à la casse). À défaut de `verdict`,
le champ `analysisStatus` est aussi accepté. Le `fileId` du payload prime ; en son absence,
`aggregateId` est utilisé.

### Exemple de production (kafka-console-producer)

```bash
echo '<fileId>:{"id":"b7d2c0a4-0000-4000-8000-000000000001","tenantId":"8c2b...","organizationId":"a91e...","eventType":"FILE_ANALYSIS_COMPLETED","aggregateType":"STORED_FILE","aggregateId":"<fileId>","occurredAt":"2026-06-07T10:16:05Z","payload":{"fileId":"<fileId>","verdict":"REJECTED","reason":"malware detected"},"status":"PUBLISHED","attemptCount":1,"publishedAt":"2026-06-07T10:16:05Z"}' \
| kafka-console-producer --bootstrap-server <kafka> --topic iwm.events.business \
    --property "parse.key=true" --property "key.separator=:"
```

---

## Endpoints HTTP

| Méthode | Chemin | Description | Accès |
|---------|--------|-------------|-------|
| `POST` | `/api/files` | Upload (multipart `file`). Réponse `201`, statut `PENDING`. | contexte utilisateur |
| `GET` | `/api/files/{id}` | Télécharge le binaire. `423` si non `ACCEPTED`. | contexte utilisateur |
| `GET` | `/api/files/{id}/metadata` | Métadonnées + `analysisStatus` + `analysisReason`. | contexte utilisateur |
| `GET` | `/api/files/{id}/review` | Télécharge un fichier en quarantaine (revue). | `canReadAdministrativeAudit` |

---

## Événements métier (`FileBusinessEventType`)

| Type | Sens | Payload |
|------|------|---------|
| `FILE_UPLOADED` | émis | `fileName, contentType, size` |
| `FILE_ANALYSIS_REQUESTED` | émis → service externe | `fileId, storagePath, fileName, contentType, size` |
| `FILE_ANALYSIS_COMPLETED` | consommé ← service externe | `fileId, verdict, reason` |
| `FILE_ANALYSIS_ACCEPTED` | émis (après verdict) | `fileId, analysisStatus, reason` |
| `FILE_ANALYSIS_REJECTED` | émis (après verdict) | `fileId, analysisStatus, reason` |

---

## Configuration (`iwm.file.storage.*`)

| Propriété | Défaut | Description |
|-----------|--------|-------------|
| `backend` | `local` | `local` ou `s3` |
| `root-path` | `./data/files` | Racine du stockage local |
| `max-file-size-bytes` | `10485760` (10 Mo) | Taille max acceptée |
| `allowed-content-types` | _(vide = tous)_ | Liste blanche de types MIME |
| `s3.endpoint / region / bucket / access-key / secret-key / prefix` | — | Paramètres S3 |

> Pour que le service externe lise les binaires, il doit accéder au **même stockage** : soit le
> bucket S3 (`backend=s3`, recommandé en prod), soit un volume disque partagé (`backend=local`).

## Migration base de données

Colonnes d'analyse ajoutées à `file_core.stored_file` :
`analysis_status` (défaut `PENDING`), `analysis_reason`, `analyzed_at`.
Migration `db/r2dbc/V76__file_analysis_quarantine.sql`, enveloppée par la release Liquibase
`releases/077-file-analysis-quarantine.yaml`.

## Tests
- `StoredFileTest` — invariants du cycle d'analyse du domaine
- `FileApplicationServiceVerdictTest` — application du verdict (ACCEPTED / REJECTED / invalide)
- `FileAnalysisVerdictConsumerTest` — mapping de l'événement Kafka vers le cas d'usage
