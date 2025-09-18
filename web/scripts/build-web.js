#!/usr/bin/env node
/**
 * Web Build Script for Feuerwehr Checklist App
 * Converts Electron renderer to standalone web app
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const SRC_DIR = path.join(__dirname, '../src/renderer');
const WEB_DIST = path.join(__dirname, '../web-dist');
const CONFIG_SCRIPT = path.join(__dirname, 'generate-config.js');

function createWebBuild() {
  console.log('🌐 Building web version of Feuerwehr Checklist App...');

  // 1. Clean previous build
  if (fs.existsSync(WEB_DIST)) {
    fs.rmSync(WEB_DIST, { recursive: true });
  }
  fs.mkdirSync(WEB_DIST, { recursive: true });

  // 2. Generate configuration for web deployment
  process.env.DEPLOYMENT_TARGET = 'web';
  console.log('📋 Generating web configuration...');
  execSync(`node "${CONFIG_SCRIPT}"`, { stdio: 'inherit' });

  // 3. Copy renderer files
  console.log('📁 Copying renderer files...');
  copyDirectory(SRC_DIR, WEB_DIST);

  // 4. Copy generated config.js from dist to web-dist
  const distConfigPath = path.join(__dirname, '../dist/renderer/config.js');
  const webConfigPath = path.join(WEB_DIST, 'config.js');
  if (fs.existsSync(distConfigPath)) {
    fs.copyFileSync(distConfigPath, webConfigPath);
    console.log('✅ Config.js copied to web-dist');
  } else {
    console.error('❌ config.js not found in dist/renderer/');
  }

  // 5. Copy processed index.html from dist to web-dist (has CSP updated)
  const distHtmlPath = path.join(__dirname, '../dist/renderer/index.html');
  const webHtmlPath = path.join(WEB_DIST, 'index.html');
  if (fs.existsSync(distHtmlPath)) {
    fs.copyFileSync(distHtmlPath, webHtmlPath);
    console.log('✅ Processed index.html copied to web-dist');
  } else {
    console.warn('⚠️ Processed index.html not found, using source version');
  }

  // 6. Create web-specific adaptations
  createWebAdaptations();

  // 7. Copy assets
  const assetsDir = path.join(__dirname, '../assets');
  if (fs.existsSync(assetsDir)) {
    copyDirectory(assetsDir, path.join(WEB_DIST, 'assets'));
  }

  console.log('✅ Web build completed in web-dist/');
  console.log('🚀 Run: npm run serve:web');
}

function copyDirectory(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }

  const entries = fs.readdirSync(src, { withFileTypes: true });
  
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    
    if (entry.isDirectory()) {
      copyDirectory(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

function createWebAdaptations() {
  // Create web-specific electron-api-bridge.js
  const apiAdapter = `
/**
 * Web API Adapter - Replaces Electron APIs with HTTP calls
 */

// Create mock electronAPI that uses HTTP instead of IPC
window.electronAPI = {
  // Authentication
  async login(username, password) {
    return await window.configUtils.fetchBackend('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
  },

  async getCurrentUser() {
    return await window.configUtils.fetchBackend('/auth/me');
  },

  // Vehicle Types
  async listVehicleTypes() {
    return await window.configUtils.fetchBackend('/vehicle-types');
  },

  async createVehicleType(data) {
    return await window.configUtils.fetchBackend('/vehicle-types', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async updateVehicleType(id, data) {
    return await window.configUtils.fetchBackend(\`/vehicle-types/\${id}\`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async deleteVehicleType(id) {
    return await window.configUtils.fetchBackend(\`/vehicle-types/\${id}\`, {
      method: 'DELETE'
    });
  },

  // Vehicles
  async listVehicles() {
    return await window.configUtils.fetchBackend('/vehicles');
  },

  async createVehicle(data) {
    return await window.configUtils.fetchBackend('/vehicles', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async updateVehicle(id, data) {
    return await window.configUtils.fetchBackend(\`/vehicles/\${id}\`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  async deleteVehicle(id) {
    return await window.configUtils.fetchBackend(\`/vehicles/\${id}\`, {
      method: 'DELETE'
    });
  },

  // Groups
  async listGroups() {
    return await window.configUtils.fetchBackend('/groups');
  },

  async createGroup(data) {
    return await window.configUtils.fetchBackend('/groups', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  // Vehicle Groups  
  async listVehicleGroups() {
    return await window.configUtils.fetchBackend('/fahrzeuggruppen');
  },

  // TÜV Management
  async listTuvTermine() {
    return await window.configUtils.fetchBackend('/tuv');
  },

  async updateTuvTermin(id, data) {
    return await window.configUtils.fetchBackend(\`/tuv/\${id}\`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  // Health Check
  async healthCheck() {
    return await window.configUtils.fetchBackend('/health');
  }
};

// Initialize after DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  console.log('🌐 Web API adapter loaded');
});
`;

  fs.writeFileSync(path.join(WEB_DIST, 'web-api-adapter.js'), apiAdapter);

  // Update index.html to include the adapter (only if we're using the source version)
  const indexPath = path.join(WEB_DIST, 'index.html');
  let indexContent = fs.readFileSync(indexPath, 'utf8');
  
  // Only add web adapter script if it's not already there
  if (!indexContent.includes('web-api-adapter.js')) {
    // Add web adapter script after config.js
    indexContent = indexContent.replace(
      '<script src="./config.js"></script>',
      '<script src="./config.js"></script>\n  <script src="./web-api-adapter.js"></script>'
    );
    
    fs.writeFileSync(indexPath, indexContent);
    console.log('✅ Web adapter script added to index.html');
  } else {
    console.log('ℹ️ Web adapter script already present in index.html');
  }

  // Create PWA manifest
  const manifest = {
    name: "Feuerwehr Fahrzeugprüfung",
    short_name: "FFW Checklist",
    description: "Vehicle Inspection Checklist for German Fire Departments",
    start_url: "/",
    display: "standalone",
    theme_color: "#c41e3a",
    background_color: "#ffffff",
    icons: [
      {
        src: "assets/icon-192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "any maskable"
      },
      {
        src: "assets/icon-512.png", 
        sizes: "512x512",
        type: "image/png"
      }
    ]
  };

  fs.writeFileSync(path.join(WEB_DIST, 'manifest.json'), JSON.stringify(manifest, null, 2));

  // Create basic service worker for offline support
  const serviceWorker = `
/**
 * Service Worker for Feuerwehr Checklist App
 * Provides basic offline functionality
 */

const CACHE_NAME = 'feuerwehr-checklist-v1';
const urlsToCache = [
  '/',
  '/index.html',
  '/config.js',
  '/web-api-adapter.js',
  '/js/renderer.js',
  '/styles/main.css',
  '/components/',
  '/assets/icon-192.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(response => {
        // Return cached version or fetch from network
        return response || fetch(event.request);
      })
  );
});
`;

  fs.writeFileSync(path.join(WEB_DIST, 'service-worker.js'), serviceWorker);

  console.log('✅ Web adaptations created');
}

if (require.main === module) {
  createWebBuild();
}

module.exports = { createWebBuild };
