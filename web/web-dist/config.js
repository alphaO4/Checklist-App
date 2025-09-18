// Auto-generated configuration file - DO NOT EDIT MANUALLY
// Generated at: 2025-09-17T11:25:56.338Z
// Environment: development

window.APP_CONFIG = {
  "backend": {
    "baseUrl": "http://10.20.1.108:8000",
    "wsUrl": "ws://10.20.1.108:8000",
    "host": "10.20.1.108",
    "port": 8000,
    "protocol": "http",
    "wsProtocol": "ws"
  },
  "environment": "development",
  "buildTime": "2025-09-17T11:25:56.338Z"
};

// Expose configuration helper functions
window.getBackendUrl = function() {
  return window.APP_CONFIG.backend.baseUrl;
};

window.getBackendWsUrl = function() {
  return window.APP_CONFIG.backend.wsUrl;
};

window.getCSPConnectSrc = function() {
  const config = window.APP_CONFIG.backend;
  return `${config.protocol}://${config.host}:${config.port} ${config.wsProtocol}://${config.host}:${config.port}`;
};