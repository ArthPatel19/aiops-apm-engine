# AI-Ops APM Engine — Backend

Spring Boot backend for the AI-Ops APM Engine assessment.

## Technology

- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- MySQL
- Google Gemini API
- Maven

## Responsibilities

The backend implements:

```text
Raw Logs
→ Validation / Deduplication
→ Sanitization
→ Persistence
→ Log Pruning / Context Compaction
→ Root-Cause Classification
→ Fingerprinting / Clustering
→ Severity + Health Scoring
→ Gemini Diagnosis
→ Rule-Based Fallback (when Gemini fails)
→ Incident Persistence
```

## Structure

```text
aiops-apm/
├── .mvn/
├── src/
│   ├── main/
│   └── test/
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
├── sample-logs.json
└── README.md
```

## Database

Create the development database:

```sql
CREATE DATABASE aiops_apm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Set credentials through environment variables:

```powershell
setx DB_USER "aiops"
setx DB_PASSWORD "your_password_here"
```

For Gemini:

```powershell
setx GEMINI_API_KEY "your_gemini_key"
setx GEMINI_MODEL "gemini-3.5-flash-lite"
```

Reopen the terminal/IDE after using `setx`, then restart the application.

## Run

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Server:

```text
http://localhost:8080
```

## Sample Logs

The repository contains `sample-logs.json`.

```powershell
curl -X POST "http://localhost:8080/api/logs" `
  -H "Content-Type: application/json" `
  --data-binary "@sample-logs.json"
```

Retrieve incidents:

```powershell
curl "http://localhost:8080/api/incidents"
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/logs` | Ingest raw APM logs |
| GET | `/api/logs?page=&size=` | List logs |
| GET | `/api/incidents` | List incidents |
| GET | `/api/incidents/{id}` | Get one incident |
| POST | `/api/incidents/{id}/analyze` | Re-run AI diagnosis |
| GET | `/api/health` | Get system health |

## AI Diagnosis

Gemini receives compact incident context and returns:

- Root-cause analysis
- Remediation steps
- Jira ticket payload
- Slack alert payload

If Gemini is unavailable, the rule-based fallback is used.

## Token Metrics

Incidents store:

- `sample_raw_tokens`
- `compacted_context_tokens`
- `estimated_raw_tokens`
- `ai_prompt_tokens`
- `ai_completion_tokens`

The first three are application-side measurements/estimates. Gemini's reported usage metadata supplies the actual prompt and completion token counts.

## Testing

```powershell
.\mvnw.cmd test
```

The test suite covers sanitization, pruning, relevant-frame selection, classification, clustering, severity/health scoring, fallback behavior, ingestion, and deduplication.

## Configuration

```properties
spring.application.name=aiops-apm

spring.datasource.url=jdbc:mysql://localhost:3306/aiops_apm
spring.datasource.username=${DB_USER:aiops}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

server.port=8080
spring.jackson.property-naming-strategy=SNAKE_CASE
```

Never commit real passwords or API keys.
