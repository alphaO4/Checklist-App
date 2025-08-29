# Copilot Instructions for Checklist App

## Project Overview
This is a **Vehicle Inspection Checklist App** for German fire departments (Feuerwehr), enabling organized vehicle safety checks with TÜV (vehicle inspection) deadline tracking.

## Current Implementation Status
- ✅ **Frontend**: Functional Electron app with TypeScript main process, vanilla JS renderer
- ✅ **Backend**: Full FastAPI implementation with SQLAlchemy models and authentication  
- ✅ **Database**: SQLite for development, complete domain models implemented
- ✅ **IPC Architecture**: Secure main/renderer communication with preload script
- ✅ **CRUD Operations**: Vehicle types, vehicles, groups, TÜV tracking all functional
- ✅ **Authentication**: JWT-based auth with role hierarchy (Benutzer → Gruppenleiter → Organisator → Admin)

## Architecture & Tech Stack
- **Frontend**: Electron (TypeScript) with HTML/CSS renderer
- **Offline Storage**: SQLite3 for local data persistence
- **IPC**: Secure contextBridge pattern for main/renderer communication
- **Planned Backend**: Python/FastAPI + PostgreSQL + WebSockets
- **Deployment Target**: Ubuntu VM on Proxmox

## Domain Model (German Context)
Key entities and their relationships:
- **Benutzer** (Users) → **Gruppen** (Groups) → **Fahrzeuggruppen** (Vehicle Groups) → **Fahrzeuge** (Vehicles)
- **Checklisten** (Checklists) assigned to vehicles/groups
- **TÜV-Termine** (inspection deadlines) with expiration tracking
- Role hierarchy: Benutzer < Gruppenleiter < Organisator < Admin

### Expected Database Schema
```sql
-- Core entities
benutzer: id, username, email, password_hash, rolle, created_at
gruppen: id, name, gruppenleiter_id, created_at
fahrzeuggruppen: id, name, gruppe_id, created_at
fahrzeuge: id, kennzeichen, typ (MTF/RTB/FR), fahrzeuggruppe_id, created_at

-- TÜV tracking
tuv_termine: id, fahrzeug_id, ablauf_datum, status, letzte_pruefung, created_at

-- Checklists
checklisten: id, name, fahrzeuggruppe_id, ersteller_id, template, created_at
checklist_items: id, checkliste_id, beschreibung, pflicht, reihenfolge, created_at
checklist_ausfuehrungen: id, checkliste_id, fahrzeug_id, benutzer_id, status, started_at, completed_at
item_ergebnisse: id, ausfuehrung_id, item_id, status (ok/fehler/nicht_pruefbar), kommentar, created_at

-- Audit log
audit_log: id, benutzer_id, aktion, ressource_typ, ressource_id, alte_werte, neue_werte, timestamp
```
- **Frontend**: Electron (TypeScript main + vanilla JS renderer) 
- **Backend**: FastAPI + SQLAlchemy + SQLite (dev) / PostgreSQL (prod)
- **IPC**: Secure contextBridge pattern with preload script
- **UI Pattern**: Component-based vanilla JS with inline `onclick` handlers
- **State**: Custom stores (`appStore`, `dataManager`) with caching layer
- **Styling**: CSS custom properties with fire department theme


## Critical Development Workflows

### Building & Running
```bash
# Backend (start first)
cd backend
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
# ⚠️ CRITICAL: Do NOT run any other commands in this terminal window!
# The backend process must remain running continuously during development.

# Frontend (requires backend running, always specify repository path)
cd "e:\Github\Checklist-App\frontend"     # Always use full repository path
npm run build                             # Required: Generate config, compile TS → JS, copy assets
npm start                                 # Launch Electron (runs prestart build automatically)
npm run dev                               # Watch mode with concurrently
```

### Configuration System (NEW)
The frontend now uses **configurable backend URLs** instead of hardcoded IPs:

```bash
# Default development (127.0.0.1:8000)
npm run build

# Custom backend server
BACKEND_HOST=192.168.1.100 npm run build

# Production HTTPS setup
BACKEND_HOST=api.example.com BACKEND_PROTOCOL=https npm run build
```

**Environment Files:**
- `.env.template` - Configuration template
- `.env.development` - Development defaults
- `.env.production` - Production defaults  
- `.env.local` - Local overrides (gitignored)

**Configuration Variables:**
- `BACKEND_HOST` - Server hostname/IP
- `BACKEND_PORT` - Server port (default: 8000)
- `BACKEND_PROTOCOL` - http/https
- `BACKEND_WS_PROTOCOL` - ws/wss

### Testing
```bash
# Backend API tests (backend must be running)
cd backend
python test_api.py                        # Basic API functionality tests
python test_vehicle_editing.py           # Vehicle management tests
# ⚠️ Frontend testing framework not yet implemented
```

### Project Structure
```
frontend/src/
├── main/                    # Main process (Node.js context)
│   ├── main.ts             # App lifecycle, window creation, security CSP
│   ├── preload.ts          # contextBridge API exposure (electronAPI)
│   ├── backend.ts          # HTTP client for FastAPI communication
│   └── ipc/handlers.ts     # IPC request/response handlers
├── renderer/               # Renderer process (Web context) 
│   ├── index.html          # SPA with German UI, includes all component scripts (template)
│   ├── js/renderer.js      # Main app controller, navigation, authentication
│   ├── components/         # Page components (vanilla JS classes)
│   ├── stores/             # dataManager (caching), appStore (state), authManager
│   ├── utils/configUtils.js # Configuration helpers for API calls
│   └── styles/main.css     # Fire department red theme, CSS custom properties
├── shared/                 # Shared utilities
│   ├── config.ts           # Configuration management for main process
│   └── types.ts            # TypeScript type definitions
backend/app/
├── main.py                 # FastAPI app, CORS, router includes
├── models/                 # SQLAlchemy domain models (German naming)
├── api/routes/             # REST endpoints by feature
├── core/                   # Settings, security, dependencies  
└── services/               # Business logic, seed data
scripts/
└── generate-config.js      # Build-time configuration generator
```

## Core Implementation Patterns

### Component Architecture (Vanilla JS)
```javascript
// Pattern: Class-based components with render() + mount() lifecycle
class FahrzeugtypenPage {
  async render() {
    await this.loadData();
    return `<div>...</div>`; // Return HTML string
  }
  
  mount() {
    this.setupEventListeners(); // Called after DOM insertion
  }
}

// Global exposure for onclick handlers
window.fahrzeugtypenPage = new FahrzeugtypenPage();

// Usage in HTML: onclick="fahrzeugtypenPage.methodName()"  
// ⚠️ Prefer onclick over addEventListener for main actions (timing issues)
```

### Secure IPC Communication
```typescript
// preload.ts - Expose safe APIs via contextBridge
contextBridge.exposeInMainWorld('electronAPI', {
  listVehicles: () => ipcRenderer.invoke('api-list-vehicles'),
  createVehicleType: (data) => ipcRenderer.invoke('api-create-vehicle-type', data)
});

// renderer: Access via window.electronAPI
const vehicles = await window.electronAPI.listVehicles();
```

### Data Flow Pattern
```javascript
// 1. Component calls dataManager
await window.dataManager.loadVehicles();

// 2. dataManager calls electronAPI  
const response = await window.electronAPI.listVehicles();

// 3. electronAPI invokes IPC handler
ipcMain.handle('api-list-vehicles', () => backendClient.listVehicles());

// 4. backendClient makes HTTP request to FastAPI
const res = await fetch(`${this.baseUrl}/vehicles`, { headers: authHeaders });
```

### Configuration-Based API Calls (NEW)
```javascript
// Renderer components use configUtils for all API calls
const response = await window.configUtils.fetchBackend('/vehicles', {
  method: 'GET'
});

// configUtils automatically handles:
// - Backend URL resolution from runtime config
// - Authentication token injection
// - Error handling and fallbacks

// Direct access to configuration
const backendUrl = window.getBackendUrl();
const config = window.APP_CONFIG.backend;
```

### German Domain Terminology
- **Entities**: `Fahrzeug`, `Fahrzeugtyp`, `Fahrzeuggruppe`, `Gruppe`, `TÜV-Termin`
- **Database**: snake_case (`fahrzeug_id`, `fahrzeugtyp_id`, `ablauf_datum`)
- **UI Text**: Native German (`Fahrzeugtyp hinzufügen`, `TÜV-Termine verwalten`)
- **Status Values**: German (`ok`, `fehler`, `nicht_pruefbar`, `current`, `warning`, `expired`)

## Backend Integration Specifics

### FastAPI Route Structure
```python
# Pattern: Feature-based routers with dependency injection
from ...core.deps import get_current_user
from ...models.vehicle_type import FahrzeugTyp

@router.post("", response_model=FahrzeugTypSchema)
def create_vehicle_type(
    data: FahrzeugTypCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    # Permission checks for organisator/admin roles
    check_admin_permission(current_user)
```

### Authentication Flow
```typescript
// Login stores JWT token, all subsequent requests include Bearer header
const { access_token } = await backendClient.login(username, password);
this.token = access_token; // Stored in tokenStorage for persistence

// Auto-included in requests via authHeaders()
private authHeaders() {
  return this.token ? { 'Authorization': `Bearer ${this.token}` } : {};
}
```

### Domain Model (SQLAlchemy Relationships)
```python
# Core hierarchy: Benutzer → Gruppe → FahrzeugGruppe → Fahrzeug
# Key models in /backend/app/models/:
# - user.py: Benutzer with rolle hierarchy
# - group.py: Gruppe with gruppenleiter relationship  
# - vehicle.py: FahrzeugGruppe, Fahrzeug with fahrzeugtyp
# - checklist.py: TuvTermin, Checkliste, ChecklistItem, ChecklistAusfuehrung, ItemErgebnis
# - vehicle_type.py: FahrzeugTyp (MTF, RTB, LF, etc.)

# Example relationship pattern:
class Fahrzeug(Base):
    fahrzeugtyp = relationship("FahrzeugTyp", back_populates="fahrzeuge")
    fahrzeuggruppe = relationship("FahrzeugGruppe", back_populates="fahrzeuge")
    tuv_termine = relationship("TuvTermin", back_populates="fahrzeug", cascade="all, delete-orphan")
```

## Security & Production Considerations
- **Electron Security**: `contextIsolation: true`, `nodeIntegration: false`, restrictive CSP
- **Window Management**: Custom title bar with `-webkit-app-region: drag`
- **Error Handling**: User-friendly German messages, console logging for debugging
- **Role-based Access**: Backend enforces permissions, frontend adapts UI based on user role

## Development Tips
- **Build First**: Always `npm run build` before `npm start` (TypeScript compilation required)
- **Repository Path**: Always specify full repository path when running `npm start` in new terminals
- **Backend Terminal**: Never run other commands in the backend terminal - dedicated process only
- **Configuration**: All hardcoded IPs have been replaced with configurable environment variables
- **Component Events**: Use `onclick="componentName.method()"` for reliability over addEventListener
- **Cache Strategy**: dataManager implements 5-minute cache with `forceRefresh` option  
- **German Context**: All user-facing text, entity names, and database fields use German terminology
- **Debugging**: Check both Electron DevTools (renderer) and terminal output (main process)

## Known TODOs & Technical Debt
- **Testing**: Frontend testing framework needs implementation (backend has basic test files)
- ✅ **IP Configuration**: ~~All hardcoded IPs need to be replaced~~ - COMPLETED: Now uses configurable environment variables
- ✅ **Environment Variables**: ~~Centralize all URL configurations~~ - COMPLETED: `.env` files with build-time generation
- ✅ **CSP Updates**: ~~Content Security Policy hardcodes backend URLs~~ - COMPLETED: Dynamic CSP generation
