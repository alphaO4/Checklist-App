# Checklist App Backend (FastAPI)

FastAPI backend for the Vehicle Inspection Checklist App (Feuerwehr).

- Python/FastAPI + SQLAlchemy ORM
- Default DB: SQLite (dev) with optional PostgreSQL via `DATABASE_URL`
- JWT auth (password grant), CORS for Electron
- WebSocket stub for realtime events (broadcast)
- Offline sync endpoint for queued actions

## Quick start

Prereqs: Python 3.11+

1) Configure environment

Create `.env` from example (defaults are fine for local dev):

```
Copy-Item .env.example .env
```

2) Install deps and run server (PowerShell):

```
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

3) Verify

- Health: GET http://127.0.0.1:8000/health
- Docs: http://127.0.0.1:8000/docs

## Config

`.env` (or environment variables):

- DATABASE_URL: SQLAlchemy URL. Default: `sqlite:///./data.db`
- JWT_SECRET: Secret for JWT signing (change in prod!)
- ACCESS_TOKEN_EXPIRE_MINUTES: Token lifetime (default 60)
- CORS_ORIGINS: Comma-separated list for Electron dev, default allows localhost

## Dev notes

- Tables are auto-created on startup for development.
- Default dev user is seeded: admin/admin.
- For production, use PostgreSQL and migrations (Alembic), add SSL, proper secrets, and user/role management.
- WebSocket events are placeholders; integrate real-time once backend logic finalizes.
