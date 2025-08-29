import { app, BrowserWindow, ipcMain } from 'electron';
import * as path from 'path';
import { setupIpcHandlers } from './ipc/handlers';

// Keep a global reference of the window object
let mainWindow: BrowserWindow | null = null;

const createWindow = (): void => {
  // Create the browser window with security best practices
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      contextIsolation: true,           // Enable context isolation for security
      nodeIntegration: false,           // Disable node integration in renderer
      preload: path.join(__dirname, 'preload.js')
    },
    icon: path.join(__dirname, '../../assets/icon.png'), // App icon
    show: false, // Don't show until ready
    titleBarStyle: process.platform === 'darwin' ? 'hiddenInset' : 'default'
  });

  // Load the app
  // Always load from local file (Electron app, not web server)
  mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));
  
  // Open DevTools in development
  if (process.env.NODE_ENV === 'development') {
    mainWindow.webContents.openDevTools();
  }

  // Show window when ready to prevent visual flash
  mainWindow.once('ready-to-show', () => {
    mainWindow?.show();
  });

  // Handle window closed
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
};

// App event handlers
app.whenReady().then(async () => {
  // Setup IPC handlers
  await setupIpcHandlers();
  
  createWindow();

  // Periodic health check and sync
  setInterval(async () => {
    try {
      const { backendClient } = await import('./backend');
      await backendClient.healthCheck();
      
      // Notify renderer about network status
      if (mainWindow) {
        mainWindow.webContents.send('network-change', true);
      }
    } catch (error) {
      // Backend is down
      if (mainWindow) {
        mainWindow.webContents.send('network-change', false);
      }
    }
  }, 30000); // Check every 30 seconds

  app.on('activate', () => {
    // Re-create window on macOS when dock icon is clicked
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

// Quit when all windows are closed (except on macOS)
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// Security: Prevent new window creation
app.on('web-contents-created', (event, contents) => {
  contents.setWindowOpenHandler(({ url }) => {
    // Prevent opening new windows, optionally open in external browser
    // require('electron').shell.openExternal(url);
    return { action: 'deny' };
  });
});

export { mainWindow };
