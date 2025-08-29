# Copilot Instructions for Checklist App

## Project Overview
This is a **Vehicle Inspection Checklist App** for German fire departments (Feuerwehr), enabling organized vehicle safety checks with TÜV (vehicle inspection) deadline tracking.

## Current Implementation Status
- ✅ **Frontend**: Functional Electron app with TypeScript, offline-capable UI
- ❌ **Backend**: Not implemented (empty `/backend` directory)
- ✅ **Offline Storage**: SQLite implementation for local data (`frontend/src/main/store/sqlite.ts`)
- ✅ **IPC Architecture**: Secure main/renderer communication with preload script
- ⚠️ **Real-time Sync**: Structured but not connected to backend
- ⚠️ **Authentication**: UI implemented, backend integration pending

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

## Critical Development Workflows

### Building & Running
```bash
# Frontend development
cd frontend
npm install                    # Install dependencies
npm run dev                   # Development with hot reload
npm run build                 # Production build (required before start)
npm start                     # Launch Electron app

# Build outputs
dist/                         # TypeScript compilation output
dist/main/main.js            # Main process entry point
dist/renderer/               # Copied HTML/CSS files
```

### Key File Structure & Patterns
```
frontend/src/
├── main/                    # Main process (Node.js context)
│   ├── main.ts             # App lifecycle, window management, security
│   ├── preload.ts          # Secure API exposure via contextBridge  
│   ├── ipc/handlers.ts     # IPC request handlers
│   └── store/sqlite.ts     # Offline SQLite operations
├── renderer/               # Renderer process (Web context)
│   ├── index.html          # Main UI with German content
│   ├── js/renderer.js      # Vanilla JS app logic (no framework)
│   └── styles/main.css     # Fire department themed styles
└── shared/types.ts         # TypeScript interfaces for domain model
```

### Core Implementation Patterns

#### Secure IPC Communication
```typescript
// preload.ts - Only expose safe APIs
contextBridge.exposeInMainWorld('electronAPI', {
  saveOffline: (data) => ipcRenderer.invoke('save-checklist-offline', data),
  onTuvAlert: (callback) => ipcRenderer.on('tuv-alert', callback)
});

// renderer.js - Access via window.electronAPI
if (typeof window.electronAPI !== 'undefined') {
  await window.electronAPI.saveOffline(checklistData);
}
```

#### Offline-First Data Management
```typescript
// SQLite schema in store/sqlite.ts
checklists: id, fahrzeug_id, name, items (JSON), status, last_modified
offline_actions: id, type, data (JSON), timestamp, retry_count, synced

// Usage pattern: Save locally first, sync later
await offlineStore.saveChecklist(data);
await offlineStore.queueAction(syncAction);
```

#### German Domain Terminology
- Use German terms consistently: `Fahrzeug`, `Gruppe`, `TÜV`, `Checkliste`
- Database/types: snake_case (`fahrzeug_id`, `gruppe_id`)
- UI text: Native German (`Anmeldung`, `Fahrzeugprüfung`, `TÜV-Termine`)
- Status values: German (`ok`, `fehler`, `nicht_pruefbar`)

### Security & Production Considerations
- **Context Isolation**: Always `contextIsolation: true`, `nodeIntegration: false`
- **CSP**: Restrictive Content-Security-Policy in HTML
- **Window Management**: Prevent unauthorized window creation
- **Draggable Header**: `-webkit-app-region: drag` for custom title bar

### Integration Points (Planned)
- **Backend API**: `http://localhost:8000` (see `.env.example`)
- **WebSocket Events**: Defined in `shared/types.ts` (not implemented)
- **Sync Strategy**: Queue offline actions, retry on connection restore

Refer to `Notes.md` for complete feature requirements and German terminology.

## Project Structure (Planned)
```
backend/          # FastAPI application
frontend/         # Electron app
shared/           # Common types/schemas
docs/             # API documentation
docker/           # Deployment configs
```

### Electron Development Patterns

#### Project Structure
```
frontend/
├── src/
│   ├── main/           # Main process (Node.js)
│   │   ├── main.ts     # App lifecycle, window management
│   │   ├── ipc/        # IPC handlers for backend communication
│   │   └── store/      # Local SQLite for offline data
│   ├── renderer/       # Renderer process (Web)
│   │   ├── components/ # Vue/React components
│   │   ├── pages/      # Main app pages
│   │   ├── stores/     # State management (Pinia/Zustand)
│   │   └── utils/      # WebSocket client, API calls
│   └── shared/         # Types shared between processes
├── assets/             # Icons, images
└── dist/              # Built application
```

#### Key Electron Patterns
```typescript
// Main process: Window management & offline storage
// main/main.ts
import { app, BrowserWindow, ipcMain } from 'electron';
import { setupOfflineStore } from './store/sqlite';

// Always use contextIsolation and disable nodeIntegration
const createWindow = () => {
  const win = new BrowserWindow({
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: path.join(__dirname, 'preload.js')
    }
  });
};

// IPC for secure communication between processes
// main/ipc/checklist-handlers.ts
ipcMain.handle('save-checklist-offline', async (event, data) => {
  return await offlineStore.saveChecklist(data);
});

// Preload script: Expose safe APIs to renderer
// main/preload.ts
contextBridge.exposeInMainWorld('electronAPI', {
  saveOffline: (data) => ipcRenderer.invoke('save-checklist-offline', data),
  onTuvAlert: (callback) => ipcRenderer.on('tuv-alert', callback)
});
```

#### Cross-Platform Considerations
```typescript
// Auto-updater for deployment
import { autoUpdater } from 'electron-updater';

// Platform-specific UI adjustments
const isMac = process.platform === 'darwin';
const isWindows = process.platform === 'win32';

// Touch-friendly UI scaling
const getScaleFactor = () => {
  const { screen } = require('electron');
  return screen.getPrimaryDisplay().scaleFactor;
};
```

#### Offline-First Architecture
```typescript
// renderer/stores/sync-store.ts
class SyncManager {
  private offlineQueue: OfflineAction[] = [];
  
  async executeAction(action: ChecklistAction) {
    try {
      if (navigator.onLine) {
        await this.syncToServer(action);
      } else {
        await this.queueOffline(action);
      }
    } catch (error) {
      await this.queueOffline(action);
    }
  }
  
  // Sync queued actions when back online
  async syncPendingActions() {
    for (const action of this.offlineQueue) {
      try {
        await this.syncToServer(action);
        this.markSynced(action.id);
      } catch (error) {
        action.retryCount++;
      }
    }
  }
}
```

## Development Setup
Project is currently in planning phase. When implementing:
1. Set up FastAPI backend with PostgreSQL
2. Create Electron frontend with offline capabilities  
3. Implement WebSocket real-time sync
4. Deploy to Ubuntu VM with proper SSL/security

Refer to `Notes.md` for complete feature requirements and German terminology.

#### WebSocket Event Specifications
```typescript
// Real-time checklist collaboration
interface ChecklistUpdatedEvent {
  type: 'checklist_updated';
  checklistId: string;
  fahrzeugId: string;
  userId: string;
  changes: {
    itemId: string;
    status: 'ok' | 'fehler' | 'nicht_pruefbar';
    kommentar?: string;
    timestamp: string;
  }[];
}

// TÜV deadline notifications
interface TuvDeadlineEvent {
  type: 'tuv_deadline_warning';
  fahrzeugId: string;
  kennzeichen: string;
  ablaufDatum: string;
  tageVerbleibend: number;
  priority: 'expired' | 'warning' | 'reminder';
}

// Group/user management changes
interface UserAssignmentEvent {
  type: 'user_assignment_changed';
  userId: string;
  oldGruppeId?: string;
  newGruppeId: string;
  changedBy: string;
  timestamp: string;
}

// Live presence for collaborative editing
interface UserPresenceEvent {
  type: 'user_presence';
  checklistId: string;
  userId: string;
  username: string;
  action: 'joined' | 'left' | 'editing_item';
  itemId?: string;
}
```

#### Offline Sync Queue Schema
```typescript
interface OfflineAction {
  id: string;
  type: 'item_update' | 'checklist_complete' | 'comment_add';
  data: any;
  timestamp: string;
  retryCount: number;
  synced: boolean;
}
```
