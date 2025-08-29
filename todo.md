# TODO – Checklist App (Feuerwehr)

This file tracks the remaining work to reach a usable MVP and beyond. Items are grouped and roughly prioritized.

## 🟥 Must-have (MVP)

- [x] Backend: Authentication
  - [x] Persist users beyond seed; add basic CRUD for Benutzer (admin-only)
  - [x] Return useful error payloads (i18n-ready messages)
  - [x] Store password policy and validation
- [x] Backend: Domain APIs (CRUD minimal)
  - [x] Fahrzeuge: list/create/get/update/delete with pagination and filtering
  - [x] Checklisten: templates + instances; list/create/get/update; minimal schema validation
  - [x] TÜV: list deadlines + create/update records, derive status (expired/warning/current)
  - [x] Sync: accept offline actions and persist/process idempotently; map to domain changes
- [x] Backend: Data & DB
  - [x] Add schemas (Pydantic) for vehicles/checklists/TÜV (create/update/read)
  - [x] Implement missing ORM models/relations and constraints (unique kennzeichen, FKs)
  - [x] Add simple seed data for demo (vehicles, a checklist template, deadlines)
- [x] Backend: Security
  - [x] Role-based access control (Benutzer/Gruppenleiter/Organisator/Admin)
  - [x] Protect domain routes with OAuth2 (current_user); enforce per-role permissions
  - [x] CORS refinement for production origins
- [x] Frontend (renderer)
  - [x] Replace placeholder pages with real views using backend data (tables/forms)
  - [x] Login screen: show API error messages, keep session state in renderer
  - [x] Basic state management (simple module or store) to hold user/token
- [x] Frontend (main)
  - [x] Persist auth token securely (e.g., keytar/OS keychain) and restore on app start
  - [x] Health check to detect backend availability instead of navigator.onLine
- [ ] Offline-first & Sync
  - [ ] Write offline queueers from UI actions (create/update checklist runs, item updates)
  - [x] Robust sync: backoff, partial failures, retry increments, per-action receipts
  - [ ] Conflict policy (last-write-wins or per-item merge) for checklist items
- [ ] Real-time (WebSocket)
  - [ ] Implement client connection in main process; forward events to renderer
  - [ ] Backend broadcast stubs: checklist_updated, tuv_deadline_warning, user_presence
- [ ] App Hardening
  - [x] Tighten Content-Security-Policy to allow only required ws:/http(s) endpoints
  - [ ] Validate and sanitize all IPC payloads

## 🟧 Should-have (Post-MVP)

- [ ] Backend
  - [ ] Pagination and filtering across list endpoints
  - [ ] Search by kennzeichen/typ; checklist name
  - [ ] Audit log write paths for key actions
  - [ ] Alembic migrations and migration docs
- [ ] Frontend UX
  - [ ] Checkliste durchführen flow (start, mark items ok/fehler/nicht_pruefbar, Kommentare)
  - [ ] TÜV board (cards with color status, due-in days)
  - [ ] Fahrzeuge table with quick actions
  - [ ] Error toasts and retry affordances for failed API calls
- [ ] Sync
  - [ ] Visual sync status indicator and manual “Jetzt synchronisieren” button
  - [ ] Queue inspector view (pending/failed/history)
- [ ] Security
  - [ ] Token refresh/renewal strategy; logout
  - [ ] Rate limiting/abuse mitigations on auth
- [ ] Packaging & Config
  - [ ] Environment config for BACKEND_URL (dev/prod) and feature flags
  - [ ] App auto-updater wiring (electron-updater) for release channels

## 🟨 Nice-to-have

- [ ] Backend
  - [ ] File upload (e.g., attach images to item_ergebnisse)
  - [ ] Reports export (CSV/PDF) endpoints
  - [ ] WebSocket presence (joined/left/editing_item)
- [ ] Frontend
  - [ ] Reports UI with export (CSV/PDF)
  - [ ] Dark mode; larger touch targets for tablets
  - [ ] Keyboard shortcuts for checklist navigation
- [ ] Performance
  - [ ] Index optimization and query profiling for larger datasets
  - [ ] SQLite WAL mode for local store, compaction controls

## 🧪 Testing & QA

- [ ] Backend tests (pytest)
  - [ ] Auth happy path + invalid creds
  - [ ] Vehicles CRUD
  - [ ] TÜV deadline status derivation logic
  - [ ] Sync actions endpoint idempotency
- [ ] Frontend tests
  - [ ] Unit tests for stores/utils
  - [ ] Smoke test for login + load vehicles
- [ ] E2E
  - [ ] Minimal E2E: start backend, start app, login, create checklist run

## 🛠️ DevEx & CI/CD

- [ ] Pre-commit hooks (black/isort/ruff for Python; eslint/prettier for TS when added)
- [ ] GitHub Actions
  - [ ] Backend: lint + test matrix
  - [ ] Frontend: build + package
  - [ ] Draft release artifacts per tag
- [ ] Docker
  - [ ] Backend Dockerfile + compose (PostgreSQL target)
  - [ ] Dev compose for local stack

## 🔐 Compliance & Ops

- [ ] Secrets management (JWT secret, DB creds) via environment/KeyVault
- [ ] Logging/Observability
  - [ ] Structured logs and request IDs
  - [ ] Basic metrics/health endpoints, uptime alerts
- [ ] Backup/Retention for DB (prod)

## 🔄 Type/Schema Alignment

- [ ] Align frontend types (`src/shared/types.ts`) with backend Pydantic models
- [ ] Generate TypeScript types from OpenAPI (e.g., openapi-typescript) to avoid drift

## 📄 Docs

- [ ] Minimal user guide (German) for workflow
- [ ] API docs notes for auth and sync semantics
- [ ] Dev setup guide (Windows/Unix), troubleshooting

---

Short-term focus to unlock a functional demo:
1) Implement Vehicles/Checklists/TÜV CRUD + schemas in backend, seed sample data
2) Wire real list/detail views in renderer (read-only first)
3) Persist token securely and add backend health indicator
4) Implement sync-push processing server-side (accept/record actions)
5) Add a few backend unit tests and a smoke flow
