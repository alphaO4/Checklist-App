/**
 * Configuration module for backend URLs and environment-specific settings
 * This module handles environment variables and provides a centralized configuration
 */

export interface BackendConfig {
  baseUrl: string;
  wsUrl: string;
  host: string;
  port: number;
  protocol: 'http' | 'https';
  wsProtocol: 'ws' | 'wss';
}

/**
 * Loads configuration from environment variables with fallbacks
 */
export function loadBackendConfig(): BackendConfig {
  // Get environment variables with fallbacks
  const host = process.env.BACKEND_HOST || '127.0.0.1';
  let port = parseInt(process.env.BACKEND_PORT || '8000', 10);
  if (isNaN(port) || port < 1 || port > 65535) {
    console.warn(`[Config] Invalid BACKEND_PORT "${process.env.BACKEND_PORT}", falling back to 8000`);
    port = 8000;
  }
  let protocolEnv = process.env.BACKEND_PROTOCOL || 'http';
  let protocol: 'http' | 'https';
  if (protocolEnv === 'http' || protocolEnv === 'https') {
    protocol = protocolEnv;
  } else {
    console.warn(`[Config] Invalid BACKEND_PROTOCOL "${process.env.BACKEND_PROTOCOL}", falling back to "http"`);
    protocol = 'http';
  }

  let wsProtocolEnv = process.env.BACKEND_WS_PROTOCOL || 'ws';
  let wsProtocol: 'ws' | 'wss';
  if (wsProtocolEnv === 'ws' || wsProtocolEnv === 'wss') {
    wsProtocol = wsProtocolEnv;
  } else {
    console.warn(`[Config] Invalid BACKEND_WS_PROTOCOL "${process.env.BACKEND_WS_PROTOCOL}", falling back to "ws"`);
    wsProtocol = 'ws';
  }

  // Construct URLs
  const baseUrl = `${protocol}://${host}:${port}`;
  const wsUrl = `${wsProtocol}://${host}:${port}`;

  return {
    baseUrl,
    wsUrl,
    host,
    port,
    protocol,
    wsProtocol
  };
}

/**
 * Default configuration for development
 */
export const defaultConfig: BackendConfig = {
  baseUrl: 'http://127.0.0.1:8000',
  wsUrl: 'ws://127.0.0.1:8000',
  host: '127.0.0.1',
  port: 8000,
  protocol: 'http',
  wsProtocol: 'ws'
};

/**
 * Get backend configuration with environment variable support
 */
export function getBackendConfig(): BackendConfig {
  try {
    return loadBackendConfig();
  } catch (error) {
    console.warn('[Config] Failed to load environment config, using defaults:', error);
    return defaultConfig;
  }
}
