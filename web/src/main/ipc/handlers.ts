import { ipcMain, app } from 'electron';
import { OfflineStore } from '../store/sqlite';
import { backendClient } from '../backend';
import { tokenStorage } from '../store/tokenStorage';

let offlineStore: OfflineStore;

export const setupIpcHandlers = async (): Promise<void> => {
  // Initialize offline store
  offlineStore = new OfflineStore();
  await offlineStore.init();

  // App version
  ipcMain.handle('get-app-version', () => {
    return app.getVersion();
  });

  // Offline storage handlers
  ipcMain.handle('save-checklist-offline', async (event, data) => {
    try {
      return await offlineStore.saveChecklist(data);
    } catch (error) {
      console.error('Failed to save checklist offline:', error);
      throw error;
    }
  });

  ipcMain.handle('load-checklist-offline', async (event, id: string) => {
    try {
      return await offlineStore.loadChecklist(id);
    } catch (error) {
      console.error('Failed to load checklist offline:', error);
      throw error;
    }
  });

  // Expose pending offline actions for sync
  ipcMain.handle('get-pending-actions', async () => {
    try {
      return await offlineStore.getPendingActions();
    } catch (error) {
      console.error('Failed to get pending actions:', error);
      throw error;
    }
  });

  ipcMain.handle('mark-action-synced', async (event, actionId: string) => {
    try {
      await offlineStore.markActionSynced(actionId);
      return true;
    } catch (error) {
      console.error('Failed to mark action synced:', error);
      throw error;
    }
  });

  // Backend integration
  ipcMain.handle('auth-login', async (event, creds: { username: string; password: string }) => {
    try {
      const { username, password } = creds;
      console.log(`[IPC] Attempting login for user: ${username}`);
      console.log(`[IPC] Backend URL: ${backendClient['baseUrl']}`);
      
      const token = await backendClient.login(username, password);
      console.log(`[IPC] Login successful, token received`);
      return token;
    } catch (error) {
      console.error('[IPC] Login failed:', error);
      throw error;
    }
  });

  ipcMain.handle('auth-me', async () => {
    return await backendClient.me();
  });

  // Token management
  ipcMain.handle('auth-store-token', async (event, token: string) => {
    try {
      await tokenStorage.storeToken(token);
      backendClient.setToken(token);
      return true;
    } catch (error) {
      console.error('Failed to store token:', error);
      throw error;
    }
  });

  ipcMain.handle('auth-get-stored-token', async () => {
    try {
      const token = await tokenStorage.getToken();
      if (token) {
        backendClient.setToken(token);
      }
      return token;
    } catch (error) {
      console.error('Failed to get stored token:', error);
      return null;
    }
  });

  ipcMain.handle('auth-clear-stored-token', async () => {
    try {
      await tokenStorage.clearToken();
      backendClient.setToken(null);
      return true;
    } catch (error) {
      console.error('Failed to clear stored token:', error);
      throw error;
    }
  });

  // Health check
  ipcMain.handle('backend-health-check', async () => {
    return await backendClient.healthCheck();
  });

  ipcMain.handle('api-list-vehicles', async () => {
    return await backendClient.listVehicles();
  });

  ipcMain.handle('api-list-vehicle-types', async () => {
    return await backendClient.listVehicleTypes();
  });

  ipcMain.handle('api-create-vehicle-type', async (event, data: any) => {
    return await backendClient.createVehicleType(data);
  });

  ipcMain.handle('api-update-vehicle-type', async (event, id: string, data: any) => {
    return await backendClient.updateVehicleType(id, data);
  });

  ipcMain.handle('api-delete-vehicle-type', async (event, id: string) => {
    return await backendClient.deleteVehicleType(id);
  });

  ipcMain.handle('api-list-checklists', async () => {
    return await backendClient.listChecklists();
  });

  ipcMain.handle('api-list-tuv', async () => {
    return await backendClient.listTuvDeadlines();
  });

  ipcMain.handle('auth-logout', async () => {
    return await backendClient.logout();
  });

  ipcMain.handle('sync-push-actions', async (event, actions: any[]) => {
    return await backendClient.pushActions(actions);
  });

  ipcMain.handle('api-list-fahrzeuggruppen', async () => {
    return await backendClient.listFahrzeuggruppen();
  });

  ipcMain.handle('api-update-vehicle', async (event, id: string, data: any) => {
    return await backendClient.updateVehicle(id, data);
  });

  ipcMain.handle('api-create-vehicle', async (event, data: any) => {
    return await backendClient.createVehicle(data);
  });

  ipcMain.handle('api-update-vehicle-tuv', async (event, vehicleId: string, data: any) => {
    return await backendClient.updateVehicleTuv(vehicleId, data);
  });

  // Group management handlers
  ipcMain.handle('api-list-groups', async () => {
    return await backendClient.listGroups();
  });

  ipcMain.handle('api-get-group', async (event, id: string) => {
    return await backendClient.getGroup(id);
  });

  ipcMain.handle('api-create-group', async (event, data: any) => {
    return await backendClient.createGroup(data);
  });

  ipcMain.handle('api-update-group', async (event, id: string, data: any) => {
    return await backendClient.updateGroup(id, data);
  });

  ipcMain.handle('api-delete-group', async (event, id: string) => {
    return await backendClient.deleteGroup(id);
  });

  // Fahrzeuggruppe management handlers
  ipcMain.handle('api-get-fahrzeuggruppe', async (event, id: string) => {
    return await backendClient.getFahrzeuggruppe(id);
  });

  ipcMain.handle('api-create-fahrzeuggruppe', async (event, data: any) => {
    return await backendClient.createFahrzeuggruppe(data);
  });

  ipcMain.handle('api-update-fahrzeuggruppe', async (event, id: string, data: any) => {
    return await backendClient.updateFahrzeuggruppe(id, data);
  });

  ipcMain.handle('api-delete-fahrzeuggruppe', async (event, id: string) => {
    return await backendClient.deleteFahrzeuggruppe(id);
  });

  ipcMain.handle('api-list-users', async () => {
    return await backendClient.listUsers();
  });

  // Window controls
  ipcMain.handle('minimize-window', (event) => {
    const window = require('electron').BrowserWindow.fromWebContents(event.sender);
    window?.minimize();
  });

  ipcMain.handle('maximize-window', (event) => {
    const window = require('electron').BrowserWindow.fromWebContents(event.sender);
    if (window?.isMaximized()) {
      window.unmaximize();
    } else {
      window?.maximize();
    }
  });

  ipcMain.handle('close-window', (event) => {
    const window = require('electron').BrowserWindow.fromWebContents(event.sender);
    window?.close();
  });
};

export { offlineStore };
