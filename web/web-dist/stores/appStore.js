// Simple state management store for the application
class AppStore {
  constructor() {
    this.state = {
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
      isOnline: navigator.onLine,
      vehicles: [],
      checklists: [],
      tuvTermine: [],
      errors: {},
      notifications: []
    };
    
    this.listeners = [];
    this.init();
  }

  init() {
    // Listen for network changes
    window.addEventListener('online', () => this.setOnline(true));
    window.addEventListener('offline', () => this.setOnline(false));
  }

  // Subscribe to state changes
  subscribe(listener) {
    this.listeners.push(listener);
    return () => {
      const index = this.listeners.indexOf(listener);
      if (index > -1) {
        this.listeners.splice(index, 1);
      }
    };
  }

  // Notify all listeners of state changes
  notify() {
    this.listeners.forEach(listener => listener(this.state));
  }

  // Update state and notify listeners
  setState(updates) {
    this.state = { ...this.state, ...updates };
    this.notify();
  }

  // Getters
  getState() {
    return this.state;
  }

  getUser() {
    return this.state.user;
  }

  isLoggedIn() {
    return this.state.isAuthenticated && this.state.user && this.state.token;
  }

  // Authentication actions
  setUser(user) {
    this.setState({ user, isAuthenticated: !!user });
  }

  setToken(token) {
    this.setState({ token, isAuthenticated: !!token });
  }

  setLoading(isLoading) {
    this.setState({ isLoading });
  }

  setOnline(isOnline) {
    this.setState({ isOnline });
  }

  login(user, token) {
    this.setState({
      user,
      token,
      isAuthenticated: true,
      isLoading: false
    });
  }

  logout() {
    this.setState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false
    });
  }

  // Data actions
  setVehicles(vehicles) {
    this.setState({ vehicles });
  }

  setChecklists(checklists) {
    this.setState({ checklists });
  }

  setTuvTermine(tuvTermine) {
    this.setState({ tuvTermine });
  }

  // Error handling
  setError(key, error) {
    this.setState({
      errors: { ...this.state.errors, [key]: error }
    });
  }

  clearError(key) {
    const errors = { ...this.state.errors };
    delete errors[key];
    this.setState({ errors });
  }

  clearAllErrors() {
    this.setState({ errors: {} });
  }

  // Notifications
  addNotification(type, message, duration = 5000) {
    const notification = {
      id: Date.now() + Math.random(),
      type,
      message,
      timestamp: Date.now()
    };
    
    this.setState({
      notifications: [...this.state.notifications, notification]
    });

    // Auto-remove notification
    if (duration > 0) {
      setTimeout(() => {
        this.removeNotification(notification.id);
      }, duration);
    }

    return notification.id;
  }

  removeNotification(id) {
    this.setState({
      notifications: this.state.notifications.filter(n => n.id !== id)
    });
  }

  clearNotifications() {
    this.setState({ notifications: [] });
  }
}

// Export singleton instance
window.appStore = window.appStore || new AppStore();
