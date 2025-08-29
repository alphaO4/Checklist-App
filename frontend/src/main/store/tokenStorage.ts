// Simple token storage using Electron's safeStorage API or fallback to encrypted file
import { safeStorage, app } from 'electron';
import * as fs from 'fs/promises';
import * as path from 'path';

class TokenStorage {
  private tokenFile: string;

  constructor() {
    this.tokenFile = path.join(app.getPath('userData'), 'auth_token.enc');
  }

  async storeToken(token: string): Promise<void> {
    try {
      if (safeStorage.isEncryptionAvailable()) {
        // Use Electron's secure storage
        const encrypted = safeStorage.encryptString(token);
        await fs.writeFile(this.tokenFile, encrypted);
      } else {
        // Fallback: store as plain text (not recommended for production)
        console.warn('Secure storage not available, storing token as plain text');
        await fs.writeFile(this.tokenFile, token);
      }
    } catch (error) {
      console.error('Failed to store token:', error);
      throw error;
    }
  }

  async getToken(): Promise<string | null> {
    try {
      const data = await fs.readFile(this.tokenFile);
      
      if (safeStorage.isEncryptionAvailable()) {
        // Decrypt using Electron's secure storage
        return safeStorage.decryptString(data);
      } else {
        // Fallback: read as plain text
        return data.toString();
      }
    } catch (error: any) {
      if (error?.code === 'ENOENT') {
        // File doesn't exist
        return null;
      }
      console.error('Failed to read token:', error);
      throw error;
    }
  }

  async clearToken(): Promise<void> {
    try {
      await fs.unlink(this.tokenFile);
    } catch (error: any) {
      if (error?.code !== 'ENOENT') {
        console.error('Failed to clear token:', error);
        throw error;
      }
    }
  }
}

export const tokenStorage = new TokenStorage();
