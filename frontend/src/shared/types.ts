// Shared types between main and renderer processes

export interface Benutzer {
  id: string;
  username: string;
  email: string;
  rolle: 'benutzer' | 'gruppenleiter' | 'organisator' | 'admin';
  gruppeId?: string;
  createdAt: string;
}

export interface Gruppe {
  id: string;
  name: string;
  gruppenleiterId: string;
  createdAt: string;
}

export interface Fahrzeuggruppe {
  id: string;
  name: string;
  gruppeId: string;
  createdAt: string;
}

export interface Fahrzeug {
  id: string;
  kennzeichen: string;
  typ: 'MTF' | 'RTB' | 'FR';
  fahrzeuggruppeId: string;
  createdAt: string;
}

export interface TuvTermin {
  id: string;
  fahrzeugId: string;
  ablaufDatum: string;
  status: 'current' | 'warning' | 'expired';
  letztePruefung?: string;
  createdAt: string;
}

export interface Checkliste {
  id: string;
  name: string;
  fahrzeuggruppeId: string;
  erstellerId: string;
  template: boolean;
  createdAt: string;
}

export interface ChecklistItem {
  id: string;
  checklisteId: string;
  beschreibung: string;
  pflicht: boolean;
  reihenfolge: number;
  createdAt: string;
}

export interface ChecklistAusfuehrung {
  id: string;
  checklisteId: string;
  fahrzeugId: string;
  benutzerId: string;
  status: 'started' | 'in_progress' | 'completed' | 'failed';
  startedAt: string;
  completedAt?: string;
}

export interface ItemErgebnis {
  id: string;
  ausfuehrungId: string;
  itemId: string;
  status: 'ok' | 'fehler' | 'nicht_pruefbar';
  kommentar?: string;
  createdAt: string;
}

// WebSocket event types
export interface ChecklistUpdatedEvent {
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

export interface TuvDeadlineEvent {
  type: 'tuv_deadline_warning';
  fahrzeugId: string;
  kennzeichen: string;
  ablaufDatum: string;
  tageVerbleibend: number;
  priority: 'expired' | 'warning' | 'reminder';
}

export interface UserAssignmentEvent {
  type: 'user_assignment_changed';
  userId: string;
  oldGruppeId?: string;
  newGruppeId: string;
  changedBy: string;
  timestamp: string;
}

export interface UserPresenceEvent {
  type: 'user_presence';
  checklistId: string;
  userId: string;
  username: string;
  action: 'joined' | 'left' | 'editing_item';
  itemId?: string;
}

// Offline sync types
export interface OfflineAction {
  id: string;
  type: 'item_update' | 'checklist_complete' | 'comment_add';
  data: any;
  timestamp: string;
  retryCount: number;
  synced: boolean;
}

// API response types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

// Authentication types
export interface LoginCredentials {
  username: string;
  password: string;
}

export interface AuthToken {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

export interface AuthState {
  user: Benutzer | null;
  token: AuthToken | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
