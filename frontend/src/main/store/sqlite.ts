import * as sqlite3 from 'sqlite3';
import * as path from 'path';
import { app } from 'electron';

interface OfflineAction {
  id: string;
  type: 'item_update' | 'checklist_complete' | 'comment_add';
  data: any;
  timestamp: string;
  retryCount: number;
  synced: boolean;
}

interface ChecklistData {
  id: string;
  fahrzeugId: string;
  name: string;
  items: any[];
  status: string;
  lastModified: string;
}

export class OfflineStore {
  private db: sqlite3.Database | null = null;
  private dbPath: string;

  constructor() {
    // Store database in user data directory
    const userDataPath = app.getPath('userData');
    this.dbPath = path.join(userDataPath, 'checklist-offline.db');
  }

  async init(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.db = new sqlite3.Database(this.dbPath, (err) => {
        if (err) {
          console.error('Failed to open database:', err);
          reject(err);
          return;
        }
        
        console.log('Connected to SQLite database:', this.dbPath);
        this.createTables().then(resolve).catch(reject);
      });
    });
  }

  private async createTables(): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const createTablesSQL = `
      CREATE TABLE IF NOT EXISTS checklists (
        id TEXT PRIMARY KEY,
        fahrzeug_id TEXT NOT NULL,
        name TEXT NOT NULL,
        items TEXT NOT NULL,
        status TEXT NOT NULL,
        last_modified TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS offline_actions (
        id TEXT PRIMARY KEY,
        type TEXT NOT NULL,
        data TEXT NOT NULL,
        timestamp TEXT NOT NULL,
        retry_count INTEGER DEFAULT 0,
        synced BOOLEAN DEFAULT FALSE,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
      );

      CREATE INDEX IF NOT EXISTS idx_checklists_fahrzeug ON checklists(fahrzeug_id);
      CREATE INDEX IF NOT EXISTS idx_actions_synced ON offline_actions(synced);
    `;

    return new Promise((resolve, reject) => {
      this.db!.exec(createTablesSQL, (err) => {
        if (err) {
          console.error('Failed to create tables:', err);
          reject(err);
        } else {
          console.log('Database tables created successfully');
          resolve();
        }
      });
    });
  }

  async saveChecklist(checklist: ChecklistData): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const sql = `
      INSERT OR REPLACE INTO checklists 
      (id, fahrzeug_id, name, items, status, last_modified)
      VALUES (?, ?, ?, ?, ?, ?)
    `;

    return new Promise((resolve, reject) => {
      this.db!.run(sql, [
        checklist.id,
        checklist.fahrzeugId,
        checklist.name,
        JSON.stringify(checklist.items),
        checklist.status,
        checklist.lastModified
      ], (err) => {
        if (err) {
          console.error('Failed to save checklist:', err);
          reject(err);
        } else {
          resolve();
        }
      });
    });
  }

  async loadChecklist(id: string): Promise<ChecklistData | null> {
    if (!this.db) throw new Error('Database not initialized');

    const sql = 'SELECT * FROM checklists WHERE id = ?';

    return new Promise((resolve, reject) => {
      this.db!.get(sql, [id], (err, row: any) => {
        if (err) {
          console.error('Failed to load checklist:', err);
          reject(err);
        } else if (row) {
          resolve({
            id: row.id,
            fahrzeugId: row.fahrzeug_id,
            name: row.name,
            items: JSON.parse(row.items),
            status: row.status,
            lastModified: row.last_modified
          });
        } else {
          resolve(null);
        }
      });
    });
  }

  async queueAction(action: OfflineAction): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const sql = `
      INSERT INTO offline_actions 
      (id, type, data, timestamp, retry_count, synced)
      VALUES (?, ?, ?, ?, ?, ?)
    `;

    return new Promise((resolve, reject) => {
      this.db!.run(sql, [
        action.id,
        action.type,
        JSON.stringify(action.data),
        action.timestamp,
        action.retryCount,
        action.synced
      ], (err) => {
        if (err) {
          console.error('Failed to queue action:', err);
          reject(err);
        } else {
          resolve();
        }
      });
    });
  }

  async getPendingActions(): Promise<OfflineAction[]> {
    if (!this.db) throw new Error('Database not initialized');

    const sql = 'SELECT * FROM offline_actions WHERE synced = FALSE ORDER BY timestamp ASC';

    return new Promise((resolve, reject) => {
      this.db!.all(sql, [], (err, rows: any[]) => {
        if (err) {
          console.error('Failed to get pending actions:', err);
          reject(err);
        } else {
          const actions = rows.map(row => ({
            id: row.id,
            type: row.type,
            data: JSON.parse(row.data),
            timestamp: row.timestamp,
            retryCount: row.retry_count,
            synced: row.synced
          }));
          resolve(actions);
        }
      });
    });
  }

  async markActionSynced(actionId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const sql = 'UPDATE offline_actions SET synced = TRUE WHERE id = ?';

    return new Promise((resolve, reject) => {
      this.db!.run(sql, [actionId], (err) => {
        if (err) {
          console.error('Failed to mark action as synced:', err);
          reject(err);
        } else {
          resolve();
        }
      });
    });
  }

  async close(): Promise<void> {
    if (!this.db) return;

    return new Promise((resolve) => {
      this.db!.close((err) => {
        if (err) {
          console.error('Failed to close database:', err);
        }
        this.db = null;
        resolve();
      });
    });
  }
}
