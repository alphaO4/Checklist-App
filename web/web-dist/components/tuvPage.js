// TÜV page component

class TuvPage {
  constructor() {
    this.unsubscribe = null;
  }

  async render() {
    await this.loadTuvTermine();
    
    const state = window.appStore.getState();
    const error = state.errors.tuvTermine;

    return `
      <div class="tuv-page">
        <div class="page-header">
          <h2>TÜV-Termine</h2>
          <div class="page-actions">
            <button class="btn btn-secondary" onclick="tuvPage.refreshData()">
              <span class="icon">🔄</span> Aktualisieren
            </button>
            <button class="btn btn-primary" onclick="tuvPage.addTuvTermin()">
              <span class="icon">➕</span> Termin hinzufügen
            </button>
          </div>
        </div>

        ${error ? this.renderError(error) : ''}
        
        <div class="tuv-filters">
          ${this.renderFilters()}
        </div>

        <div class="tuv-content">
          ${this.renderTuvBoard(state.tuvTermine, state.vehicles)}
        </div>
      </div>
    `;
  }

  renderError(error) {
    return `
      <div class="error-message">
        <p><strong>Fehler beim Laden der TÜV-Termine:</strong> ${error}</p>
        <button class="btn btn-secondary" onclick="tuvPage.refreshData()">
          Erneut versuchen
        </button>
      </div>
    `;
  }

  renderFilters() {
    return `
      <div class="filter-bar">
        <div class="filter-group">
          <label for="statusFilter">Status:</label>
          <select id="statusFilter" onchange="tuvPage.filterByStatus(this.value)">
            <option value="">Alle</option>
            <option value="expired">Überfällig</option>
            <option value="warning">Warnung (≤ 7 Tage)</option>
            <option value="reminder">Erinnerung (≤ 30 Tage)</option>
            <option value="current">Aktuell</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label for="typeFilter">Fahrzeugtyp:</label>
          <select id="typeFilter" onchange="tuvPage.filterByType(this.value)">
            <option value="">Alle</option>
            <option value="MTF">MTF</option>
            <option value="RTB">RTB</option>
            <option value="FR">FR</option>
          </select>
        </div>
        
        <button class="btn btn-secondary" onclick="tuvPage.clearFilters()">
          Filter zurücksetzen
        </button>
      </div>
    `;
  }

  renderTuvBoard(tuvTermine, vehicles) {
    if (!tuvTermine || tuvTermine.length === 0) {
      return `
        <div class="no-data">
          <p>Keine TÜV-Termine gefunden.</p>
          <button class="btn btn-primary" onclick="tuvPage.addTuvTermin()">
            Ersten Termin hinzufügen
          </button>
        </div>
      `;
    }

    // Group by status
    const grouped = this.groupTuvTermineByStatus(tuvTermine, vehicles);

    return `
      <div class="tuv-board">
        <div class="tuv-column expired">
          <h3 class="column-header">
            <span class="status-icon">⚠️</span>
            Überfällig (${grouped.expired.length})
          </h3>
          <div class="tuv-cards">
            ${grouped.expired.map(item => this.renderTuvCard(item)).join('')}
          </div>
        </div>
        
        <div class="tuv-column warning">
          <h3 class="column-header">
            <span class="status-icon">🔶</span>
            Warnung - 7 Tage (${grouped.warning.length})
          </h3>
          <div class="tuv-cards">
            ${grouped.warning.map(item => this.renderTuvCard(item)).join('')}
          </div>
        </div>
        
        <div class="tuv-column reminder">
          <h3 class="column-header">
            <span class="status-icon">🔔</span>
            Erinnerung - 30 Tage (${grouped.reminder.length})
          </h3>
          <div class="tuv-cards">
            ${grouped.reminder.map(item => this.renderTuvCard(item)).join('')}
          </div>
        </div>
        
        <div class="tuv-column current">
          <h3 class="column-header">
            <span class="status-icon">✅</span>
            Aktuell (${grouped.current.length})
          </h3>
          <div class="tuv-cards">
            ${grouped.current.map(item => this.renderTuvCard(item)).join('')}
          </div>
        </div>
      </div>
    `;
  }

  renderTuvCard(item) {
    const { tuvTermin, vehicle, status, daysUntil } = item;
    
    let statusText = '';
    if (status === 'expired') {
      statusText = `${Math.abs(daysUntil)} Tage überfällig`;
    } else if (status === 'warning' || status === 'reminder') {
      statusText = `${daysUntil} Tage verbleibend`;
    } else {
      statusText = 'Aktuell';
    }

    const ablaufDatum = new Date(tuvTermin.ablauf_datum || tuvTermin.ablaufDatum);
    const vehicleType = vehicle?.fahrzeugtyp?.name || vehicle?.typ || '?';

    return `
      <div class="tuv-card ${status}" data-tuv-id="${tuvTermin.id}">
        <div class="card-header">
          <h4 class="vehicle-kennzeichen">${vehicle?.kennzeichen || 'Unbekannt'}</h4>
          <span class="vehicle-type-badge ${vehicleType.toLowerCase() || 'unknown'}">
            ${vehicleType}
          </span>
        </div>
        
        <div class="card-content">
          <div class="tuv-date">
            <span class="date-label">TÜV-Ablauf:</span>
            <span class="date-value">${ablaufDatum.toLocaleDateString('de-DE')}</span>
          </div>
          
          <div class="tuv-status">
            <span class="status-text">${statusText}</span>
          </div>
          
          ${tuvTermin.letzte_pruefung ? `
            <div class="last-check">
              <span class="check-label">Letzte Prüfung:</span>
              <span class="check-value">${new Date(tuvTermin.letzte_pruefung).toLocaleDateString('de-DE')}</span>
            </div>
          ` : ''}
        </div>
        
        <div class="card-actions">
          <button class="btn btn-sm btn-primary" onclick="tuvPage.updateTuvTermin('${tuvTermin.id}')">
            Bearbeiten
          </button>
          ${status === 'expired' || status === 'warning' ? `
            <button class="btn btn-sm btn-warning" onclick="tuvPage.scheduleNewTuv('${tuvTermin.id}')">
              TÜV buchen
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }

  groupTuvTermineByStatus(tuvTermine, vehicles) {
    const today = new Date();
    const grouped = {
      expired: [],
      warning: [],
      reminder: [],
      current: []
    };

    tuvTermine.forEach(t => {
      const vehicle = vehicles.find(v => v.id === (t.fahrzeug_id || t.fahrzeugId));
      const ablaufDatum = new Date(t.ablauf_datum || t.ablaufDatum);
      const daysUntil = Math.ceil((ablaufDatum - today) / (1000 * 60 * 60 * 24));
      
      let status = 'current';
      if (daysUntil < 0) {
        status = 'expired';
      } else if (daysUntil <= 7) {
        status = 'warning';
      } else if (daysUntil <= 30) {
        status = 'reminder';
      }

      grouped[status].push({
        tuvTermin: t,
        vehicle: vehicle,
        status: status,
        daysUntil: daysUntil
      });
    });

    // Sort each group by days until expiry
    Object.keys(grouped).forEach(key => {
      grouped[key].sort((a, b) => a.daysUntil - b.daysUntil);
    });

    return grouped;
  }

  async loadTuvTermine() {
    try {
      await window.dataManager.loadTuvTermine();
      await window.dataManager.loadVehicles(); // For vehicle info
    } catch (error) {
      console.error('Failed to load TÜV termine:', error);
    }
  }

  async refreshData() {
    try {
      window.appStore.addNotification('info', 'Aktualisiere TÜV-Termine...');
      await window.dataManager.loadTuvTermine(true);
      await window.dataManager.loadVehicles(true);
      
      // Re-render the page
      const pageContent = document.getElementById('pageContent');
      if (pageContent) {
        pageContent.innerHTML = await this.render();
      }
      
      window.appStore.addNotification('success', 'TÜV-Termine aktualisiert');
    } catch (error) {
      console.error('Failed to refresh TÜV termine:', error);
      window.appStore.addNotification('error', 'Fehler beim Aktualisieren der TÜV-Termine');
    }
  }

  addTuvTermin() {
    window.appStore.addNotification('info', 'TÜV-Termin hinzufügen - Funktion wird implementiert');
    // TODO: Open add TÜV termin dialog/form
  }

  updateTuvTermin(tuvId) {
    window.appStore.addNotification('info', `TÜV-Termin ${tuvId} bearbeiten - Funktion wird implementiert`);
    // TODO: Open edit TÜV termin dialog/form
  }

  scheduleNewTuv(tuvId) {
    window.appStore.addNotification('info', `Neuen TÜV für ${tuvId} buchen - Funktion wird implementiert`);
    // TODO: Open schedule new TÜV dialog
  }

  filterByStatus(status) {
    window.appStore.addNotification('info', `Filter nach Status: ${status || 'Alle'}`);
    // TODO: Implement filtering
  }

  filterByType(type) {
    window.appStore.addNotification('info', `Filter nach Typ: ${type || 'Alle'}`);
    // TODO: Implement filtering
  }

  clearFilters() {
    const statusFilter = document.getElementById('statusFilter');
    const typeFilter = document.getElementById('typeFilter');
    
    if (statusFilter) statusFilter.value = '';
    if (typeFilter) typeFilter.value = '';
    
    window.appStore.addNotification('info', 'Filter zurückgesetzt');
    // TODO: Clear filters and re-render
  }

  mount() {
    this.unsubscribe = window.appStore.subscribe((state) => {
      // Re-render if TÜV data changes
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
window.tuvPage = window.tuvPage || new TuvPage();
