// Simple HTTP client for the FastAPI backend using global fetch (Node 18+)
export class BackendClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl?: string) {
    // Allow override via env BACKEND_URL; default to localhost:8000
    const envUrl = process.env.BACKEND_URL;
    this.baseUrl = (baseUrl || envUrl || 'http://localhost:8000').replace(/\/$/, '');
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
    const res = await fetch(`${this.baseUrl}/vehicles`, { headers: { ...this.authHeaders() } });
    if (!res.ok) throw new Error('Fehler beim Laden der Fahrzeuge');
    return res.json();
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
}

export const backendClient = new BackendClient();
