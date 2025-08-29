// Authentication manager for handling login, token storage, and session management

class AuthManager {
  constructor() {
    this.init();
  }

  async init() {
    // Try to restore token from secure storage on app start
    await this.restoreSession();
  }

  async restoreSession() {
    try {
      if (typeof window.electronAPI !== 'undefined') {
        const storedToken = await window.electronAPI.getStoredToken();
        if (storedToken) {
          // Validate token by trying to get user info
          const user = await window.electronAPI.me();
          if (user) {
            window.appStore.login(user, storedToken);
            return true;
          }
        }
      }
    } catch (error) {
      console.warn('Could not restore session:', error);
      // Clear invalid token
      await this.clearStoredToken();
    }
    return false;
  }

  async login(username, password) {
    window.appStore.setLoading(true);
    window.appStore.clearError('login');

    try {
      if (!window.appStore.getState().isOnline) {
        throw new Error('Keine Internetverbindung für die Anmeldung verfügbar');
      }

      if (typeof window.electronAPI === 'undefined') {
        throw new Error('Electron API nicht verfügbar');
      }

      // Attempt login
      const tokenResponse = await window.electronAPI.login(username, password);
      
      if (!tokenResponse?.access_token) {
        throw new Error('Ungültige Antwort vom Server');
      }

      // Get user info
      const user = await window.electronAPI.me();
      
      if (!user) {
        throw new Error('Benutzerinformationen konnten nicht abgerufen werden');
      }

      // Store token securely
      await window.electronAPI.storeToken(tokenResponse.access_token);

      // Update app state
      window.appStore.login(user, tokenResponse.access_token);
      
      window.appStore.addNotification('success', `Willkommen, ${user.username}!`);
      
      return { success: true, user };
      
    } catch (error) {
      console.error('Login error:', error);
      const errorMessage = this.getErrorMessage(error);
      window.appStore.setError('login', errorMessage);
      window.appStore.addNotification('error', errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      window.appStore.setLoading(false);
    }
  }

  async logout() {
    try {
      // Notify backend about logout
      if (typeof window.electronAPI !== 'undefined') {
        try {
          await window.electronAPI.logout();
        } catch (error) {
          console.warn('Backend logout failed, continuing with local logout:', error);
        }
      }
      
      // Clear stored token
      await this.clearStoredToken();
      
      // Update app state
      window.appStore.logout();
      window.appStore.clearAllErrors();
      window.appStore.addNotification('info', 'Erfolgreich abgemeldet');
      
    } catch (error) {
      console.error('Logout error:', error);
      // Force logout even if clearing token fails
      window.appStore.logout();
    }
  }

  async clearStoredToken() {
    if (typeof window.electronAPI !== 'undefined') {
      try {
        await window.electronAPI.clearStoredToken();
      } catch (error) {
        console.warn('Could not clear stored token:', error);
      }
    }
  }

  getErrorMessage(error) {
    if (typeof error === 'string') {
      return error;
    }
    
    if (error?.message) {
      // Map common backend errors to German
      const errorMap = {
        'Unauthorized': 'Benutzername oder Passwort ungültig',
        'Not authenticated': 'Nicht angemeldet',
        'Invalid credentials': 'Ungültige Anmeldedaten',
        'Network error': 'Netzwerkfehler - bitte später versuchen',
        'Server error': 'Serverfehler - bitte später versuchen'
      };

      return errorMap[error.message] || error.message;
    }

    return 'Ein unbekannter Fehler ist aufgetreten';
  }

  // Check if backend is reachable
  async checkBackendHealth() {
    try {
      if (typeof window.electronAPI !== 'undefined') {
        await window.electronAPI.healthCheck();
        return true;
      }
      return false;
    } catch (error) {
      return false;
    }
  }
}

// Export singleton instance
window.authManager = window.authManager || new AuthManager();
