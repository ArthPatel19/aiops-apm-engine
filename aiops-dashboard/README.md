# AI-Ops APM Dashboard — Frontend

Flutter dashboard for the AI-Ops APM Engine assessment.

The dashboard provides the operator-facing UI for overall system health, ingested APM logs, grouped incidents, severity, AI diagnosis, remediation guidance, and copy-pasteable Jira/Slack payloads.

## Technology

- Flutter
- Dart
- BLoC
- Clean architecture
- REST API integration
- Responsive UI for supported Flutter targets

## Responsibilities

The dashboard consumes the Spring Boot backend and presents:

- Health overview
- Overall system health score
- Error count
- Live ingested log feed
- PII-redacted logs
- Grouped incidents
- Incident severity
- AI-generated root-cause analysis
- Suggested remediation steps
- Jira ticket payload
- Slack alert payload
- Incident token metrics

## Architecture

The frontend follows clean architecture and BLoC-based state management.

```text
Flutter UI
   ↓
BLoC / Presentation State
   ↓
Domain / Application Logic
   ↓
Data Layer / Repository
   ↓
REST API
   ↓
Spring Boot Backend
```

The dashboard and backend are separate applications but are part of the same assessment repository.

## Project structure

```text
aiops-dashboard/
├── android/
├── ios/
├── lib/
├── linux/
├── macos/
├── test/
├── web/
├── windows/
├── .gitignore
├── .metadata
├── analysis_options.yaml
├── pubspec.lock
├── pubspec.yaml
└── README.md
```

The Flutter platform directories are retained because the application can target Flutter Web and supported mobile/desktop platforms.

## Requirements

- Flutter SDK
- Dart SDK included with Flutter
- Running AI-Ops APM backend

Backend:

```text
http://localhost:8080
```

## Install

From this directory:

```powershell
flutter pub get
```

## Run

For Flutter Web:

```powershell
flutter run -d chrome
```

For another available Flutter device/emulator:

```powershell
flutter run
```

The backend must be running for live data.

## Backend connection

The dashboard communicates with the Spring Boot REST API.

Main endpoints consumed by the dashboard include:

```text
GET /api/health
GET /api/logs
GET /api/incidents
GET /api/incidents/{id}
```

For local development, the backend runs on:

```text
http://localhost:8080
```

For an Android emulator, the host machine is commonly reachable through:

```text
10.0.2.2
```

For a physical Android device on the same network, configure the API base URL to use the computer's LAN IP.

## Dashboard flow

```text
Health Overview
      │
      ├── system health score
      └── error count
             │
             ▼
        Live Log Feed
             │
             ▼
       Incident Board
             │
             ▼
     Incident Detail Screen
             │
       ┌─────┼──────────────┐
       ▼     ▼              ▼
    Root   Remediation   Jira / Slack
    Cause    Steps         Payloads
```

## Incident detail

The incident detail view is designed around the assignment requirement to let an operator inspect an AI-generated incident diagnosis.

It presents the available incident information, including:

- root-cause summary
- remediation steps
- severity
- affected feature/API
- incident metadata
- Jira payload
- Slack payload
- token metrics where provided by the backend

The Jira and Slack payloads are structured JSON so they can be copied into the corresponding workflow.

## Testing

Run Flutter tests with:

```powershell
flutter test
```

## Build

Web:

```powershell
flutter build web
```

Android:

```powershell
flutter build apk
```

## Running both applications

Use two terminals.

### Terminal 1 — backend

```powershell
cd aiops-apm
.\mvnw.cmd spring-boot:run
```

### Terminal 2 — frontend

```powershell
cd aiops-dashboard
flutter run -d chrome
```

The normal local flow is:

```text
Flutter Dashboard
       ↓
http://localhost:8080
       ↓
Spring Boot API
       ↓
MySQL + AI diagnosis
```

## Repository relationship

The frontend and backend are intentionally separate applications inside one GitHub repository:

```text
AI-Ops-APM-Engine/
├── aiops-apm/          # Spring Boot backend
└── aiops-dashboard/    # Flutter frontend
```

This keeps the two applications independently buildable and testable while presenting the complete assessment as one repository.

Backend documentation:

[`../aiops-apm/README.md`](../aiops-apm/README.md)

Repository documentation:

[`../README.md`](../README.md)
