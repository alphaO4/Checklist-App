// Fahrzeuge page component

class FahrzeugePage {
  constructor() {
    this.unsubscribe = null;
  }

  async render() {
    await this.loadFahrzeuge();
    
    const state = window.appStore.getState();
    const error = state.errors.vehicles;

    return `
      <div class="fahrzeuge-page">
        <div class="page-header">
          <h2>Fahrzeuge</h2>
          <div class="page-actions">
            <button class="btn btn-secondary" onclick="fahrzeugePage.refreshData()">
              <span class="icon">🔄</span> Aktualisieren
            </button>
            <button class="btn btn-primary" onclick="fahrzeugePage.addVehicle()">
              <span class="icon">➕</span> Fahrzeug hinzufügen
            </button>
          </div>
        </div>

        ${error ? this.renderError(error) : ''}
        
        <div class="fahrzeuge-content">
          ${this.renderFahrzeugeTable(state.vehicles)}
        </div>
      </div>
    `;
  }

  renderError(error) {
    return `
      <div class="error-message">
        <p><strong>Fehler beim Laden der Fahrzeuge:</strong> ${error}</p>
        <button class="btn btn-secondary" onclick="fahrzeugePage.refreshData()">
          Erneut versuchen
        </button>
      </div>
    `;
  }

  renderFahrzeugeTable(vehicles) {
    if (!vehicles || vehicles.length === 0) {
      return `
        <div class="no-data">
          <p>Keine Fahrzeuge gefunden.</p>
          <button class="btn btn-primary" onclick="fahrzeugePage.addVehicle()">
            Erstes Fahrzeug hinzufügen
          </button>
        </div>
      `;
    }

    return `
      <div class="data-table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>Kennzeichen</th>
              <th>Typ</th>
              <th>Fahrzeuggruppe</th>
              <th>TÜV-Status</th>
              <th>Letzte Prüfung</th>
              <th>Aktionen</th>
            </tr>
          </thead>
          <tbody>
            ${vehicles.map(v => this.renderFahrzeugRow(v)).join('')}
          </tbody>
        </table>
      </div>
    `;
  }

  renderFahrzeugRow(vehicle) {
    const tuvStatus = this.getTuvStatus(vehicle);
    const lastCheck = this.getLastCheckDate(vehicle);
    const vehicleType = vehicle.fahrzeugtyp?.name || vehicle.typ || 'Unbekannt';

    return `
      <tr class="vehicle-row" data-vehicle-id="${vehicle.id}">
        <td class="vehicle-kennzeichen">
          <strong>${vehicle.kennzeichen || 'Unbekannt'}</strong>
        </td>
        <td class="vehicle-typ">
          <span class="vehicle-type-badge ${vehicleType.toLowerCase() || 'unknown'}">
            ${vehicleType}
          </span>
        </td>
        <td class="vehicle-group">
          ${this.getFahrzeuggruppeNameById(vehicle.fahrzeuggruppe_id || vehicle.fahrzeuggruppeId) || 'Nicht zugeordnet'}
        </td>
        <td class="tuv-status">
          <span class="status-badge ${tuvStatus.class}">
            ${tuvStatus.text}
          </span>
        </td>
        <td class="last-check">
          ${lastCheck || 'Nie'}
        </td>
        <td class="vehicle-actions">
          <button class="btn btn-sm btn-primary" onclick="fahrzeugePage.startChecklist('${vehicle.id}')">
            Prüfung
          </button>
          <button class="btn btn-sm btn-secondary" onclick="fahrzeugePage.editVehicle('${vehicle.id}')">
            Bearbeiten
          </button>
          <button class="btn btn-sm btn-danger" onclick="fahrzeugePage.deleteVehicle('${vehicle.id}')">
            Löschen
          </button>
        </td>
      </tr>
    `;
  }

  getTuvStatus(vehicle) {
    const state = window.appStore.getState();
    const tuvTermin = state.tuvTermine.find(t => (t.fahrzeug_id || t.fahrzeugId) === vehicle.id);
    
    if (!tuvTermin || !tuvTermin.ablauf_datum) {
      return { class: 'unknown', text: 'Unbekannt' };
    }

    const ablaufDatum = new Date(tuvTermin.ablauf_datum);
    const today = new Date();
    const daysUntil = Math.ceil((ablaufDatum - today) / (1000 * 60 * 60 * 24));
    
    if (daysUntil < 0) {
      return { class: 'expired', text: `${Math.abs(daysUntil)} Tage überfällig` };
    } else if (daysUntil <= 7) {
      return { class: 'warning', text: `${daysUntil} Tage verbleibend` };
    } else if (daysUntil <= 30) {
      return { class: 'reminder', text: `${daysUntil} Tage verbleibend` };
    } else {
      return { class: 'current', text: 'Aktuell' };
    }
  }

  getLastCheckDate(vehicle) {
    // TODO: Get last checklist execution date for this vehicle
    // For now, return placeholder
    return 'Vor 3 Tagen';
  }

  getFahrzeuggruppeNameById(id) {
    // TODO: Look up fahrzeuggruppe name by ID
    // For now, return placeholder
    if (!id) return null;
    // Convert to string for substring operation
    const idStr = String(id);
    return `Gruppe ${idStr.substring(0, 8)}`;
  }

  async loadFahrzeuge() {
    try {
      await window.dataManager.loadVehicles();
      await window.dataManager.loadTuvTermine(); // For TÜV status
    } catch (error) {
      console.error('Failed to load fahrzeuge:', error);
    }
  }

  async refreshData() {
    try {
      window.appStore.addNotification('info', 'Aktualisiere Fahrzeuge...');
      await window.dataManager.loadVehicles(true);
      await window.dataManager.loadTuvTermine(true);
      
      // Re-render the page
      const pageContent = document.getElementById('pageContent');
      if (pageContent) {
        pageContent.innerHTML = await this.render();
      }
      
      window.appStore.addNotification('success', 'Fahrzeuge aktualisiert');
    } catch (error) {
      console.error('Failed to refresh fahrzeuge:', error);
      window.appStore.addNotification('error', 'Fehler beim Aktualisieren der Fahrzeuge');
    }
  }

  addVehicle() {
    window.appStore.addNotification('info', 'Fahrzeug hinzufügen - Funktion wird implementiert');
    // TODO: Open add vehicle dialog/form
  }

  editVehicle(vehicleId) {
    window.appStore.addNotification('info', `Fahrzeug ${vehicleId} bearbeiten - Funktion wird implementiert`);
    // TODO: Open edit vehicle dialog/form
  }

  deleteVehicle(vehicleId) {
    if (confirm('Fahrzeug wirklich löschen?')) {
      window.appStore.addNotification('info', `Fahrzeug ${vehicleId} löschen - Funktion wird implementiert`);
      // TODO: Delete vehicle via API
    }
  }

  startChecklist(vehicleId) {
    window.appStore.addNotification('info', `Checkliste für Fahrzeug ${vehicleId} starten - Funktion wird implementiert`);
    // TODO: Start checklist for vehicle
  }

  mount() {
    this.unsubscribe = window.appStore.subscribe((state) => {
      // Re-render if vehicles data changes
    });
  }

  unmount() {
    if (this.unsubscribe) {
      this.unsubscribe();
      this.unsubscribe = null;
    }
  }
}

// Export singleton instance
window.fahrzeugePage = window.fahrzeugePage || new FahrzeugePage();
