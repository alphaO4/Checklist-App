
/**
 * Web API Adapter - Replaces Electron APIs with HTTP calls
 */

// Create mock electronAPI that uses HTTP instead of IPC
window.electronAPI = {
  // Authentication
  async login(username, password) {
    return await window.configUtils.fetchBackend('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
  },

  async getCurrentUser() {
    return await window.configUtils.fetchBackend('/auth/me');
  },

  // Vehicle Types
  async listVehicleTypes() {
    return await window.configUtils.fetchBackend('/vehicle-types');
  },

  async createVehicleType(data) {
    return await window.configUtils.fetchBackend('/vehicle-types', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async updateVehicleType(id, data) {
    return await window.configUtils.fetchBackend(`/vehicle-types/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async deleteVehicleType(id) {
    return await window.configUtils.fetchBackend(`/vehicle-types/${id}`, {
      method: 'DELETE'
    });
  },

  // Vehicles
  async listVehicles() {
    return await window.configUtils.fetchBackend('/vehicles');
  },

  async createVehicle(data) {
    return await window.configUtils.fetchBackend('/vehicles', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async updateVehicle(id, data) {
    return await window.configUtils.fetchBackend(`/vehicles/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async deleteVehicle(id) {
    return await window.configUtils.fetchBackend(`/vehicles/${id}`, {
      method: 'DELETE'
    });
  },

  // Groups
  async listGroups() {
    return await window.configUtils.fetchBackend('/groups');
  },

  async createGroup(data) {
    return await window.configUtils.fetchBackend('/groups', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  // Vehicle Groups  
  async listVehicleGroups() {
    return await window.configUtils.fetchBackend('/fahrzeuggruppen');
  },

  // TÜV Management
  async listTuvTermine() {
    return await window.configUtils.fetchBackend('/tuv');
  },

  async updateTuvTermin(id, data) {
    return await window.configUtils.fetchBackend(`/tuv/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  // Health Check
  async healthCheck() {
    return await window.configUtils.fetchBackend('/health');
  },

  // Token storage (using localStorage for web)
  async getStoredToken() {
    return localStorage.getItem('authToken');
  },

  async storeToken(token) {
    localStorage.setItem('authToken', token);
  },

  async clearStoredToken() {
    localStorage.removeItem('authToken');
  },

  // User info with stored token
  async me() {
    const token = await this.getStoredToken();
    if (!token) {
      throw new Error('No stored token');
    }
    return await window.configUtils.fetchBackend('/auth/me', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  },

  // Event handlers (stub implementations for web)
  onTuvAlert(callback) {
    console.log('TÜV alert handler registered (web mode)');
    // Return cleanup function
    return () => console.log('TÜV alert handler cleaned up');
  },

  onNetworkChange(callback) {
    console.log('Network change handler registered (web mode)');
    return () => console.log('Network change handler cleaned up');
  },

  onSyncUpdate(callback) {
    console.log('Sync update handler registered (web mode)');
    return () => console.log('Sync update handler cleaned up');
  }
};

// Initialize after DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  console.log('🌐 Web API adapter loaded');
});
