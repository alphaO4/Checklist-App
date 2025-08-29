import { contextBridge, ipcRenderer } from 'electron';

// Define the API that will be exposed to the renderer process
const electronAPI = {
  // App info
  getAppVersion: () => ipcRenderer.invoke('get-app-version'),
  
  // Offline storage operations
  saveOffline: (data: any) => ipcRenderer.invoke('save-checklist-offline', data),
  loadOffline: (id: string) => ipcRenderer.invoke('load-checklist-offline', id),
  getPendingActions: () => ipcRenderer.invoke('get-pending-actions'),
  markActionSynced: (actionId: string) => ipcRenderer.invoke('mark-action-synced', actionId),
  
  // TÜV deadline notifications
  onTuvAlert: (callback: (event: any, data: any) => void) => {
    ipcRenderer.on('tuv-alert', callback);
    // Return cleanup function
    return () => ipcRenderer.removeListener('tuv-alert', callback);
  },
  
  // Real-time synchronization
  onSyncUpdate: (callback: (event: any, data: any) => void) => {
    ipcRenderer.on('sync-update', callback);
    return () => ipcRenderer.removeListener('sync-update', callback);
  },
  
  // Network status
  onNetworkChange: (callback: (event: any, isOnline: boolean) => void) => {
    ipcRenderer.on('network-change', callback);
    return () => ipcRenderer.removeListener('network-change', callback);
  },
  
  // Window controls
  minimizeWindow: () => ipcRenderer.invoke('minimize-window'),
  maximizeWindow: () => ipcRenderer.invoke('maximize-window'),
  closeWindow: () => ipcRenderer.invoke('close-window'),

  // Backend integration
  login: (username: string, password: string) => ipcRenderer.invoke('auth-login', { username, password }),
  logout: () => ipcRenderer.invoke('auth-logout'),
  me: () => ipcRenderer.invoke('auth-me'),
  listVehicles: () => ipcRenderer.invoke('api-list-vehicles'),
  listVehicleTypes: () => ipcRenderer.invoke('api-list-vehicle-types'),
  createVehicleType: (data: any) => ipcRenderer.invoke('api-create-vehicle-type', data),
  listChecklists: () => ipcRenderer.invoke('api-list-checklists'),
  listTuv: () => ipcRenderer.invoke('api-list-tuv'),
  pushActions: (actions: any[]) => ipcRenderer.invoke('sync-push-actions', actions),
  
  // Token management
  storeToken: (token: string) => ipcRenderer.invoke('auth-store-token', token),
  getStoredToken: () => ipcRenderer.invoke('auth-get-stored-token'),
  clearStoredToken: () => ipcRenderer.invoke('auth-clear-stored-token'),
  
  // Health check
  healthCheck: () => ipcRenderer.invoke('backend-health-check')
};

// Expose the API to the renderer process
contextBridge.exposeInMainWorld('electronAPI', electronAPI);

// Type declaration for TypeScript support in renderer
export type ElectronAPI = typeof electronAPI;
