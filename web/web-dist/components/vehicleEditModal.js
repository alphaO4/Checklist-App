// Vehicle Edit Modal Component

class VehicleEditModal {
  constructor() {
    this.vehicle = null;
    this.fahrzeugtypen = [];
    this.fahrzeuggruppen = [];
    this.isLoading = false;
  }

  async show(vehicleId) {
    try {
      this.isLoading = true;
      this.render(); // Show loading state immediately
      
      // Load vehicle data, types, and groups
      await Promise.all([
        this.loadVehicle(vehicleId),
        this.loadFahrzeugtypen(),
        this.loadFahrzeuggruppen()
      ]);
      
    } catch (error) {
      console.error('Failed to load vehicle edit data:', error);
      window.appStore.addNotification('error', 'Fehler beim Laden der Fahrzeugdaten');
    } finally {
      this.isLoading = false;
      this.render(); // Re-render to show form and hide loading
      this.attachEventListeners();
    }
  }

  async loadVehicle(vehicleId) {
    if (!vehicleId) {
      // Creating a new vehicle - initialize with default values
      this.vehicle = {
        id: null,
        kennzeichen: '',
        fahrzeugtyp_id: null,
        fahrzeuggruppe_id: null,
        tuv_data: null
      };
      return;
    }

    const state = window.appStore.getState();
    const token = state.token;
    
    const response = await window.configUtils.fetchBackend(`/vehicles/${vehicleId}`, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    if (!response.ok) {
      throw new Error('Failed to load vehicle data');
    }
    
    this.vehicle = await response.json();
    
    // Also load TÜV data
      const tuvResponse = await window.configUtils.fetchBackend(`/vehicles/${vehicleId}/tuv`, {
        headers: {
          'Content-Type': 'application/json'
        }
      });
      if (tuvResponse.ok) {
        const tuvData = await tuvResponse.json();
        this.vehicle.tuv_data = tuvData.tuv_data;
      }
    }

  async loadFahrzeugtypen() {
    const state = window.appStore.getState();
    const token = state.token;
    
    const response = await window.configUtils.fetchBackend('/vehicle-types', {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    if (response.ok) {
      this.fahrzeugtypen = await response.json();
    }
  }

  async loadFahrzeuggruppen() {
    const state = window.appStore.getState();
    const token = state.token;
    
    const response = await window.configUtils.fetchBackend('/fahrzeuggruppen', {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    if (response.ok) {
      this.fahrzeuggruppen = await response.json();
    }
  }

  render() {
    const isEdit = this.vehicle && this.vehicle.id;
    const title = isEdit ? 'Fahrzeug bearbeiten' : 'Neues Fahrzeug hinzufügen';
    
    const modalHtml = `
      <div class="modal-overlay" id="vehicleEditModal">
        <div class="modal-content vehicle-edit-modal">
          <div class="modal-header">
            <h3>${title}</h3>
            <button class="modal-close" onclick="vehicleEditModal.hide()">×</button>
          </div>
          
          <div class="modal-body">
            ${this.isLoading ? this.renderLoading() : this.renderForm()}
          </div>
          
          <div class="modal-footer">
            <button class="btn btn-secondary" onclick="vehicleEditModal.hide()">
              Abbrechen
            </button>
            <button class="btn btn-primary" onclick="vehicleEditModal.save()" ${this.isLoading ? 'disabled' : ''}>
              ${isEdit ? 'Aktualisieren' : 'Erstellen'}
            </button>
          </div>
        </div>
      </div>
    `;

    // Remove existing modal
    const existingModal = document.getElementById('vehicleEditModal');
    if (existingModal) {
      existingModal.remove();
    }

    // Add new modal
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Show modal
    setTimeout(() => {
      const modal = document.getElementById('vehicleEditModal');
      if (modal) {
        modal.classList.add('show');
      }
    }, 10);
  }

  renderLoading() {
    return `
      <div class="loading-spinner">
        <div class="spinner"></div>
        <p>Lade Fahrzeugdaten...</p>
      </div>
    `;
  }

  renderForm() {
    if (!this.vehicle) {
      return '<p>Fehler beim Laden der Fahrzeugdaten</p>';
    }

    const tuvData = this.vehicle.tuv_data || {};
    const ablaufDatum = tuvData.ablauf_datum ? new Date(tuvData.ablauf_datum).toISOString().split('T')[0] : '';
    const letztePruefung = tuvData.letzte_pruefung ? new Date(tuvData.letzte_pruefung).toISOString().split('T')[0] : '';

    return `
      <form id="vehicleEditForm" class="vehicle-edit-form">
        <div class="form-section">
          <h4>Fahrzeugdaten</h4>
          
          <div class="form-group">
            <label for="kennzeichen">Kennzeichen *</label>
            <input 
              type="text" 
              id="kennzeichen" 
              name="kennzeichen" 
              value="${this.vehicle.kennzeichen || ''}" 
              required
              placeholder="z.B. FW-MT-01"
            >
          </div>

          <div class="form-group">
            <label for="fahrzeugtyp_id">Fahrzeugtyp *</label>
            <select id="fahrzeugtyp_id" name="fahrzeugtyp_id" required>
              <option value="">Typ auswählen</option>
              ${this.fahrzeugtypen.map(typ => `
                <option value="${typ.id}" ${typ.id === this.vehicle.fahrzeugtyp_id ? 'selected' : ''}>
                  ${typ.name} - ${typ.beschreibung || ''}
                </option>
              `).join('')}
            </select>
          </div>

          <div class="form-group">
            <label for="fahrzeuggruppe_id">Fahrzeuggruppe *</label>
            <select id="fahrzeuggruppe_id" name="fahrzeuggruppe_id" required>
              <option value="">Gruppe auswählen</option>
              ${this.fahrzeuggruppen.map(gruppe => `
                <option value="${gruppe.id}" ${gruppe.id === this.vehicle.fahrzeuggruppe_id ? 'selected' : ''}>
                  ${gruppe.name}
                </option>
              `).join('')}
            </select>
          </div>
        </div>

        <div class="form-section">
          <h4>TÜV-Daten</h4>
          
          <div class="form-group">
            <label for="ablauf_datum">TÜV Ablaufdatum</label>
            <input 
              type="date" 
              id="ablauf_datum" 
              name="ablauf_datum" 
              value="${ablaufDatum}"
            >
          </div>

          <div class="form-group">
            <label for="letzte_pruefung">Letzte TÜV-Prüfung</label>
            <input 
              type="date" 
              id="letzte_pruefung" 
              name="letzte_pruefung" 
              value="${letztePruefung}"
            >
          </div>

          ${tuvData.status ? `
            <div class="form-group">
              <label>Aktueller TÜV-Status</label>
              <div class="tuv-status-display">
                <span class="status-badge ${tuvData.status}">
                  ${this.getTuvStatusText(tuvData.status, tuvData.days_remaining)}
                </span>
              </div>
            </div>
          ` : ''}
        </div>
      </form>
    `;
  }

  getTuvStatusText(status, daysRemaining) {
    switch (status) {
      case 'current':
        return 'Aktuell';
      case 'warning':
        return `${daysRemaining} Tage verbleibend`;
      case 'expired':
        return `${Math.abs(daysRemaining)} Tage überfällig`;
      default:
        return 'Unbekannt';
    }
  }

  attachEventListeners() {
    // Close modal when clicking outside
    const modal = document.getElementById('vehicleEditModal');
    if (modal) {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          this.hide();
        }
      });
    }

    // Escape key to close
    document.addEventListener('keydown', this.handleEscapeKey.bind(this));
  }

  handleEscapeKey(e) {
    if (e.key === 'Escape') {
      this.hide();
    }
  }

  async save() {
    try {
      const form = document.getElementById('vehicleEditForm');
      if (!form) return;

      const formData = new FormData(form);
      const vehicleData = {
        kennzeichen: formData.get('kennzeichen'),
        fahrzeugtyp_id: parseInt(formData.get('fahrzeugtyp_id')),
        fahrzeuggruppe_id: parseInt(formData.get('fahrzeuggruppe_id'))
      };

      const tuvData = {
        fahrzeug_id: this.vehicle.id,
        ablauf_datum: formData.get('ablauf_datum') || null,
        letzte_pruefung: formData.get('letzte_pruefung') || null
      };

      // Clean up empty dates
      if (tuvData.ablauf_datum === '') tuvData.ablauf_datum = null;
      if (tuvData.letzte_pruefung === '') tuvData.letzte_pruefung = null;

      const state = window.appStore.getState();
      const token = state.token;

      console.log('Save operation - Auth state:', {
        isAuthenticated: state.isAuthenticated,
        hasToken: !!token,
        tokenLength: token ? token.length : 0,
        user: state.user
      });

      if (!token) {
        throw new Error('Kein Authentifizierungstoken verfügbar. Bitte melden Sie sich erneut an.');
      }

      const isEdit = this.vehicle && this.vehicle.id;
      
      // Create or update vehicle data
      const endpoint = isEdit 
        ? `/vehicles/${this.vehicle.id}`
        : '/vehicles';
      const method = isEdit ? 'PUT' : 'POST';

      const vehicleResponse = await window.configUtils.fetchBackend(endpoint, {
        method,
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(vehicleData)
      });
      if (!vehicleResponse.ok) {
        if (vehicleResponse.status === 401) {
          // Token expired or invalid - prompt for re-login
          window.appStore.logout();
          throw new Error('Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut an.');
        }
        const error = await vehicleResponse.json();
        throw new Error(error.detail || `Failed to ${isEdit ? 'update' : 'create'} vehicle`);
      }
      const savedVehicle = await vehicleResponse.json();

      // Update TÜV data (only if vehicle has TÜV dates and we have a vehicle ID)
      if ((tuvData.ablauf_datum || tuvData.letzte_pruefung) && (isEdit || savedVehicle.id)) {
        const vehicleId = isEdit ? this.vehicle.id : savedVehicle.id;
        tuvData.fahrzeug_id = vehicleId;
        
        const tuvResponse = await window.configUtils.fetchBackend(`/vehicles/${vehicleId}/tuv`, {
          method: (isEdit && this.vehicle.tuv_data) ? 'PUT' : 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(tuvData)
        });
        if (!tuvResponse.ok) {
          if (tuvResponse.status === 401) {
            // Token expired or invalid - prompt for re-login
            window.appStore.logout();
            throw new Error('Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut an.');
          }
          console.warn('Failed to update TÜV data:', await tuvResponse.text());
        }
      }

      // Success
      const successMessage = isEdit ? 'Fahrzeug erfolgreich aktualisiert' : 'Fahrzeug erfolgreich erstellt';
      window.appStore.addNotification('success', successMessage);
      this.hide();
      
      // Refresh vehicle list
      if (window.fahrzeugePage && window.fahrzeugePage.refreshData) {
        await window.fahrzeugePage.refreshData();
      }

    } catch (error) {
      console.error('Failed to save vehicle:', error);
      window.appStore.addNotification('error', `Fehler beim Speichern: ${error.message}`);
    }
  }

  hide() {
    const modal = document.getElementById('vehicleEditModal');
    if (modal) {
      modal.classList.remove('show');
      setTimeout(() => {
        modal.remove();
      }, 300);
    }

    // Remove escape key listener
    document.removeEventListener('keydown', this.handleEscapeKey.bind(this));
  }
}

// Export singleton instance
window.vehicleEditModal = window.vehicleEditModal || new VehicleEditModal();
