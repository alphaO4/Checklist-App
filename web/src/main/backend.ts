// Simple HTTP client for the FastAPI backend using global fetch (Node 18+)
import { getBackendConfig } from '../shared/config';

export class BackendClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl?: string) {
    // Use provided URL, environment variable, or configuration
    if (baseUrl) {
      this.baseUrl = baseUrl.replace(/\/$/, '');
    } else {
      const envUrl = process.env.BACKEND_URL;
      if (envUrl) {
        this.baseUrl = envUrl.replace(/\/$/, '');
      } else {
        const config = getBackendConfig();
        this.baseUrl = config.baseUrl;
      }
    }
    console.log(`[BackendClient] Initialized with baseUrl: ${this.baseUrl}`);
  }

  setToken(token: string | null) {
    this.token = token;
  }

  getToken() {
    return this.token;
  }

  private authHeaders(): Record<string, string> {
    const headers: Record<string, string> = {};
    if (this.token) headers['Authorization'] = `Bearer ${this.token}`;
    return headers;
  }

  async login(username: string, password: string): Promise<{ access_token: string }> {
    console.log(`[BackendClient] Attempting login to: ${this.baseUrl}/auth/login`);
    console.log(`[BackendClient] Request body:`, { username, password: '***' });
    
    const res = await fetch(`${this.baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    
    console.log(`[BackendClient] Response status: ${res.status}`);
    
    if (!res.ok) {
      const text = await res.text();
      console.error(`[BackendClient] Login failed with status ${res.status}:`, text);
      throw new Error(text || `Login failed (${res.status})`);
    }
    const data = await res.json();
    console.log(`[BackendClient] Login successful, setting token`);
    this.token = data.access_token;
    return data;
  }

  async me(): Promise<any> {
    const res = await fetch(`${this.baseUrl}/auth/me`, {
      headers: { ...this.authHeaders() }
    });
    if (!res.ok) throw new Error('Nicht autorisiert');
    return res.json();
  }

  async listVehicles(): Promise<any[]> {
    console.log('[BackendClient] Fetching vehicles...');
    const res = await fetch(`${this.baseUrl}/vehicles`, { headers: { ...this.authHeaders() } });
    console.log('[BackendClient] Vehicles response status:', res.status);
    if (!res.ok) {
      const errorText = await res.text();
      console.error('[BackendClient] Vehicles error:', errorText);
      throw new Error('Fehler beim Laden der Fahrzeuge');
    }
    const data = await res.json();
    console.log('[BackendClient] Vehicles data:', data);
    return data;
  }

  async listVehicleTypes(): Promise<any[]> {
    const res = await fetch(`${this.baseUrl}/vehicle-types`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Fahrzeugtypen');
    return res.json();
  }

  async createVehicleType(data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/vehicle-types`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Erstellen des Fahrzeugtyps');
    return res.json();
  }

  async updateVehicleType(id: string, data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/vehicle-types/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Aktualisieren des Fahrzeugtyps');
    return res.json();
  }

  async deleteVehicleType(id: string): Promise<any> {
    const res = await fetch(`${this.baseUrl}/vehicle-types/${id}`, {
      method: 'DELETE',
      headers: { ...this.authHeaders() }
    });
    if (!res.ok) throw new Error('Fehler beim Löschen des Fahrzeugtyps');
    return res.json();
  }

  async logout(): Promise<void> {
    const res = await fetch(`${this.baseUrl}/auth/logout`, {
      method: 'POST',
      headers: { ...this.authHeaders() }
    });
    if (!res.ok) throw new Error('Fehler beim Abmelden');
    this.token = null;
  }

  async listChecklists(): Promise<any[]> {
    const res = await fetch(`${this.baseUrl}/checklists`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Checklisten');
    return res.json();
  }

  async listTuvDeadlines(): Promise<any[]> {
    console.log('[BackendClient] Fetching TÜV deadlines...');
    const res = await fetch(`${this.baseUrl}/tuv/deadlines`, { headers: { ...this.authHeaders() } });
    console.log('[BackendClient] TÜV deadlines response status:', res.status);
    if (!res.ok) {
      const errorText = await res.text();
      console.error('[BackendClient] TÜV deadlines error:', errorText);
      throw new Error('Fehler beim Laden der TÜV-Termine');
    }
    const data = await res.json();
    console.log('[BackendClient] TÜV deadlines data:', data);
    return data;
  }

  async pushActions(actions: any[]): Promise<{ accepted: number }> {
    const res = await fetch(`${this.baseUrl}/sync/actions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(actions)
    });
    if (!res.ok) throw new Error('Fehler beim Synchronisieren');
    return res.json();
  }

  async healthCheck(): Promise<{ status: string }> {
    const res = await fetch(`${this.baseUrl}/health`);
    if (!res.ok) throw new Error(`Health check failed (${res.status})`);
    return res.json();
  }

  async listFahrzeuggruppen(): Promise<any[]> {
    const res = await fetch(`${this.baseUrl}/fahrzeuggruppen`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Fahrzeuggruppen');
    return res.json();
  }

  async updateVehicle(id: string, data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/vehicles/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Aktualisieren des Fahrzeugs');
    return res.json();
  }

  async createVehicle(data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/vehicles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Erstellen des Fahrzeugs');
    return res.json();
  }

  async updateVehicleTuv(vehicleId: string, data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/vehicles/${vehicleId}/tuv`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Aktualisieren der TÜV-Daten');
    return res.json();
  }

  // Group management methods
  async listGroups(): Promise<any[]> {
    const res = await fetch(`${this.baseUrl}/groups`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Gruppen');
    const data = await res.json();
    return Array.isArray(data) ? data : (data.items || []);
  }

  async getGroup(id: string): Promise<any> {
    const res = await fetch(`${this.baseUrl}/groups/${id}`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Gruppe');
    return res.json();
  }

  async createGroup(data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/groups`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Erstellen der Gruppe');
    return res.json();
  }

  async updateGroup(id: string, data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/groups/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Aktualisieren der Gruppe');
    return res.json();
  }

  async deleteGroup(id: string): Promise<void> {
    const res = await fetch(`${this.baseUrl}/groups/${id}`, {
      method: 'DELETE',
      headers: { ...this.authHeaders() }
    });
    if (!res.ok) throw new Error('Fehler beim Löschen der Gruppe');
  }

  // Fahrzeuggruppe management methods
  async getFahrzeuggruppe(id: string): Promise<any> {
    const res = await fetch(`${this.baseUrl}/fahrzeuggruppen/${id}`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Fahrzeuggruppe');
    return res.json();
  }

  async createFahrzeuggruppe(data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/fahrzeuggruppen`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Erstellen der Fahrzeuggruppe');
    return res.json();
  }

  async updateFahrzeuggruppe(id: string, data: any): Promise<any> {
    const res = await fetch(`${this.baseUrl}/fahrzeuggruppen/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Fehler beim Aktualisieren der Fahrzeuggruppe');
    return res.json();
  }

  async deleteFahrzeuggruppe(id: string): Promise<void> {
    const res = await fetch(`${this.baseUrl}/fahrzeuggruppen/${id}`, {
      method: 'DELETE',
      headers: { ...this.authHeaders() }
    });
    if (!res.ok) throw new Error('Fehler beim Löschen der Fahrzeuggruppe');
  }

  async listUsers(): Promise<any[]> {
    const res = await fetch(`${this.baseUrl}/users`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Benutzer');
    const data = await res.json();
    return Array.isArray(data) ? data : (data.items || []);
  }
}

export const backendClient = new BackendClient();
