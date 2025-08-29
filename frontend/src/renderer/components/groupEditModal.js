// Group Edit Modal Component

class GroupEditModal {
  constructor() {
    this.group = null;
    this.groupType = null; // 'main-group' or 'vehicle-group'
    this.availableGroups = [];
    this.availableUsers = [];
    this.isLoading = false;
  }

  async show(groupId, groupType) {
    try {
      this.groupType = groupType;
      this.isLoading = true;
      this.render(); // Show loading state immediately
      
      // Load required data
      await Promise.all([
        this.loadGroup(groupId),
        this.loadAvailableGroups(),
        this.loadAvailableUsers()
      ]);
      
    } catch (error) {
      console.error('Failed to load group edit data:', error);
      window.appStore.addNotification('error', 'Fehler beim Laden der Gruppendaten');
    } finally {
      this.isLoading = false;
      this.render(); // Re-render to show form and hide loading
      this.attachEventListeners();
    }
  }

  async loadGroup(groupId) {
    if (!groupId) {
      this.group = null;
      return;
    }

    const state = window.appStore.getState();
    const token = state.token;

    if (!token) {
      throw new Error('Kein Authentifizierungstoken verfügbar. Bitte melden Sie sich erneut an.');
    }

    const endpoint = this.groupType === 'main-group' 
      ? `/groups/${groupId}`
      : `/fahrzeuggruppen/${groupId}`;

    const response = await window.configUtils.fetchBackend(endpoint, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      if (response.status === 401) {
        window.appStore.logout();
        throw new Error('Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut an.');
      }
      throw new Error('Failed to load group data');
    }

    this.group = await response.json();
  }

  async loadAvailableGroups() {
    // Load main groups for vehicle group assignment
    if (this.groupType === 'vehicle-group') {
      try {
        if (typeof window.electronAPI !== 'undefined') {
          this.availableGroups = await window.electronAPI.listGroups() || [];
        }
      } catch (error) {
        console.error('Failed to load available groups:', error);
        this.availableGroups = [];
      }
    }
  }

  async loadAvailableUsers() {
    // Load users for group leader assignment (main groups only)
    if (this.groupType === 'main-group') {
      try {
        const state = window.appStore.getState();
        const token = state.token;

        if (!token) {
          return;
        }

        const response = await window.configUtils.fetchBackend('/users', {
          headers: {
            'Content-Type': 'application/json'
          }
        });
        if (!response.ok) {
          if (response.status === 401) {
            window.appStore.logout();
            throw new Error('Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut an.');
          }
          throw new Error('Failed to load available users');
        }
        const data = await response.json();
        this.availableUsers = Array.isArray(data) ? data : (data.items || []);
      } catch (error) {
        console.error('Failed to load available users:', error);
        this.availableUsers = [];
      }
    }
  }

  render() {
    const isEdit = !!this.group;
    const title = this.getModalTitle(isEdit);

    const modalHtml = `
      <div class="modal-overlay" id="groupEditModal">
        <div class="modal-content group-edit-modal">
          <div class="modal-header">
            <h3>${title}</h3>
            <button class="modal-close" onclick="groupEditModal.hide()">×</button>
          </div>
          
          <div class="modal-body">
            ${this.isLoading ? this.renderLoading() : this.renderForm()}
          </div>
          
          <div class="modal-footer">
            <button class="btn btn-secondary" onclick="groupEditModal.hide()">
              Abbrechen
            </button>
            <button class="btn btn-primary" onclick="groupEditModal.save()" ${this.isLoading ? 'disabled' : ''}>
              ${isEdit ? 'Aktualisieren' : 'Erstellen'}
            </button>
          </div>
        </div>
      </div>
    `;

    // Remove existing modal
    const existingModal = document.getElementById('groupEditModal');
    if (existingModal) {
      existingModal.remove();
    }

    // Add new modal
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Show modal
    setTimeout(() => {
      const modal = document.getElementById('groupEditModal');
      if (modal) {
        modal.classList.add('show');
      }
    }, 10);
  }

  getModalTitle(isEdit) {
    if (this.groupType === 'main-group') {
      return isEdit ? 'Hauptgruppe bearbeiten' : 'Neue Hauptgruppe erstellen';
    } else {
      return isEdit ? 'Fahrzeuggruppe bearbeiten' : 'Neue Fahrzeuggruppe erstellen';
    }
  }

  renderLoading() {
    return `
      <div class="loading-spinner">
        <div class="spinner"></div>
        <p>Lade Gruppendaten...</p>
      </div>
    `;
  }

  renderForm() {
    if (this.groupType === 'main-group') {
      return this.renderMainGroupForm();
    } else {
      return this.renderVehicleGroupForm();
    }
  }

  renderMainGroupForm() {
    return `
      <form id="groupEditForm" class="group-form">
        <div class="form-group">
          <label for="groupName">Gruppenname *</label>
          <input 
            type="text" 
            id="groupName" 
            name="name" 
            value="${this.group?.name || ''}" 
            required
            placeholder="z.B. Löschzug 1, Einsatzabteilung"
          >
        </div>
        
        <div class="form-group">
          <label for="gruppenleiter">Gruppenleiter</label>
          <select id="gruppenleiter" name="gruppenleiter_id">
            <option value="">-- Kein Gruppenleiter zugewiesen --</option>
            ${this.availableUsers.map(user => `
              <option value="${user.id}" ${this.group?.gruppenleiter_id === user.id ? 'selected' : ''}>
                ${user.username} (${user.rolle})
              </option>
            `).join('')}
          </select>
        </div>
        
        <div class="form-group">
          <label for="groupDescription">Beschreibung</label>
          <textarea 
            id="groupDescription" 
            name="description" 
            rows="3"
            placeholder="Optionale Beschreibung der Gruppe"
          >${this.group?.description || ''}</textarea>
        </div>
      </form>
    `;
  }

  renderVehicleGroupForm() {
    return `
      <form id="groupEditForm" class="group-form">
        <div class="form-group">
          <label for="vehicleGroupName">Fahrzeuggruppen-Name *</label>
          <input 
            type="text" 
            id="vehicleGroupName" 
            name="name" 
            value="${this.group?.name || ''}" 
            required
            placeholder="z.B. Löschfahrzeuge, Rettungsfahrzeuge"
          >
        </div>
        
        <div class="form-group">
          <label for="parentGroup">Hauptgruppe *</label>
          <select id="parentGroup" name="gruppe_id" required>
            <option value="">-- Hauptgruppe auswählen --</option>
            ${this.availableGroups.map(gruppe => `
              <option value="${gruppe.id}" ${this.group?.gruppe_id === gruppe.id ? 'selected' : ''}>
                ${gruppe.name}
              </option>
            `).join('')}
          </select>
        </div>
        
        <div class="form-group">
          <label for="vehicleGroupDescription">Beschreibung</label>
          <textarea 
            id="vehicleGroupDescription" 
            name="description" 
            rows="3"
            placeholder="Optionale Beschreibung der Fahrzeuggruppe"
          >${this.group?.description || ''}</textarea>
        </div>
      </form>
    `;
  }

  async save() {
    try {
      const form = document.getElementById('groupEditForm');
      if (!form) return;

      const formData = new FormData(form);
      const data = {};
      
      // Collect form data
      for (let [key, value] of formData.entries()) {
        if (value.trim() === '') {
          data[key] = null;
        } else if (key.includes('_id')) {
          data[key] = parseInt(value);
        } else {
          data[key] = value.trim();
        }
      }

      const state = window.appStore.getState();
      const token = state.token;

      if (!token) {
        throw new Error('Kein Authentifizierungstoken verfügbar. Bitte melden Sie sich erneut an.');
      }

      console.log('Save operation - Auth state:', {
        isAuthenticated: state.isAuthenticated,
        hasToken: !!token,
        tokenLength: token ? token.length : 0,
        user: state.user
      });

      const isEdit = !!this.group;
      const endpoint = this.getApiEndpoint(isEdit);
      const method = isEdit ? 'PUT' : 'POST';

      const response = await window.configUtils.fetchBackend(endpoint, {
        method,
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      });

      if (!response.ok) {
        if (response.status === 401) {
          window.appStore.logout();
          throw new Error('Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut an.');
        }
        const error = await response.json();
        throw new Error(error.detail || 'Failed to save group');
      }

      // Success
      const groupTypeName = this.groupType === 'main-group' ? 'Hauptgruppe' : 'Fahrzeuggruppe';
      const action = isEdit ? 'aktualisiert' : 'erstellt';
      window.appStore.addNotification('success', `${groupTypeName} erfolgreich ${action}`);
      this.hide();
      
      // Refresh groups page
      if (window.gruppenPage && window.gruppenPage.refreshData) {
        await window.gruppenPage.refreshData();
      }

    } catch (error) {
      console.error('Failed to save group:', error);
      window.appStore.addNotification('error', `Fehler beim Speichern: ${error.message}`);
    }
  }

  getApiEndpoint(isEdit) {
    if (this.groupType === 'main-group') {
      return isEdit 
        ? `/groups/${this.group.id}`
        : '/groups';
    } else {
      return isEdit 
        ? `/fahrzeuggruppen/${this.group.id}`
        : '/fahrzeuggruppen';
    }
  }

  hide() {
    const modal = document.getElementById('groupEditModal');
    if (modal) {
      modal.classList.remove('show');
      setTimeout(() => {
        modal.remove();
      }, 300);
    }

    // Remove escape key listener
    document.removeEventListener('keydown', this.handleEscapeKey.bind(this));
  }

  handleEscapeKey(event) {
    if (event.key === 'Escape') {
      this.hide();
    }
  }

  attachEventListeners() {
    // Add escape key listener
    document.addEventListener('keydown', this.handleEscapeKey.bind(this));
  }
}

// Export singleton instance
window.groupEditModal = window.groupEditModal || new GroupEditModal();
