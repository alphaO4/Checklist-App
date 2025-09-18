/**
 * Build-time configuration generator
 * This script generates a runtime configuration file for the renderer process
 */

const fs = require('fs');
const path = require('path');

// Load environment variables from .env files
function loadEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {};
  }
  
  const envContent = fs.readFileSync(filePath, 'utf8');
  const env = {};
  
  envContent.split('\n').forEach(line => {
    const trimmed = line.trim();
    if (trimmed && !trimmed.startsWith('#')) {
      const [key, ...valueParts] = trimmed.split('=');
      if (key && valueParts.length > 0) {
        env[key.trim()] = valueParts.join('=').trim();
      }
    }
  });
  
  return env;
}

function generateConfig() {
  const nodeEnv = process.env.NODE_ENV || 'development';
  
  // Load environment files in order of precedence
  const envFiles = [
    path.join(__dirname, '..', '.env.template'),
    path.join(__dirname, '..', `.env.${nodeEnv}`),
    path.join(__dirname, '..', '.env.local')
  ];
  
  let config = {};
  
  // Load env files (later files override earlier ones)
  envFiles.forEach(file => {
    const env = loadEnvFile(file);
    config = { ...config, ...env };
  });
  
  // Override with actual environment variables
  const envKeys = ['BACKEND_HOST', 'BACKEND_PORT', 'BACKEND_PROTOCOL', 'BACKEND_WS_PROTOCOL'];
  envKeys.forEach(key => {
    if (process.env[key]) {
      config[key] = process.env[key];
    }
  });
  
  // Generate the configuration object
  const host = config.BACKEND_HOST || 'localhost';
  let port = parseInt(config.BACKEND_PORT || '8000', 10);
  if (isNaN(port)) {
    port = 8000;
  }
  const protocol = config.BACKEND_PROTOCOL || 'http';
  const wsProtocol = config.BACKEND_WS_PROTOCOL || 'ws';
  
  const runtimeConfig = {
    backend: {
      baseUrl: `${protocol}://${host}:${port}`,
      wsUrl: `${wsProtocol}://${host}:${port}`,
      host,
      port,
      protocol,
      wsProtocol
    },
    environment: nodeEnv,
    buildTime: new Date().toISOString()
  };
  
  // Generate the JavaScript file for the renderer
  const configJs = `
// Auto-generated configuration file - DO NOT EDIT MANUALLY
// Generated at: ${runtimeConfig.buildTime}
// Environment: ${nodeEnv}

window.APP_CONFIG = ${JSON.stringify(runtimeConfig, null, 2)};

// Expose configuration helper functions
window.getBackendUrl = function() {
  return window.APP_CONFIG.backend.baseUrl;
};

window.getBackendWsUrl = function() {
  return window.APP_CONFIG.backend.wsUrl;
};

window.getCSPConnectSrc = function() {
  const config = window.APP_CONFIG.backend;
  return \`\${config.protocol}://\${config.host}:\${config.port} \${config.wsProtocol}://\${config.host}:\${config.port}\`;
};
`.trim();
  
  // Ensure dist directory exists
  const distDir = path.join(__dirname, '..', 'dist', 'renderer');
  if (!fs.existsSync(distDir)) {
    fs.mkdirSync(distDir, { recursive: true });
  }
  
  // Write the configuration file
  const configPath = path.join(distDir, 'config.js');
  fs.writeFileSync(configPath, configJs);
  
  // Process HTML template
  processHtmlTemplate(runtimeConfig);
  
  console.log(`[Config] Generated runtime configuration for ${nodeEnv} environment`);
  console.log(`[Config] Backend URL: ${runtimeConfig.backend.baseUrl}`);
  console.log(`[Config] WebSocket URL: ${runtimeConfig.backend.wsUrl}`);
  console.log(`[Config] Config written to: ${configPath}`);
  
  return runtimeConfig;
}

function processHtmlTemplate(config) {
  const srcHtmlPath = path.join(__dirname, '..', 'src', 'renderer', 'index.html');
  const distHtmlPath = path.join(__dirname, '..', 'dist', 'renderer', 'index.html');
  
  if (!fs.existsSync(srcHtmlPath)) {
    console.warn(`[Config] HTML template not found: ${srcHtmlPath}`);
    return;
  }
  
  let htmlContent = fs.readFileSync(srcHtmlPath, 'utf8');
  
  // Replace template variables
  const connectSrc = `${config.backend.protocol}://${config.backend.host}:${config.backend.port} ${config.backend.wsProtocol}://${config.backend.host}:${config.backend.port}`;
  htmlContent = htmlContent.replace('{{BACKEND_CONNECT_SRC}}', connectSrc);
  
  // Ensure dist directory exists
  const distDir = path.dirname(distHtmlPath);
  if (!fs.existsSync(distDir)) {
    fs.mkdirSync(distDir, { recursive: true });
  }
  
  fs.writeFileSync(distHtmlPath, htmlContent);
  console.log(`[Config] Processed HTML template with CSP: ${connectSrc}`);
}

// Run if called directly
if (require.main === module) {
  generateConfig();
}

module.exports = { generateConfig };
