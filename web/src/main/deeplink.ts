/**
 * Deep Link Protocol Handler for Feuerwehr Checklist App
 * Enables launching Electron app from web browser with custom protocol
 * Based on: https://dev.to/rwwagner90/launching-electron-apps-from-the-browser-59oc
 */

import { app, BrowserWindow } from 'electron';
import { resolve } from 'path';

const isDev = process.env.NODE_ENV === 'development';
let deeplinkingUrl: string | undefined;

/**
 * Setup custom protocol handling for launching app from browser
 * Protocol: feuerwehr-checklist://action/data
 * 
 * Examples:
 * - feuerwehr-checklist://vehicle/inspect/MTF-123
 * - feuerwehr-checklist://checklist/start/daily-check
 * - feuerwehr-checklist://tuv/reminder/vehicle-456
 */
export function setupProtocolHandler() {
  // Set up custom protocol client
  if (isDev && process.platform === 'win32') {
    // Special handling for Windows development mode
    app.setAsDefaultProtocolClient('feuerwehr-checklist', process.execPath, [
      resolve(process.argv[1])
    ]);
  } else {
    app.setAsDefaultProtocolClient('feuerwehr-checklist');
  }

  // macOS: Handle open-url event
  app.on('open-url', (event, url) => {
    event.preventDefault();
    deeplinkingUrl = url;
    handleDeepLink(url);
  });

  // Windows/Linux: Handle second-instance (when app is already running)
  const gotTheLock = app.requestSingleInstanceLock();

  if (!gotTheLock) {
    app.quit();
    return;
  } else {
    app.on('second-instance', (event, argv) => {
      // Windows/Linux: Find the custom protocol URL in argv
      if (process.platform !== 'darwin') {
        deeplinkingUrl = argv.find((arg) => arg.startsWith('feuerwehr-checklist://'));
      }

      // Focus existing window and handle deep link
      const mainWindow = BrowserWindow.getAllWindows()[0];
      if (mainWindow) {
        if (mainWindow.isMinimized()) mainWindow.restore();
        mainWindow.focus();
        
        if (deeplinkingUrl) {
          handleDeepLink(deeplinkingUrl);
        }
      }
    });
  }
}

/**
 * Handle deep link URLs and route to appropriate app sections
 */
function handleDeepLink(url: string) {
  console.log('🔗 Deep link received:', url);
  
  try {
    const urlObj = new URL(url);
    const action = urlObj.hostname;
    const pathParts = urlObj.pathname.split('/').filter(Boolean);
    
    const mainWindow = BrowserWindow.getAllWindows()[0];
    if (!mainWindow) return;

    // Route based on action
    switch (action) {
      case 'vehicle':
        handleVehicleDeepLink(pathParts, mainWindow);
        break;
        
      case 'checklist':
        handleChecklistDeepLink(pathParts, mainWindow);
        break;
        
      case 'tuv':
        handleTuvDeepLink(pathParts, mainWindow);
        break;
        
      case 'login':
        handleLoginDeepLink(urlObj.searchParams, mainWindow);
        break;
        
      default:
        // Default: just focus the app
        mainWindow.webContents.send('deep-link-received', { 
          action: 'default',
          url 
        });
    }
  } catch (error) {
    console.error('❌ Error processing deep link:', error);
  }
}

/**
 * Handle vehicle-related deep links
 * Examples:
 * - feuerwehr-checklist://vehicle/inspect/MTF-123
 * - feuerwehr-checklist://vehicle/view/456
 */
function handleVehicleDeepLink(pathParts: string[], mainWindow: BrowserWindow) {
  const [subAction, vehicleId] = pathParts;
  
  switch (subAction) {
    case 'inspect':
      // Navigate to vehicle inspection page
      mainWindow.webContents.send('navigate-to', {
        page: 'vehicle-inspection',
        vehicleId
      });
      break;
      
    case 'view':
      // Navigate to vehicle details
      mainWindow.webContents.send('navigate-to', {
        page: 'vehicle-details',
        vehicleId
      });
      break;
      
    default:
      // Navigate to vehicles overview
      mainWindow.webContents.send('navigate-to', {
        page: 'vehicles'
      });
  }
}

/**
 * Handle checklist-related deep links
 * Examples:
 * - feuerwehr-checklist://checklist/start/daily-check
 * - feuerwehr-checklist://checklist/resume/abc123
 */
function handleChecklistDeepLink(pathParts: string[], mainWindow: BrowserWindow) {
  const [subAction, checklistId] = pathParts;
  
  switch (subAction) {
    case 'start':
      mainWindow.webContents.send('start-checklist', { checklistId });
      break;
      
    case 'resume':
      mainWindow.webContents.send('resume-checklist', { checklistId });
      break;
      
    default:
      mainWindow.webContents.send('navigate-to', { page: 'checklists' });
  }
}

/**
 * Handle TÜV-related deep links
 * Examples:
 * - feuerwehr-checklist://tuv/reminder/vehicle-456
 * - feuerwehr-checklist://tuv/expired
 */
function handleTuvDeepLink(pathParts: string[], mainWindow: BrowserWindow) {
  const [subAction, vehicleId] = pathParts;
  
  switch (subAction) {
    case 'reminder':
      mainWindow.webContents.send('show-tuv-reminder', { vehicleId });
      break;
      
    case 'expired':
      mainWindow.webContents.send('navigate-to', { 
        page: 'tuv',
        filter: 'expired'
      });
      break;
      
    default:
      mainWindow.webContents.send('navigate-to', { page: 'tuv' });
  }
}

/**
 * Handle login deep links with pre-filled data
 * Example: feuerwehr-checklist://login?group=feuerwehr-muster&return=vehicles
 */
function handleLoginDeepLink(searchParams: URLSearchParams, mainWindow: BrowserWindow) {
  const group = searchParams.get('group');
  const returnTo = searchParams.get('return');
  
  mainWindow.webContents.send('pre-fill-login', {
    group,
    returnTo
  });
}

/**
 * Get the pending deep link URL (for processing after app startup)
 */
export function getPendingDeepLink(): string | undefined {
  return deeplinkingUrl;
}

/**
 * Clear the pending deep link
 */
export function clearPendingDeepLink(): void {
  deeplinkingUrl = undefined;
}

/**
 * Generate deep link URLs for sharing
 */
export class DeepLinkGenerator {
  private static baseUrl = 'feuerwehr-checklist://';

  static vehicleInspection(vehicleId: string): string {
    return `${this.baseUrl}vehicle/inspect/${vehicleId}`;
  }

  static vehicleDetails(vehicleId: string): string {
    return `${this.baseUrl}vehicle/view/${vehicleId}`;
  }

  static startChecklist(checklistId: string): string {
    return `${this.baseUrl}checklist/start/${checklistId}`;
  }

  static resumeChecklist(checklistId: string): string {
    return `${this.baseUrl}checklist/resume/${checklistId}`;
  }

  static tuvReminder(vehicleId: string): string {
    return `${this.baseUrl}tuv/reminder/${vehicleId}`;
  }

  static loginWithGroup(group: string, returnTo?: string): string {
    const params = new URLSearchParams({ group });
    if (returnTo) params.set('return', returnTo);
    return `${this.baseUrl}login?${params.toString()}`;
  }
}
