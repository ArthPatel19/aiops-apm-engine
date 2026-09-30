# AI-Ops APM Engine

A low-cost, AI-driven Application Performance Monitoring (APM) operations engine. It ingests raw error logs, redacts sensitive data, groups related errors into incidents, and uses an LLM (Google Gemini) to produce a root-cause diagnosis, remediation steps, and ready-to-paste Jira/Slack payloads — while keeping AI token usage to a minimum.

This repository contains the complete assessment implementation:

- **Backend:** Java 21, Spring Boot, MySQL
- **Frontend:** Flutter (Web + Mobile), BLoC, clean architecture
- **AI provider:** Google Gemini (`gemini-3.5-flash-lite`), with a deterministic zero-cost fallback if the AI is unavailable

The backend and frontend are kept as two separate applications inside the same Git repository so they can be developed and run independently.

---

## Table of Contents

- [Architecture](#architecture)
- [Why this is token-efficient](#why-this-is-token-efficient)
- [Project structure](#project-structure)
- [Backend setup](#backend-setup)
- [Frontend setup](#frontend-setup)
- [API reference](#api-reference)
- [Testing](#testing)
- [Known limitations](#known-limitations)

---

## Architecture

```text
                 POST /api/logs (raw logs)
                         │
                         ▼
                Validation & deduplication
                         │
                         ▼
          PII / secret sanitization (passwords, tokens,
           JWTs, emails, AWS keys → [REDACTED_SECRET])
                         │
                         ▼
                   Saved to MySQL
                         │
                         ▼
          Log pruning (normalize message, cap to
           3 relevant stack frames, dedupe frames)
                         │
                         ▼
         Root-cause classification + fingerprinting
                         │
                         ▼
        Clustering — related logs join ONE incident
        (e.g. 3 raw logs → 2 incidents, in the sample data)
                         │
               ┌─────────┴─────────┐
               │                   │
         New incident         Existing incident
               │                   │
      Severity + health      Severity + health
        score updated          score updated
               │                   │
               ▼                   ▼
     AI diagnosis (Gemini,     No AI call —
      ONE call, ever, per      logCount just
      incident) with a         increments
      rule-based fallback
               │
               ▼
       Stored on the incident
               │
               ▼
    GET /api/incidents, /api/health, /api/logs
               │
               ▼
          Flutter dashboard
```

---

## Why this is token-efficient

The assignment's core requirement is minimizing LLM token usage without losing diagnostic quality. This project does that in three layers, and we measure each one honestly rather than presenting a single flattering number:

1. **Fewer calls, not smaller calls, is the biggest lever.** The AI is called exactly once per *incident*, never once per *log*, and never again once an incident exists. This means multiple related logs can be represented by one incident-level AI diagnosis.

2. **A hard ceiling on what any single call can cost.** Regardless of how long or repetitive a raw stack trace is, pruning caps it to 3 relevant frames (collapsing duplicates) and normalizes/truncates the message. This bounds worst-case diagnostic context size.

3. **Measured, apples-to-apples payload reduction.** Each incident stores four numbers, computed so the comparison is fair:
   - `sample_raw_tokens` — the representative log's raw diagnostic payload (type, status, feature, API, full message, full stack trace)
   - `compacted_context_tokens` — the same fields after normalization, truncation, and frame-capping
   - `estimated_raw_tokens` — the cumulative raw volume across every log in the incident (grows with `logCount`, unlike the actual AI cost)
   - `ai_prompt_tokens` — Gemini's own reported `usageMetadata.promptTokenCount` for the one real call made

We deliberately do **not** claim `ai_prompt_tokens` is "lower than" an estimate — they measure different things (an estimate vs. a real tokenizer count including instructions and schema overhead). The fair, provable preprocessing comparison is `sample_raw_tokens` vs. `compacted_context_tokens`.

---

## Project structure

```text
AI-Ops-APM-Engine/
├── aiops-apm/                    Spring Boot REST API
│   ├── .mvn/
│   │   └── wrapper/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── .gitignore
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   ├── sample-logs.json
│   └── README.md
│
├── aiops-dashboard/              Flutter dashboard
│   ├── android/
│   ├── ios/
│   ├── lib/
│   ├── linux/
│   ├── macos/
│   ├── test/
│   ├── web/
│   ├── windows/
│   ├── .gitignore
│   ├── .metadata
│   ├── README.md
│   ├── analysis_options.yaml
│   ├── pubspec.lock
│   └── pubspec.yaml
│
├── .gitignore
└── README.md
```

### Backend architecture

The backend follows a layered Spring Boot structure:

```text
src/main/java/com/aiops/aiops_apm/
├── controller/      REST endpoints
├── service/         sanitizer, pruning, clustering, severity,
│                    health, AI and business logic
├── repository/      Spring Data JPA
├── entity/          ApmLog, Incident
├── dto/             request/response records
├── config/          Gemini properties, CORS and configuration
└── exception/       global error handling
```

### Frontend architecture

The Flutter dashboard uses BLoC and clean architecture. The dashboard is responsible for presenting system health, ingested logs, grouped incidents, AI diagnosis, remediation steps, and copy-pasteable Jira/Slack payloads.

Detailed application documentation is available in:

- [`aiops-apm/README.md`](aiops-apm/README.md)
- [`aiops-dashboard/README.md`](aiops-dashboard/README.md)

---

## Backend setup

**Requirements:** Java 21, Maven (or the included Maven Wrapper), MySQL 8.x, and a Google Gemini API key if live Gemini diagnosis is required.

### 1. Create the database

```sql
CREATE DATABASE aiops_apm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'aiops'@'localhost' IDENTIFIED BY 'your_password_here';
GRANT ALL PRIVILEGES ON aiops_apm.* TO 'aiops'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Set environment variables

Never commit real credentials.

On Windows PowerShell:

```powershell
setx DB_USER "aiops"
setx DB_PASSWORD "your_password_here"
setx GEMINI_API_KEY "your_gemini_key"
setx GEMINI_MODEL "gemini-3.5-flash-lite"
```

After using `setx`, close and reopen the terminal/IDE and restart the backend so the new environment variables are available.

### 3. Run the backend

From the repository root:

```powershell
cd aiops-apm
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The server starts on:

```text
http://localhost:8080
```

### 4. Try the sample data

From `aiops-apm/`:

```powershell
curl -X POST "http://localhost:8080/api/logs" `
  -H "Content-Type: application/json" `
  --data-binary "@sample-logs.json"
```

Retrieve incidents:

```powershell
curl "http://localhost:8080/api/incidents"
```

If no `GEMINI_API_KEY` is set, the application still works using the deterministic rule-based fallback instead of calling Gemini.

---

## Frontend setup

**Requirements:** Flutter SDK and the backend running on `localhost:8080`.

From the repository root:

```powershell
cd aiops-dashboard
flutter pub get
flutter run -d chrome
```

The dashboard can also be run on a supported emulator/device with:

```powershell
flutter run
```

The backend must be running for live API data.

For Android emulator networking, the host machine is commonly reachable through:

```text
10.0.2.2
```

For a physical Android device on the same network, configure the frontend API base URL to use the computer's LAN IP.

---

## API reference

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/logs` | Ingest raw APM logs (array) |
| `GET` | `/api/logs?page=&size=` | List ingested logs (PII redacted) |
| `GET` | `/api/incidents` | List all incidents with AI diagnosis + token metrics |
| `GET` | `/api/incidents/{id}` | Single incident detail |
| `POST` | `/api/incidents/{id}/analyze` | Manually re-run AI diagnosis |
| `GET` | `/api/health` | Overall system health score |

---

## Testing

### Backend

```powershell
cd aiops-apm
.\mvnw.cmd test
```

The backend tests cover:

- PII/secret sanitization, including truncated JWTs, escaped-JSON secrets, and idempotency
- Log pruning and frame-capping under deliberately large/repetitive input
- Root-cause classification
- Incident clustering, including same-incident vs. different-incident cases and time-window behavior
- Severity and health scoring
- AI diagnostic fallback behavior
- End-to-end log ingestion
- Deduplication

### Frontend

```powershell
cd aiops-dashboard
flutter test
```

---

## Known limitations

- Token estimates use a simple `~4 characters per token` heuristic for the "raw" and "compacted" comparison; only `ai_prompt_tokens` and `ai_completion_tokens` are Gemini's real, measured tokenizer counts.
- The representative sample for an incident is fixed at creation and does not update as more logs join — this is a deliberate choice for stable, non-flip-flopping AI diagnoses, not an oversight.
- `ddl-auto: update` is used for convenience during development; a production deployment would use a proper migration tool (e.g. Flyway).
- The Gemini API is an external dependency; when it is unavailable or fails, the backend falls back to deterministic rule-based diagnosis.

---

## Assessment coverage

The implementation covers the assessment's core requirements:

- Spring Boot REST API
- Flutter dashboard
- MySQL persistence
- PII and secret redaction
- Token-efficient log preprocessing
- Smart stack-trace pruning
- Root-cause classification and fingerprinting
- Incident clustering to avoid unnecessary AI calls
- Rule-based severity and health scoring
- Structured Gemini JSON output
- Jira and Slack payload generation
- Clean frontend state management
- Automated backend tests
