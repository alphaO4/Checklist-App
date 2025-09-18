// Groups Management Page Component

class GruppenPage {
  constructor() {
    this.gruppen = [];
    this.fahrzeuggruppen = [];
    this.isLoading = false;
    this.currentTab = 'main-groups'; // 'main-groups' or 'vehicle-groups'
  }

  async show() {
    const pageContent = document.getElementById('pageContent');
    pageContent.innerHTML = this.getLoadingHtml();
    
    try {
      this.isLoading = true;
      await this.loadData();
      this.render();
      this.attachEventListeners();
    } catch (error) {
      console.error('Failed to load groups:', error);
      window.appStore.addNotification('error', 'Fehler beim Laden der Gruppen');
      pageContent.innerHTML = '<div class="error">Fehler beim Laden der Gruppen</div>';
    } finally {
      this.isLoading = false;
    }
  }

  async loadData() {
    try {
      // Load both main groups and vehicle groups
      const [gruppenResponse, fahrzeuggruppenResponse] = await Promise.all([
        this.loadGruppen(),
        this.loadFahrzeuggruppen()
      ]);
    } catch (error) {
      console.error('Error loading group data:', error);
      throw error;
    }
  }

  async loadGruppen() {
    try {
      if (typeof window.electronAPI !== 'undefined') {
        this.gruppen = await window.electronAPI.listGroups() || [];
      }
    } catch (error) {
      console.error('Failed to load main groups:', error);
      this.gruppen = [];
    }
  }

  async loadFahrzeuggruppen() {
    try {
      if (typeof window.electronAPI !== 'undefined') {
        this.fahrzeuggruppen = await window.electronAPI.listFahrzeuggruppen() || [];
      }
    } catch (error) {
      console.error('Failed to load vehicle groups:', error);
      this.fahrzeuggruppen = [];
    }
  }

  render() {
    const pageContent = document.getElementById('pageContent');
    pageContent.innerHTML = this.getPageHtml();
  }

  getLoadingHtml() {
    return `
      <div class="loading-container">
        <div class="loading-spinner">
          <div class="spinner"></div>
          <p>Lade Gruppen...</p>
        </div>
      </div>
    `;
  }

  getPageHtml() {
    return `
      <div class="groups-page">
        <div class="page-header">
          <h2>🏢 Gruppenverwaltung</h2>
          <div class="header-actions">
            <button class="btn btn-primary" onclick="gruppenPage.createGroup()">
              <span class="icon">➕</span> Neue Hauptgruppe
            </button>
            <button class="btn btn-secondary" onclick="gruppenPage.createVehicleGroup()">
              <span class="icon">🚗</span> Neue Fahrzeuggruppe
            </button>
          </div>
        </div>

        <div class="tabs">
          <button class="tab-button ${this.currentTab === 'main-groups' ? 'active' : ''}" 
                  onclick="gruppenPage.switchTab('main-groups')">
            Hauptgruppen (${this.gruppen.length})
          </button>
          <button class="tab-button ${this.currentTab === 'vehicle-groups' ? 'active' : ''}" 
                  onclick="gruppenPage.switchTab('vehicle-groups')">
            Fahrzeuggruppen (${this.fahrzeuggruppen.length})
          </button>
        </div>

        <div class="tab-content">
          ${this.currentTab === 'main-groups' ? this.renderMainGroups() : this.renderVehicleGroups()}
        </div>
      </div>
    `;
  }

  renderMainGroups() {
    if (this.gruppen.length === 0) {
      return `
        <div class="empty-state">
          <h3>Keine Hauptgruppen vorhanden</h3>
          <p>Erstellen Sie eine neue Hauptgruppe, um zu beginnen.</p>
          <button class="btn btn-primary" onclick="gruppenPage.createGroup()">
            Erste Hauptgruppe erstellen
          </button>
        </div>
      `;
    }

    return `
      <div class="groups-grid">
        ${this.gruppen.map(gruppe => `
          <div class="group-card">
            <div class="group-header">
              <h3>${gruppe.name || 'Unbenannte Gruppe'}</h3>
              <div class="group-actions">
                <button class="btn-icon" onclick="gruppenPage.editGroup(${gruppe.id})" title="Bearbeiten">
                  ✏️
                </button>
                <button class="btn-icon delete" onclick="gruppenPage.deleteGroup(${gruppe.id})" title="Löschen">
                  🗑️
                </button>
              </div>
            </div>
            <div class="group-info">
              <p><strong>Gruppenleiter:</strong> ${gruppe.gruppenleiter?.username || 'Nicht zugewiesen'}</p>
              <p><strong>Erstellt:</strong> ${new Date(gruppe.created_at).toLocaleDateString('de-DE')}</p>
              <p><strong>Fahrzeuggruppen:</strong> ${this.getVehicleGroupsForGroup(gruppe.id).length}</p>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  }

  renderVehicleGroups() {
    if (this.fahrzeuggruppen.length === 0) {
      return `
        <div class="empty-state">
          <h3>Keine Fahrzeuggruppen vorhanden</h3>
          <p>Erstellen Sie eine neue Fahrzeuggruppe, um Fahrzeuge zu organisieren.</p>
          <button class="btn btn-primary" onclick="gruppenPage.createVehicleGroup()">
            Erste Fahrzeuggruppe erstellen
          </button>
        </div>
      `;
    }

    return `
      <div class="groups-grid">
        ${this.fahrzeuggruppen.map(fahrzeuggruppe => `
          <div class="group-card">
            <div class="group-header">
              <h3>${fahrzeuggruppe.name || 'Unbenannte Fahrzeuggruppe'}</h3>
              <div class="group-actions">
                <button class="btn-icon" onclick="gruppenPage.editVehicleGroup(${fahrzeuggruppe.id})" title="Bearbeiten">
                  ✏️
                </button>
                <button class="btn-icon delete" onclick="gruppenPage.deleteVehicleGroup(${fahrzeuggruppe.id})" title="Löschen">
                  🗑️
                </button>
              </div>
            </div>
            <div class="group-info">
              <p><strong>Hauptgruppe:</strong> ${this.getMainGroupName(fahrzeuggruppe.gruppe_id)}</p>
              <p><strong>Erstellt:</strong> ${new Date(fahrzeuggruppe.created_at).toLocaleDateString('de-DE')}</p>
              <p><strong>Fahrzeuge:</strong> ${this.getVehicleCountForGroup(fahrzeuggruppe.id)}</p>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  }

  getVehicleGroupsForGroup(gruppeId) {
    return this.fahrzeuggruppen.filter(fg => fg.gruppe_id === gruppeId);
  }

  getMainGroupName(gruppeId) {
    const gruppe = this.gruppen.find(g => g.id === gruppeId);
    return gruppe ? gruppe.name : 'Nicht zugeordnet';
  }

  getVehicleCountForGroup(fahrzeuggruppeId) {
    // This would need to be loaded from the vehicles data
    // For now, return placeholder
    return '—';
  }

  switchTab(tab) {
    this.currentTab = tab;
    this.render();
  }

  createGroup() {
    if (typeof window.groupEditModal !== 'undefined') {
      window.groupEditModal.show(null, 'main-group');
    }
  }

  createVehicleGroup() {
    if (typeof window.groupEditModal !== 'undefined') {
      window.groupEditModal.show(null, 'vehicle-group');
    }
  }

  editGroup(groupId) {
    if (typeof window.groupEditModal !== 'undefined') {
      window.groupEditModal.show(groupId, 'main-group');
    }
  }

  editVehicleGroup(groupId) {
    if (typeof window.groupEditModal !== 'undefined') {
      window.groupEditModal.show(groupId, 'vehicle-group');
    }
  }

  async deleteGroup(groupId) {
    const gruppe = this.gruppen.find(g => g.id === groupId);
    if (!gruppe) return;

    if (!confirm(`Sind Sie sicher, dass Sie die Gruppe "${gruppe.name}" löschen möchten?`)) {
      return;
    }

    try {
      if (typeof window.electronAPI !== 'undefined') {
        await window.electronAPI.deleteGroup(groupId);
        window.appStore.addNotification('success', 'Gruppe erfolgreich gelöscht');
        await this.refreshData();
      }
    } catch (error) {
      console.error('Failed to delete group:', error);
      window.appStore.addNotification('error', 'Fehler beim Löschen der Gruppe');
    }
  }

  async deleteVehicleGroup(groupId) {
    const fahrzeuggruppe = this.fahrzeuggruppen.find(fg => fg.id === groupId);
    if (!fahrzeuggruppe) return;

    if (!confirm(`Sind Sie sicher, dass Sie die Fahrzeuggruppe "${fahrzeuggruppe.name}" löschen möchten?`)) {
      return;
    }

    try {
      if (typeof window.electronAPI !== 'undefined') {
        await window.electronAPI.deleteFahrzeuggruppe(groupId);
        window.appStore.addNotification('success', 'Fahrzeuggruppe erfolgreich gelöscht');
        await this.refreshData();
      }
    } catch (error) {
      console.error('Failed to delete vehicle group:', error);
      window.appStore.addNotification('error', 'Fehler beim Löschen der Fahrzeuggruppe');
    }
  }

  async refreshData() {
    try {
      await this.loadData();
      this.render();
    } catch (error) {
      console.error('Failed to refresh group data:', error);
    }
  }

  attachEventListeners() {
    // Event listeners are handled through onclick attributes in the HTML
    // This method is kept for consistency with other page components
  }
}

// Export singleton instance
window.gruppenPage = window.gruppenPage || new GruppenPage();
