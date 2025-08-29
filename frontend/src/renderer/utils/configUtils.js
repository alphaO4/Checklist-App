/**
 * Configuration utilities for the renderer process
 * This module provides access to the runtime configuration loaded via config.js
 */

/**
 * Get the backend base URL from the runtime configuration
 * @returns {string} The backend base URL
 */
function getBackendUrl() {
  if (typeof window !== 'undefined' && window.getBackendUrl) {
    return window.getBackendUrl();
  }
  
  // Fallback if config is not loaded
  console.warn('[ConfigUtils] Runtime configuration not available, using fallback');
  return 'http://127.0.0.1:8000';
}

/**
 * Get the backend WebSocket URL from the runtime configuration
 * @returns {string} The backend WebSocket URL
 */
function getBackendWsUrl() {
  if (typeof window !== 'undefined' && window.getBackendWsUrl) {
    return window.getBackendWsUrl();
  }
  
  // Fallback if config is not loaded
  console.warn('[ConfigUtils] Runtime configuration not available, using fallback');
  return 'ws://127.0.0.1:8000';
}

/**
 * Get the current backend configuration
 * @returns {object} The backend configuration object
 */
function getBackendConfig() {
  if (typeof window !== 'undefined' && window.APP_CONFIG) {
    return window.APP_CONFIG.backend;
  }
  
  // Fallback configuration
  console.warn('[ConfigUtils] Runtime configuration not available, using fallback');
  return {
    baseUrl: 'http://127.0.0.1:8000',
    wsUrl: 'ws://127.0.0.1:8000',
    host: '127.0.0.1',
    port: 8000,
    protocol: 'http',
    wsProtocol: 'ws'
  };
}

/**
 * Make an authenticated fetch request to the backend
 * @param {string} endpoint - The API endpoint (relative to base URL)
 * @param {object} options - Fetch options
 * @returns {Promise<Response>} The fetch response
 */
async function fetchBackend(endpoint, options = {}) {
  const baseUrl = getBackendUrl();
  const url = `${baseUrl}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  
  // Get auth token from appStore
  let headers = { ...options.headers };
  
  if (typeof window !== 'undefined' && window.appStore) {
    try {
      const state = window.appStore.getState();
      if (
        state &&
        typeof state.token === 'string' &&
        state.token.trim().length > 0 &&
        // JWT tokens have three base64url-encoded parts separated by dots: header.payload.signature
        // This regex checks for the three-part structure with valid base64url characters (no padding in JWT parts)
        /^[A-Za-z0-9\-_]+?\.[A-Za-z0-9\-_]+?\.[A-Za-z0-9\-_]+$/.test(state.token)
      ) {
        headers['Authorization'] = `Bearer ${state.token}`;
      }
    } catch (error) {
      console.warn('[ConfigUtils] Failed to get auth token from appStore:', error);
    }
  }
  
  return fetch(url, {
    ...options,
    headers
  });
}

// Export for use in other modules
if (typeof window !== 'undefined') {
  window.configUtils = {};
  window.configUtils.getBackendUrl = getBackendUrl;
  window.configUtils.getBackendWsUrl = getBackendWsUrl;
  window.configUtils.getBackendConfig = getBackendConfig;
  window.configUtils.fetchBackend = fetchBackend;
}
