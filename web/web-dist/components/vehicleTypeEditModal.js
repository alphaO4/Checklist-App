class VehicleTypeEditModal {
  constructor() {
    this.isVisible = false;
    this.isEditing = false;
    this.vehicleType = null;
    this.loading = false;
  }

  show(vehicleType = null) {
    this.isEditing = !!vehicleType;
    this.vehicleType = vehicleType || {
      name: '',
      beschreibung: '',
      aktiv: true
    };

    this.render();
    this.isVisible = true;
    document.body.style.overflow = 'hidden';
  }

  hide() {
    const modal = document.getElementById('vehicleTypeEditModal');
    if (modal) {
      modal.remove();
    }
    this.isVisible = false;
    document.body.style.overflow = '';
  }

  render() {
    // Remove existing modal if any
    const existingModal = document.getElementById('vehicleTypeEditModal');
    if (existingModal) {
      existingModal.remove();
    }

    const modalTitle = this.isEditing ? 'Fahrzeugtyp bearbeiten' : 'Neuen Fahrzeugtyp erstellen';
    const submitButtonText = this.isEditing ? 'Aktualisieren' : 'Erstellen';

    const modalHTML = `
      <div id="vehicleTypeEditModal" class="modal-overlay">
        <div class="modal-content">
          <div class="modal-header">
            <h3>${modalTitle}</h3>
            <button type="button" class="modal-close" id="closeVehicleTypeModal">
              <i class="icon-x"></i>
            </button>
          </div>

          <form id="vehicleTypeForm" class="modal-body">
            <div class="form-group">
              <label for="vehicleTypeName">Name *</label>
              <input 
                type="text" 
                id="vehicleTypeName" 
                name="name" 
                value="${this.vehicleType.name || ''}" 
                required 
                maxlength="100"
                placeholder="z.B. MTF, RTB, LF 10"
              >
              <small class="form-help">Der Name des Fahrzeugtyps (z.B. MTF, RTB, LF 10)</small>
            </div>

            <div class="form-group">
              <label for="vehicleTypeDescription">Beschreibung</label>
              <textarea 
                id="vehicleTypeDescription" 
                name="beschreibung" 
                rows="3"
                maxlength="500"
                placeholder="Optionale Beschreibung des Fahrzeugtyps..."
              >${this.vehicleType.beschreibung || ''}</textarea>
              <small class="form-help">Optionale Beschreibung oder zusätzliche Informationen</small>
            </div>

            <div class="form-group">
              <div class="checkbox-group">
                <label class="checkbox-label">
                  <input 
                    type="checkbox" 
                    id="vehicleTypeActive" 
                    name="aktiv" 
                    ${this.vehicleType.aktiv !== false ? 'checked' : ''}
                  >
                  <span class="checkbox-custom"></span>
                  Fahrzeugtyp ist aktiv
                </label>
                <small class="form-help">Inaktive Fahrzeugtypen werden bei der Fahrzeugauswahl nicht angezeigt</small>
              </div>
            </div>

            <div class="form-actions">
              <button type="button" id="cancelVehicleTypeBtn" class="btn btn-secondary">
                Abbrechen
              </button>
              <button type="submit" id="saveVehicleTypeBtn" class="btn btn-primary" ${this.loading ? 'disabled' : ''}>
                ${this.loading ? 'Speichere...' : submitButtonText}
              </button>
            </div>
          </form>
        </div>
      </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
    this.setupEventListeners();

    // Focus the name input
    setTimeout(() => {
      const nameInput = document.getElementById('vehicleTypeName');
      if (nameInput) {
        nameInput.focus();
        nameInput.select();
      }
    }, 100);
  }

  setupEventListeners() {
    // Close modal events
    const closeBtn = document.getElementById('closeVehicleTypeModal');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.hide());
    }

    const cancelBtn = document.getElementById('cancelVehicleTypeBtn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => this.hide());
    }

    // Click outside to close
    const modalOverlay = document.getElementById('vehicleTypeEditModal');
    if (modalOverlay) {
      modalOverlay.addEventListener('click', (e) => {
        if (e.target === modalOverlay) {
          this.hide();
        }
      });
    }

    // Escape key to close
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && this.isVisible) {
        this.hide();
      }
    });

    // Form submission
    const form = document.getElementById('vehicleTypeForm');
    if (form) {
      form.addEventListener('submit', (e) => {
        e.preventDefault();
        this.saveVehicleType();
      });
    }

    // Name input validation
    const nameInput = document.getElementById('vehicleTypeName');
    if (nameInput) {
      nameInput.addEventListener('input', () => {
        this.validateForm();
      });
    }
  }

  validateForm() {
    const nameInput = document.getElementById('vehicleTypeName');
    const saveBtn = document.getElementById('saveVehicleTypeBtn');
    
    if (nameInput && saveBtn) {
      const isValid = nameInput.value.trim().length > 0;
      saveBtn.disabled = !isValid || this.loading;
    }
  }

  async saveVehicleType() {
    if (this.loading) return;

    const form = document.getElementById('vehicleTypeForm');
    if (!form) return;

    const formData = new FormData(form);
    const data = {
      name: formData.get('name').trim(),
      beschreibung: formData.get('beschreibung').trim() || null,
      aktiv: formData.has('aktiv')
    };

    // Validation
    if (!data.name) {
      window.appStore.addNotification('error', 'Name ist erforderlich');
      return;
    }

    if (data.name.length > 100) {
      window.appStore.addNotification('error', 'Name darf maximal 100 Zeichen lang sein');
      return;
    }

    if (data.beschreibung && data.beschreibung.length > 500) {
      window.appStore.addNotification('error', 'Beschreibung darf maximal 500 Zeichen lang sein');
      return;
    }

    try {
      this.setLoading(true);

      let result;
      if (this.isEditing) {
        result = await window.dataManager.updateVehicleType(this.vehicleType.id, data);
      } else {
        result = await window.dataManager.createVehicleType(data);
      }

      // Refresh the vehicle types page
      if (window.fahrzeugtypenPage) {
        await window.fahrzeugtypenPage.refresh();
      }

      this.hide();

    } catch (error) {
      console.error('Failed to save vehicle type:', error);
      // Error notification is handled by dataManager
    } finally {
      this.setLoading(false);
    }
  }

  setLoading(loading) {
    this.loading = loading;
    
    const saveBtn = document.getElementById('saveVehicleTypeBtn');
    if (saveBtn) {
      saveBtn.disabled = loading;
      saveBtn.textContent = loading ? 'Speichere...' : (this.isEditing ? 'Aktualisieren' : 'Erstellen');
    }

    // Disable form inputs during loading
    const form = document.getElementById('vehicleTypeForm');
    if (form) {
      const inputs = form.querySelectorAll('input, textarea, button');
      inputs.forEach(input => {
        if (input.id !== 'saveVehicleTypeBtn') {
          input.disabled = loading;
        }
      });
    }
  }
}

// Initialize and expose globally
window.vehicleTypeEditModal = new VehicleTypeEditModal();
