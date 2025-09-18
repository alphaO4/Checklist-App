class FahrzeugtypenPage {
  constructor() {
    this.fahrzeugtypen = [];
    this.loading = false;
    this.filter = {
      search: '',
      aktiv: 'all' // 'all', 'active', 'inactive'
    };
  }

  async render() {
    await this.loadFahrzeugtypen();
    
    return `
      <div class="fahrzeugtypen-page">
        <div class="page-header">
          <h2>Fahrzeugtypen verwalten</h2>
          <button id="addVehicleTypeBtn" class="btn btn-primary" onclick="fahrzeugtypenPage.openVehicleTypeModal()">
            <i class="icon-plus"></i> Fahrzeugtyp hinzufügen
          </button>
        </div>

        <div class="page-controls">
          <div class="search-controls">
            <div class="search-group">
              <input type="text" id="searchInput" placeholder="Fahrzeugtypen suchen..." value="${this.filter.search}">
              <button id="clearSearchBtn" class="btn btn-icon ${this.filter.search ? '' : 'hidden'}">
                <i class="icon-x"></i>
              </button>
            </div>
            
            <div class="filter-group">
              <label for="statusFilter">Status:</label>
              <select id="statusFilter">
                <option value="all" ${this.filter.aktiv === 'all' ? 'selected' : ''}>Alle</option>
                <option value="active" ${this.filter.aktiv === 'active' ? 'selected' : ''}>Aktiv</option>
                <option value="inactive" ${this.filter.aktiv === 'inactive' ? 'selected' : ''}>Inaktiv</option>
              </select>
            </div>
          </div>
        </div>

        <div class="fahrzeugtypen-content">
          ${this.loading ? this.renderLoading() : this.renderFahrzeugtypen()}
        </div>
      </div>
    `;
  }

  renderLoading() {
    return '<div class="loading">Fahrzeugtypen werden geladen...</div>';
  }

  renderFahrzeugtypen() {
    const filteredTypes = this.getFilteredFahrzeugtypen();

    if (filteredTypes.length === 0) {
      return `
        <div class="empty-state">
          <div class="empty-icon">🚗</div>
          <h3>Keine Fahrzeugtypen gefunden</h3>
          <p>Es wurden keine Fahrzeugtypen gefunden, die Ihren Suchkriterien entsprechen.</p>
          ${this.filter.search || this.filter.aktiv !== 'all' ? 
            '<button id="clearFiltersBtn" class="btn btn-secondary" onclick="fahrzeugtypenPage.clearFilters()">Filter zurücksetzen</button>' : 
            '<button id="addFirstVehicleTypeBtn" class="btn btn-primary" onclick="fahrzeugtypenPage.openVehicleTypeModal()">Ersten Fahrzeugtyp erstellen</button>'
          }
        </div>
      `;
    }

    return `
      <div class="fahrzeugtypen-grid">
        ${filteredTypes.map(typ => this.renderVehicleTypeCard(typ)).join('')}
      </div>
    `;
  }

  renderVehicleTypeCard(typ) {
    const statusClass = typ.aktiv ? 'active' : 'inactive';
    const statusText = typ.aktiv ? 'Aktiv' : 'Inaktiv';
    
    return `
      <div class="vehicle-type-card ${statusClass}" data-id="${typ.id}">
        <div class="vehicle-type-header">
          <h3 class="vehicle-type-name">${typ.name}</h3>
          <div class="vehicle-type-status ${statusClass}">
            <span class="status-dot"></span>
            ${statusText}
          </div>
        </div>
        
        ${typ.beschreibung ? `
          <div class="vehicle-type-description">
            <p>${typ.beschreibung}</p>
          </div>
        ` : ''}
        
        <div class="vehicle-type-meta">
          <small class="created-date">
            Erstellt: ${new Date(typ.created_at).toLocaleDateString('de-DE')}
          </small>
        </div>
        
        <div class="vehicle-type-actions">
          <button class="btn btn-icon btn-secondary" onclick="fahrzeugtypenPage.editVehicleType('${typ.id}')" title="Bearbeiten">
            <i class="icon-edit"></i>
          </button>
          <button class="btn btn-icon btn-danger" onclick="fahrzeugtypenPage.deleteVehicleType('${typ.id}')" title="Löschen">
            <i class="icon-trash"></i>
          </button>
        </div>
      </div>
    `;
  }

  getFilteredFahrzeugtypen() {
    return this.fahrzeugtypen.filter(typ => {
      // Search filter
      if (this.filter.search) {
        const searchTerm = this.filter.search.toLowerCase();
        const matchesSearch = 
          typ.name.toLowerCase().includes(searchTerm) ||
          (typ.beschreibung && typ.beschreibung.toLowerCase().includes(searchTerm));
        if (!matchesSearch) return false;
      }

      // Status filter
      if (this.filter.aktiv === 'active' && !typ.aktiv) return false;
      if (this.filter.aktiv === 'inactive' && typ.aktiv) return false;

      return true;
    });
  }

  async loadFahrzeugtypen() {
    try {
      this.loading = true;
      this.fahrzeugtypen = await window.dataManager.loadVehicleTypes(true);
    } catch (error) {
      console.error('Failed to load vehicle types:', error);
      window.appStore.addNotification('error', 'Fehler beim Laden der Fahrzeugtypen');
    } finally {
      this.loading = false;
    }
  }

  mount() {
    console.log('Mount called for fahrzeugtypenPage');
    // Use setTimeout to ensure DOM elements are ready
    setTimeout(() => {
      this.setupEventListeners();
    }, 0);
  }

  setupEventListeners() {
    console.log('Setting up event listeners for fahrzeugtypenPage');
    
    // Search input
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.filter.search = e.target.value;
        this.updateContent();
        this.updateClearSearchButton();
      });
    }

    // Clear search button
    const clearSearchBtn = document.getElementById('clearSearchBtn');
    if (clearSearchBtn) {
      clearSearchBtn.addEventListener('click', () => {
        this.filter.search = '';
        const searchInput = document.getElementById('searchInput');
        if (searchInput) searchInput.value = '';
        this.updateContent();
        this.updateClearSearchButton();
      });
    }

    // Status filter
    const statusFilter = document.getElementById('statusFilter');
    if (statusFilter) {
      statusFilter.addEventListener('change', (e) => {
        this.filter.aktiv = e.target.value;
        this.updateContent();
      });
    }
  }

  updateContent() {
    const contentContainer = document.querySelector('.fahrzeugtypen-content');
    if (contentContainer) {
      contentContainer.innerHTML = this.renderFahrzeugtypen();
    }
  }

  updateClearSearchButton() {
    const clearBtn = document.getElementById('clearSearchBtn');
    if (clearBtn) {
      if (this.filter.search) {
        clearBtn.classList.remove('hidden');
      } else {
        clearBtn.classList.add('hidden');
      }
    }
  }

  clearFilters() {
    this.filter.search = '';
    this.filter.aktiv = 'all';
    
    const searchInput = document.getElementById('searchInput');
    if (searchInput) searchInput.value = '';
    
    const statusFilter = document.getElementById('statusFilter');
    if (statusFilter) statusFilter.value = 'all';
    
    this.updateContent();
    this.updateClearSearchButton();
  }

  openVehicleTypeModal(vehicleType = null) {
    console.log('openVehicleTypeModal called with:', vehicleType);
    console.log('window.vehicleTypeEditModal exists:', !!window.vehicleTypeEditModal);
    
    if (window.vehicleTypeEditModal) {
      window.vehicleTypeEditModal.show(vehicleType);
    } else {
      console.error('vehicleTypeEditModal not found on window object');
      window.appStore.addNotification('error', 'Modal-Komponente nicht gefunden. Bitte Seite neu laden.');
    }
  }

  async editVehicleType(id) {
    const vehicleType = this.fahrzeugtypen.find(typ => typ.id == id);
    if (vehicleType) {
      this.openVehicleTypeModal(vehicleType);
    }
  }

  async deleteVehicleType(id) {
    const vehicleType = this.fahrzeugtypen.find(typ => typ.id == id);
    if (!vehicleType) return;

    const confirmed = confirm(
      `Möchten Sie den Fahrzeugtyp "${vehicleType.name}" wirklich löschen?\n\n` +
      `Hinweis: Falls Fahrzeuge diesen Typ verwenden, wird er nur deaktiviert.`
    );

    if (!confirmed) return;

    try {
      await window.dataManager.deleteVehicleType(id);
      await this.loadFahrzeugtypen();
      this.updateContent();
    } catch (error) {
      console.error('Failed to delete vehicle type:', error);
    }
  }

  // Called when a vehicle type is created/updated from the modal
  async refresh() {
    await this.loadFahrzeugtypen();
    this.updateContent();
  }
}

// Initialize and expose globally
window.fahrzeugtypenPage = new FahrzeugtypenPage();
