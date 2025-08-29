// Data manager for handling API calls and data caching

class DataManager {
  constructor() {
    this.cache = {
      vehicles: { data: [], lastFetch: 0 },
      checklists: { data: [], lastFetch: 0 },
      tuvTermine: { data: [], lastFetch: 0 }
    };
    this.CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
  }

  // Check if data is cached and fresh
  isCacheFresh(key) {
    const cached = this.cache[key];
    return cached && (Date.now() - cached.lastFetch) < this.CACHE_DURATION;
  }

  // Load vehicles with caching
  async loadVehicles(forceRefresh = false) {
    if (!forceRefresh && this.isCacheFresh('vehicles')) {
      window.appStore.setVehicles(this.cache.vehicles.data);
      return this.cache.vehicles.data;
    }

    try {
      window.appStore.clearError('vehicles');
      
      if (typeof window.electronAPI === 'undefined') {
        throw new Error('Electron API nicht verfügbar');
      }

      const response = await window.electronAPI.listVehicles();
      
      // Extract the items array from the paginated response
      const vehicles = Array.isArray(response) ? response : (response.items || []);
      
      // Update cache and store
      this.cache.vehicles = { data: vehicles, lastFetch: Date.now() };
      window.appStore.setVehicles(vehicles);
      
      return vehicles;
    } catch (error) {
      console.error('Failed to load vehicles:', error);
      const errorMessage = this.getErrorMessage(error);
      window.appStore.setError('vehicles', errorMessage);
      window.appStore.addNotification('error', `Fehler beim Laden der Fahrzeuge: ${errorMessage}`);
      
      // Return cached data if available
      if (this.cache.vehicles.data.length > 0) {
        window.appStore.setVehicles(this.cache.vehicles.data);
        return this.cache.vehicles.data;
      }
      
      throw error;
    }
  }

  // Load vehicle types
  async loadVehicleTypes(forceRefresh = false) {
    try {
      if (typeof window.electronAPI === 'undefined') {
        throw new Error('Electron API nicht verfügbar');
      }

      const vehicleTypes = await window.electronAPI.listVehicleTypes();
      return vehicleTypes;
    } catch (error) {
      console.error('Failed to load vehicle types:', error);
      throw error;
    }
  }

  // Create vehicle type
  async createVehicleType(data) {
    try {
      if (typeof window.electronAPI === 'undefined') {
        throw new Error('Electron API nicht verfügbar');
      }

      const newType = await window.electronAPI.createVehicleType(data);
      window.appStore.addNotification('success', `Fahrzeugtyp "${data.name}" wurde erstellt`);
      return newType;
    } catch (error) {
      console.error('Failed to create vehicle type:', error);
      const errorMessage = this.getErrorMessage(error);
      window.appStore.addNotification('error', `Fehler beim Erstellen des Fahrzeugtyps: ${errorMessage}`);
      throw error;
    }
  }

  // Load checklists with caching
  async loadChecklists(forceRefresh = false) {
    if (!forceRefresh && this.isCacheFresh('checklists')) {
      window.appStore.setChecklists(this.cache.checklists.data);
      return this.cache.checklists.data;
    }

    try {
      window.appStore.clearError('checklists');
      
      if (typeof window.electronAPI === 'undefined') {
        throw new Error('Electron API nicht verfügbar');
      }

      const response = await window.electronAPI.listChecklists();
      
      // Extract the items array from the paginated response
      const checklists = Array.isArray(response) ? response : (response.items || []);
      
      // Update cache and store
      this.cache.checklists = { data: checklists, lastFetch: Date.now() };
      window.appStore.setChecklists(checklists);
      
      return checklists;
    } catch (error) {
      console.error('Failed to load checklists:', error);
      const errorMessage = this.getErrorMessage(error);
      window.appStore.setError('checklists', errorMessage);
      window.appStore.addNotification('error', `Fehler beim Laden der Checklisten: ${errorMessage}`);
      
      // Return cached data if available
      if (this.cache.checklists.data.length > 0) {
        window.appStore.setChecklists(this.cache.checklists.data);
        return this.cache.checklists.data;
      }
      
      throw error;
    }
  }

  // Load TÜV deadlines with caching
  async loadTuvTermine(forceRefresh = false) {
    if (!forceRefresh && this.isCacheFresh('tuvTermine')) {
      window.appStore.setTuvTermine(this.cache.tuvTermine.data);
      return this.cache.tuvTermine.data;
    }

    try {
      window.appStore.clearError('tuvTermine');
      
      if (typeof window.electronAPI === 'undefined') {
        throw new Error('Electron API nicht verfügbar');
      }

      const response = await window.electronAPI.listTuv();
      
      // Extract the items array from the paginated response
      const tuvTermine = Array.isArray(response) ? response : (response.items || []);
      
      // Update cache and store
      this.cache.tuvTermine = { data: tuvTermine, lastFetch: Date.now() };
      window.appStore.setTuvTermine(tuvTermine);
      
      return tuvTermine;
    } catch (error) {
      console.error('Failed to load TÜV termine:', error);
      const errorMessage = this.getErrorMessage(error);
      window.appStore.setError('tuvTermine', errorMessage);
      window.appStore.addNotification('error', `Fehler beim Laden der TÜV-Termine: ${errorMessage}`);
      
      // Return cached data if available
      if (this.cache.tuvTermine.data.length > 0) {
        window.appStore.setTuvTermine(this.cache.tuvTermine.data);
        return this.cache.tuvTermine.data;
      }
      
      throw error;
    }
  }

  // Load all data
  async loadAllData(forceRefresh = false) {
    const promises = [
      this.loadVehicles(forceRefresh),
      this.loadChecklists(forceRefresh),
      this.loadTuvTermine(forceRefresh)
    ];

    const results = await Promise.allSettled(promises);
    
    // Check if any failed
    const failures = results.filter(r => r.status === 'rejected');
    if (failures.length > 0) {
      console.warn(`${failures.length} data loading operations failed`);
    }
    
    return {
      vehicles: results[0].status === 'fulfilled' ? results[0].value : [],
      checklists: results[1].status === 'fulfilled' ? results[1].value : [],
      tuvTermine: results[2].status === 'fulfilled' ? results[2].value : []
    };
  }

  // Clear all caches
  clearCache() {
    this.cache = {
      vehicles: { data: [], lastFetch: 0 },
      checklists: { data: [], lastFetch: 0 },
      tuvTermine: { data: [], lastFetch: 0 }
    };
  }

  // Refresh all data
  async refreshAllData() {
    this.clearCache();
    return await this.loadAllData(true);
  }

  getErrorMessage(error) {
    if (typeof error === 'string') {
      return error;
    }
    
    if (error?.message) {
      // Map common errors to German
      const errorMap = {
        'Unauthorized': 'Nicht autorisiert',
        'Not authenticated': 'Nicht angemeldet',
        'Network error': 'Netzwerkfehler',
        'Server error': 'Serverfehler',
        'Fehler beim Laden der Fahrzeuge': 'Fahrzeuge konnten nicht geladen werden',
        'Fehler beim Laden der Checklisten': 'Checklisten konnten nicht geladen werden',
        'Fehler beim Laden der TÜV-Termine': 'TÜV-Termine konnten nicht geladen werden'
      };

      return errorMap[error.message] || error.message;
    }

    return 'Ein unbekannter Fehler ist aufgetreten';
  }
}

// Export singleton instance
window.dataManager = window.dataManager || new DataManager();
