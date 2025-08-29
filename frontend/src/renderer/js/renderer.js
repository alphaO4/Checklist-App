// Renderer process script
// This file handles the UI logic and communicates with the main process via IPC

class App {
  constructor() {
    this.currentPage = null;
    this.init();
  }

  async init() {
    this.setupEventListeners();
    this.setupElectronListeners();
    this.subscribeToStore();
    
    // Try to restore session first
    const restored = await window.authManager.restoreSession();
    
    if (restored) {
      this.showMainApp();
    } else {
      this.updateConnectionStatus();
      await this.checkOfflineData();
    }
    
    console.log('Feuerwehr Checklist App initialized');
  }

  setupEventListeners() {
    // Login form
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
      loginForm.addEventListener('submit', this.handleLogin.bind(this));
    }

    // Logout button
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', this.handleLogout.bind(this));
    }

    // Offline work button
    const offlineWorkBtn = document.getElementById('offlineWorkBtn');
    if (offlineWorkBtn) {
      offlineWorkBtn.addEventListener('click', this.startOfflineMode.bind(this));
    }

    // Navigation
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
      link.addEventListener('click', this.handleNavigation.bind(this));
    });

    // Network status changes
    window.addEventListener('online', () => {
      window.appStore.setOnline(true);
      this.syncOfflineData();
    });

    window.addEventListener('offline', () => {
      window.appStore.setOnline(false);
    });
  }

  subscribeToStore() {
    // Subscribe to app store changes
    this.unsubscribeStore = window.appStore.subscribe((state) => {
      this.updateConnectionStatus();
      this.updateNotifications();
      this.updateUserMenu();
      
      // Update auth state
      if (state.isAuthenticated && !this.isMainAppVisible()) {
        this.showMainApp();
      } else if (!state.isAuthenticated && this.isMainAppVisible()) {
        this.showLoginScreen();
      }
    });
  }

  isMainAppVisible() {
    const appContent = document.getElementById('appContent');
    return appContent && appContent.style.display !== 'none';
  }

  showLoginScreen() {
    const welcomeScreen = document.getElementById('welcomeScreen');
    const appContent = document.getElementById('appContent');
    
    if (welcomeScreen && appContent) {
      welcomeScreen.style.display = 'block';
      appContent.style.display = 'none';
    }
  }

  setupElectronListeners() {
    // Check if we're in Electron environment
    if (typeof window.electronAPI !== 'undefined') {
      // TÜV deadline alerts
      window.electronAPI.onTuvAlert((event, data) => {
        this.showNotification('warning', `TÜV-Warnung: Fahrzeug ${data.kennzeichen} läuft in ${data.tageVerbleibend} Tagen ab!`);
      });

      // Sync updates
      window.electronAPI.onSyncUpdate((event, data) => {
        this.handleSyncUpdate(data);
      });

      // Network change events
      window.electronAPI.onNetworkChange((event, isOnline) => {
        this.isOnline = isOnline;
        this.updateConnectionStatus();
        if (isOnline) {
          this.syncOfflineData();
        }
      });
    }
  }

  updateConnectionStatus() {
    const state = window.appStore.getState();
    const statusIndicator = document.getElementById('statusIndicator');
    const statusText = document.getElementById('statusText');
    
    if (!statusIndicator || !statusText) return;

    if (state.isOnline) {
      statusIndicator.className = 'status-indicator online';
      statusText.textContent = 'Online';
    } else {
      statusIndicator.className = 'status-indicator offline';
      statusText.textContent = 'Offline';
    }
  }

  updateNotifications() {
    const state = window.appStore.getState();
    const notifications = document.getElementById('notifications');
    
    if (!notifications) return;

    // Clear existing notifications
    notifications.innerHTML = '';

    // Add current notifications
    state.notifications.forEach(notification => {
      const notificationElement = document.createElement('div');
      notificationElement.className = `notification ${notification.type}`;
      notificationElement.innerHTML = `
        <div class="notification-content">
          <p>${notification.message}</p>
          <button class="notification-close" onclick="window.appStore.removeNotification('${notification.id}')">×</button>
        </div>
      `;
      notifications.appendChild(notificationElement);
    });
  }

  async handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    if (!username || !password) {
      window.appStore.addNotification('error', 'Bitte geben Sie Benutzername und Passwort ein.');
      return;
    }

    // Show loading state
    const submitBtn = event.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = 'Anmeldung läuft...';
    submitBtn.disabled = true;

    try {
      const result = await window.authManager.login(username, password);
      
      if (result.success) {
        // Clear form
        document.getElementById('username').value = '';
        document.getElementById('password').value = '';
        // showMainApp will be called automatically via store subscription
      }
    } catch (error) {
      console.error('Login error:', error);
    } finally {
      // Reset button state
      submitBtn.textContent = originalText;
      submitBtn.disabled = false;
    }
  }

  async handleLogout(event) {
    event.preventDefault();
    
    // Show loading state
    const logoutBtn = event.target;
    const originalText = logoutBtn.textContent;
    logoutBtn.textContent = 'Wird abgemeldet...';
    logoutBtn.disabled = true;

    try {
      await window.authManager.logout();
      // showLoginScreen will be called automatically via store subscription
    } catch (error) {
      console.error('Logout error:', error);
      // Force logout even if there's an error
      window.appStore.logout();
    } finally {
      // Reset button state
      logoutBtn.textContent = originalText;
      logoutBtn.disabled = false;
    }
  }

  updateUserMenu() {
    const state = window.appStore.getState();
    const userMenu = document.getElementById('userMenu');
    const userName = document.getElementById('userName');
    
    if (userMenu && userName) {
      if (state.isAuthenticated && state.user) {
        userMenu.style.display = 'flex';
        userName.textContent = state.user.username;
      } else {
        userMenu.style.display = 'none';
      }
    }
  }

  showOfflineDialog() {
    const state = window.appStore.getState();
    
    if (!state.isOnline) {
      const authSection = document.getElementById('authSection');
      const offlineMode = document.getElementById('offlineMode');
      
      if (authSection && offlineMode) {
        authSection.style.display = 'none';
        offlineMode.style.display = 'block';
      }
    }
  }

  startOfflineMode() {
    // Create offline user
    const offlineUser = {
      id: 'offline',
      username: 'Offline-Benutzer',
      email: '',
      rolle: 'benutzer'
    };
    
    window.appStore.login(offlineUser, null);
    window.appStore.addNotification('info', 'Offline-Modus gestartet. Daten werden synchronisiert, sobald eine Verbindung verfügbar ist.');
  }

  showMainApp() {
    const welcomeScreen = document.getElementById('welcomeScreen');
    const appContent = document.getElementById('appContent');
    
    if (welcomeScreen && appContent) {
      welcomeScreen.style.display = 'none';
      appContent.style.display = 'flex';
    }
    
    // Load dashboard by default
    this.loadPage('dashboard');
  }

  handleNavigation(event) {
    event.preventDefault();
    
    const link = event.target;
    const page = link.getAttribute('href').substring(1); // Remove #
    
    // Update active nav
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    link.classList.add('active');
    
    // Load page
    this.loadPage(page);
  }

  async loadPage(pageName) {
    const pageContent = document.getElementById('pageContent');
    if (!pageContent) return;

    // Unmount current page component
    if (this.currentPage && this.currentPage.unmount) {
      this.currentPage.unmount();
    }

    // Show loading
    pageContent.innerHTML = '<div class="loading">Seite wird geladen...</div>';

    try {
      let pageComponent;
      let html;

      switch (pageName) {
        case 'dashboard':
          pageComponent = window.dashboardPage;
          html = await pageComponent.render();
          break;
        case 'checklists':
          html = this.getChecklistsHTML();
          await this.loadChecklists();
          break;
        case 'fahrzeuge':
          pageComponent = window.fahrzeugePage;
          html = await pageComponent.render();
          break;
        case 'tuv':
          pageComponent = window.tuvPage;
          html = await pageComponent.render();
          break;
        case 'reports':
          html = this.getReportsHTML();
          break;
        default:
          html = '<h2>Seite nicht gefunden</h2>';
      }

      pageContent.innerHTML = html;

      // Mount the new page component
      if (pageComponent && pageComponent.mount) {
        pageComponent.mount();
      }

      this.currentPage = pageComponent;

    } catch (error) {
      console.error('Failed to load page:', error);
      pageContent.innerHTML = `
        <div class="error-page">
          <h2>Fehler beim Laden der Seite</h2>
          <p>${error.message || 'Unbekannter Fehler'}</p>
          <button class="btn btn-primary" onclick="location.reload()">Seite neu laden</button>
        </div>
      `;
    }
  }

  getChecklistsHTML() {
    return `
      <div class="checklists">
        <h2>Checklisten</h2>
        <p>Hier können Sie Fahrzeug-Checklisten verwalten und durchführen.</p>
        <button class="btn btn-primary">Neue Checkliste</button>
        <ul id="checklists-list" class="list"></ul>
      </div>
    `;
  }

  getReportsHTML() {
    return `
      <div class="reports">
        <h2>Berichte</h2>
        <p>Berichte und Auswertungen der Fahrzeugprüfungen.</p>
        <button class="btn btn-primary">Bericht erstellen</button>
      </div>
    `;
  }

  async checkOfflineData() {
    if (typeof window.electronAPI !== 'undefined') {
      try {
        const pending = await window.electronAPI.getPendingActions();
        if (pending && pending.length > 0) {
          window.appStore.addNotification('info', `${pending.length} Offline-Aktion(en) warten auf Synchronisierung.`);
        }
      } catch (error) {
        console.error('Error checking offline data:', error);
      }
    }
  }

  async syncOfflineData() {
    const state = window.appStore.getState();
    if (!state.isOnline) return;
    
    console.log('Syncing offline data...');
    window.appStore.addNotification('info', 'Synchronisiere Offline-Daten...');
    
    try {
      if (typeof window.electronAPI === 'undefined') return;
      const pending = await window.electronAPI.getPendingActions();
      if (!pending || pending.length === 0) {
        window.appStore.addNotification('info', 'Keine Offline-Daten zu synchronisieren.');
        return;
      }
      const res = await window.electronAPI.pushActions(pending);
      if (res && res.accepted >= 0) {
        // Mark all as synced optimistically
        for (const a of pending) {
          await window.electronAPI.markActionSynced(a.id);
        }
        window.appStore.addNotification('success', `Synchronisiert: ${pending.length} Aktion(en).`);
      } else {
        window.appStore.addNotification('error', 'Synchronisierung fehlgeschlagen.');
      }
    } catch (e) {
      console.error(e);
      window.appStore.addNotification('error', e.message || 'Synchronisierung fehlgeschlagen.');
    }
  }

  handleSyncUpdate(data) {
    console.log('Received sync update:', data);
    // Handle real-time updates from WebSocket
    window.appStore.addNotification('info', 'Daten wurden aktualisiert.');
  }

  async loadChecklists() {
    const list = document.getElementById('checklists-list');
    if (!list) return;
    list.innerHTML = '<li>Lade Checklisten...</li>';
    try {
      const data = await window.electronAPI?.listChecklists();
      list.innerHTML = '';
      if (data && data.length) {
        for (const c of data) {
          const li = document.createElement('li');
          li.textContent = c.name || JSON.stringify(c);
          list.appendChild(li);
        }
      } else {
        list.innerHTML = '<li>Keine Checklisten gefunden</li>';
      }
    } catch (e) {
      list.innerHTML = `<li>Fehler: ${e.message || e}</li>`;
    }
  }
}

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  window.app = new App();
});
