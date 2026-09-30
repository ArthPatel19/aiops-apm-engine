# AI-Ops APM Engine

A low-cost, AI-driven Application Performance Monitoring (APM) operations
engine. It ingests raw error logs, redacts sensitive data, groups related
errors into incidents, and uses an LLM (Google Gemini) to produce a
root-cause diagnosis, remediation steps, and ready-to-paste Jira/Slack
payloads — while keeping AI token usage to a minimum.

**Backend:** Java 21, Spring Boot, MySQL
**Frontend:** Flutter (Web + Mobile), BLoC, clean architecture
**AI provider:** Google Gemini (`gemini-3.5-flash-lite`), with a
deterministic zero-cost fallback if the AI is unavailable

---

## Table of contents

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

```
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

## Why this is token-efficient

The assignment's core requirement is minimizing LLM token usage without
losing diagnostic quality. This project does that in three layers, and we
measure each one honestly rather than presenting a single flattering
number:

1. **Fewer calls, not smaller calls, is the biggest lever.** The AI is
   called exactly once per *incident*, never once per *log*, and never
   again once an incident exists — proven with real data: sending the same
   3 logs twice produced `duplicates: 3` and **zero** additional AI calls.
   In a real outage with hundreds of cascading logs, this ratio is what
   actually controls cost.
2. **A hard ceiling on what any single call can cost.** Regardless of how
   long or repetitive a raw stack trace is, pruning caps it to 3 relevant
   frames (collapsing duplicates) and normalizes/truncates the message.
   This bounds worst-case cost even if a stack trace is genuinely
   "thousands of lines long," as the brief describes.
3. **Measured, apples-to-apples payload reduction.** Each incident stores
   four numbers, computed so the comparison is fair:
   - `sample_raw_tokens` — the representative log's raw diagnostic
     payload (type, status, feature, API, full message, full stack trace)
   - `compacted_context_tokens` — the same fields, after normalization,
     truncation, and frame-capping
   - `estimated_raw_tokens` — the cumulative raw volume across every log
     in the incident (grows with `logCount`, unlike the actual AI cost)
   - `ai_prompt_tokens` — Gemini's own reported `usageMetadata.promptTokenCount`
     for the one real call made

   We deliberately do **not** claim `ai_prompt_tokens` is "lower than"
   an estimate — they measure different things (an estimate vs. a real
   tokenizer count including instructions and schema overhead). The fair,
   provable comparison is `sample_raw_tokens` vs. `compacted_context_tokens`.

## Project structure

```
aiops-apm-engine/
├── backend/          Spring Boot REST API
│   └── src/main/java/com/aiops/aiops_apm/
│       ├── controller/    REST endpoints
│       ├── service/       business logic (sanitizer, pruning,
│       │                  clustering, severity, health, AI)
│       ├── repository/    Spring Data JPA
│       ├── entity/        ApmLog, Incident
│       ├── dto/            request/response records
│       ├── config/         Gemini properties, CORS
│       └── exception/      global error handling
└── frontend/          Flutter dashboard (clean architecture + BLoC)
    └── lib/
        ├── core/            networking, theming, shared widgets
        ├── features/
        │   ├── health/      domain / data / presentation
        │   ├── logs/        domain / data / presentation
        │   └── incidents/   domain / data / presentation
        └── presentation/    top-level dashboard page
```

## Backend setup

**Requirements:** Java 21, Maven, MySQL 8.x, a free Google Gemini API key.

1. **Create the database:**
   ```sql
   CREATE DATABASE aiops_apm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'aiops'@'localhost' IDENTIFIED BY 'your_password_here';
   GRANT ALL PRIVILEGES ON aiops_apm.* TO 'aiops'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Set environment variables** (never commit real credentials):
   ```
   setx DB_USER "aiops"
   setx DB_PASSWORD "your_password_here"
   setx GEMINI_API_KEY "your_gemini_key"
   setx GEMINI_MODEL "gemini-3.5-flash-lite"
   ```
   Close and reopen your terminal/IDE afterwards so they take effect.

3. **Run:**
   ```
   cd backend
   mvn test
   mvn spring-boot:run
   ```
   Server starts on `http://localhost:8080`.

4. **Try it:**
   ```
   curl -X POST "http://localhost:8080/api/logs" -H "Content-Type: application/json" --data-binary "@sample-logs.json"
   curl "http://localhost:8080/api/incidents"
   ```

If no `GEMINI_API_KEY` is set, the app still works fully — it automatically
uses the deterministic rule-based fallback instead of calling Gemini.

## Frontend setup

**Requirements:** Flutter SDK, the backend running on `localhost:8080`.

```
cd frontend
flutter create --project-name apm_dashboard .   # only needed once, generates platform folders
flutter pub get
flutter run -d chrome                            # or an emulator/device
```

The dashboard auto-detects Android emulators (`10.0.2.2`) vs. web/iOS/desktop
(`localhost`). For a real Android device on the same network, edit
`lib/core/constants/api_constants.dart` and point it at your computer's LAN
IP instead.

## API reference

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/logs` | Ingest raw APM logs (array) |
| `GET` | `/api/logs?page=&size=` | List ingested logs (PII redacted) |
| `GET` | `/api/incidents` | List all incidents with AI diagnosis + token metrics |
| `GET` | `/api/incidents/{id}` | Single incident detail |
| `POST` | `/api/incidents/{id}/analyze` | Manually re-run AI diagnosis |
| `GET` | `/api/health` | Overall system health score |

## Testing

```
cd backend
mvn test
```

Covers: PII/secret sanitization (including truncated JWTs, escaped-JSON
secrets, and idempotency), log pruning and frame-capping under
deliberately large/repetitive input, root-cause classification,
clustering (same-incident vs. different-incident vs. outside time window),
severity/health scoring, the AI diagnostic fallback path, and end-to-end
log ingestion with deduplication.

## Known limitations

- Token estimates use a simple `~4 characters per token` heuristic for the
  "raw" and "compacted" comparison; only `ai_prompt_tokens` is Gemini's
  real, measured tokenizer count.
- The representative sample for an incident is fixed at creation and does
  not update as more logs join — this is a deliberate choice for stable,
  non-flip-flopping AI diagnoses, not an oversight.
- `ddl-auto: update` is used for convenience during development; a
  production deployment would use a proper migration tool (e.g. Flyway).
