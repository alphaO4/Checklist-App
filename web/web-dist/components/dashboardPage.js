// Dashboard page component

class DashboardPage {
  constructor() {
    this.unsubscribe = null;
  }

  async render() {
    // Load data if needed
    await this.loadDashboardData();

    const state = window.appStore.getState();
    
    // Ensure tuvTermine is always an array
    if (!Array.isArray(state.tuvTermine)) {
      console.warn('tuvTermine is not an array, setting to empty array');
      state.tuvTermine = [];
    }
    
    const stats = this.calculateStats(state);

    return `
      <div class="dashboard">
        <div class="dashboard-header">
          <h2>Dashboard</h2>
          <button class="btn btn-secondary" onclick="dashboardPage.refreshData()">
            <span class="icon">🔄</span> Aktualisieren
          </button>
        </div>
        
        <div class="dashboard-stats">
          ${this.renderStatsCards(stats)}
        </div>

        <div class="dashboard-content">
          <div class="dashboard-section">
            <h3>Anstehende TÜV-Termine</h3>
            ${this.renderUpcomingTuv(state.tuvTermine)}
          </div>
          
          <div class="dashboard-section">
            <h3>Letzte Aktivitäten</h3>
            ${this.renderRecentActivity()}
          </div>
        </div>

        ${this.renderErrors(state.errors)}
      </div>
    `;
  }

  renderStatsCards(stats) {
    return `
      <div class="stat-card ${stats.openChecklists > 0 ? 'has-action' : ''}">
        <h3>Offene Checklisten</h3>
        <p class="stat-number">${stats.openChecklists}</p>
        <span class="stat-description">Zu bearbeiten</span>
      </div>
      
      <div class="stat-card ${stats.tuvThisWeek > 0 ? 'has-warning' : ''}">
        <h3>TÜV-Termine diese Woche</h3>
        <p class="stat-number">${stats.tuvThisWeek}</p>
        <span class="stat-description">Fällig in 7 Tagen</span>
      </div>
      
      <div class="stat-card ${stats.expiredTuv > 0 ? 'has-error' : ''}">
        <h3>Überfällige TÜV</h3>
        <p class="stat-number">${stats.expiredTuv}</p>
        <span class="stat-description">Sofort handeln</span>
      </div>
      
      <div class="stat-card">
        <h3>Fahrzeuge gesamt</h3>
        <p class="stat-number">${stats.totalVehicles}</p>
        <span class="stat-description">Im System</span>
      </div>
    `;
  }

  renderUpcomingTuv(tuvTermine) {
    if (!tuvTermine || tuvTermine.length === 0) {
      return '<p class="no-data">Keine TÜV-Termine gefunden.</p>';
    }

    // Sort by ablauf_datum and take first 5
    const upcoming = tuvTermine
      .filter(t => t.ablauf_datum || t.ablaufDatum)
      .sort((a, b) => {
        const dateA = new Date(a.ablauf_datum || a.ablaufDatum);
        const dateB = new Date(b.ablauf_datum || b.ablaufDatum);
        return dateA - dateB;
      })
      .slice(0, 5);

    if (upcoming.length === 0) {
      return '<p class="no-data">Keine anstehenden TÜV-Termine.</p>';
    }

    return `
      <div class="tuv-list">
        ${upcoming.map(t => this.renderTuvItem(t)).join('')}
      </div>
      <a href="#tuv" class="view-all-link" onclick="window.app.handleNavigation(event)">Alle TÜV-Termine anzeigen →</a>
    `;
  }

  renderTuvItem(tuvTermin) {
    const ablaufDatum = new Date(tuvTermin.ablauf_datum || tuvTermin.ablaufDatum);
    const today = new Date();
    const daysUntil = Math.ceil((ablaufDatum - today) / (1000 * 60 * 60 * 24));
    
    let statusClass = 'current';
    let statusText = 'Aktuell';
    
    if (daysUntil < 0) {
      statusClass = 'expired';
      statusText = `${Math.abs(daysUntil)} Tage überfällig`;
    } else if (daysUntil <= 7) {
      statusClass = 'warning';
      statusText = `${daysUntil} Tage verbleibend`;
    } else if (daysUntil <= 30) {
      statusClass = 'reminder';
      statusText = `${daysUntil} Tage verbleibend`;
    }

    return `
      <div class="tuv-item ${statusClass}">
        <div class="tuv-info">
          <strong>${tuvTermin.kennzeichen || 'Unbekannt'}</strong>
          <span class="tuv-date">${ablaufDatum.toLocaleDateString('de-DE')}</span>
        </div>
        <div class="tuv-status">
          <span class="status-badge ${statusClass}">${statusText}</span>
        </div>
      </div>
    `;
  }

  renderRecentActivity() {
    // Placeholder for recent activity
    return `
      <div class="activity-list">
        <div class="activity-item">
          <span class="activity-time">Heute 14:30</span>
          <span class="activity-text">Checkliste für MTF-1 abgeschlossen</span>
        </div>
        <div class="activity-item">
          <span class="activity-time">Heute 10:15</span>
          <span class="activity-text">TÜV-Termin für RTB-2 aktualisiert</span>
        </div>
        <div class="activity-item">
          <span class="activity-time">Gestern 16:45</span>
          <span class="activity-text">Neues Fahrzeug FR-3 hinzugefügt</span>
        </div>
      </div>
    `;
  }

  renderErrors(errors) {
    const errorKeys = Object.keys(errors);
    if (errorKeys.length === 0) {
      return '';
    }

    return `
      <div class="dashboard-errors">
        <h4>Fehler beim Laden der Daten:</h4>
        <ul>
          ${errorKeys.map(key => `<li>${key}: ${errors[key]}</li>`).join('')}
        </ul>
      </div>
    `;
  }

  calculateStats(state) {
    const stats = {
      openChecklists: 0,
      tuvThisWeek: 0,
      expiredTuv: 0,
      totalVehicles: state.vehicles.length
    };

    // Calculate TÜV statistics
    const today = new Date();
    const nextWeek = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);

    state.tuvTermine.forEach(t => {
      const ablaufDatum = new Date(t.ablauf_datum || t.ablaufDatum);
      
      if (ablaufDatum < today) {
        stats.expiredTuv++;
      } else if (ablaufDatum <= nextWeek) {
        stats.tuvThisWeek++;
      }
    });

    // TODO: Calculate open checklists from actual data
    stats.openChecklists = state.checklists.filter(c => !c.template).length;

    return stats;
  }

  async loadDashboardData() {
    try {
      await window.dataManager.loadAllData();
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
      // Ensure state has default values even if loading fails
      const state = window.appStore.getState();
      if (!Array.isArray(state.tuvTermine)) {
        window.appStore.setTuvTermine([]);
      }
      if (!Array.isArray(state.vehicles)) {
        window.appStore.setVehicles([]);
      }
      if (!Array.isArray(state.checklists)) {
        window.appStore.setChecklists([]);
      }
    }
  }

  async refreshData() {
    try {
      window.appStore.addNotification('info', 'Aktualisiere Dashboard...');
      await window.dataManager.refreshAllData();
      
      // Re-render the page
      const pageContent = document.getElementById('pageContent');
      if (pageContent) {
        pageContent.innerHTML = await this.render();
      }
      
      window.appStore.addNotification('success', 'Dashboard aktualisiert');
    } catch (error) {
      console.error('Failed to refresh dashboard:', error);
      window.appStore.addNotification('error', 'Fehler beim Aktualisieren des Dashboards');
    }
  }

  mount() {
    // Subscribe to state changes for reactive updates
    this.unsubscribe = window.appStore.subscribe((state) => {
      // Re-render if needed
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
window.dashboardPage = window.dashboardPage || new DashboardPage();
