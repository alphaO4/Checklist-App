# Mobile App Deployment with Capacitor

## Setup Instructions

### 1. Install Capacitor
```bash
cd frontend
npm install @capacitor/core @capacitor/cli
npm install @capacitor/ios @capacitor/android
```

### 2. Initialize Capacitor Project
```bash
npx cap init "Feuerwehr Checklist" "de.feuerwehr.checklist"
```

### 3. Configure capacitor.config.ts
```typescript
import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'de.feuerwehr.checklist',
  appName: 'Feuerwehr Checklist',
  webDir: 'web-dist',
  server: {
    androidScheme: 'https'
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: "#c41e3a",
      showSpinner: false
    },
    SQLite: {
      iosDatabaseLocation: 'Library/CapacitorDatabase',
      androidDatabaseLocation: 'default'
    }
  }
};

export default config;
```

### 4. Add Mobile Platforms
```bash
# Build web version first
npm run build:web

# Add platforms
npx cap add ios
npx cap add android

# Copy web assets to native projects
npx cap copy

# Open in IDEs
npx cap open ios      # Requires Xcode on macOS
npx cap open android  # Requires Android Studio
```

### 5. Install Mobile-Specific Plugins
```bash
# Essential plugins for fire department app
npm install @capacitor/filesystem
npm install @capacitor/network
npm install @capacitor/device
npm install @capacitor-community/sqlite
npm install @capacitor/preferences
```

## Mobile Adaptations

### SQLite for Mobile
Replace browser storage with Capacitor SQLite:

```javascript
// mobile-storage-adapter.js
import { CapacitorSQLite, SQLiteConnection } from '@capacitor-community/sqlite';
import { Capacitor } from '@capacitor/core';

class MobileStorage {
  constructor() {
    this.sqlite = new SQLiteConnection(CapacitorSQLite);
    this.platform = Capacitor.getPlatform();
  }

  async initDatabase() {
    if (this.platform === 'web') {
      // Use WebSQL or IndexedDB fallback
      return this.initWebDB();
    }
    
    // Native mobile SQLite
    await this.sqlite.createConnection({
      database: 'feuerwehr_checklist.db',
      version: 1,
      encrypted: false,
      mode: 'no-encryption'
    });
  }

  async executeQuery(sql, params = []) {
    const db = await this.sqlite.retrieveConnection('feuerwehr_checklist.db');
    return await db.execute(sql, params);
  }
}
```

### Network Detection
```javascript
// network-manager.js
import { Network } from '@capacitor/network';

class NetworkManager {
  constructor() {
    this.isOnline = true;
    this.setupNetworkListeners();
  }

  async setupNetworkListeners() {
    const status = await Network.getStatus();
    this.isOnline = status.connected;

    Network.addListener('networkStatusChange', (status) => {
      this.isOnline = status.connected;
      this.handleNetworkChange(status);
    });
  }

  handleNetworkChange(status) {
    if (status.connected) {
      // Sync offline data when back online
      this.syncOfflineData();
    } else {
      // Switch to offline mode
      this.enableOfflineMode();
    }
  }
}
```

### Push Notifications (Future)
```javascript
// notifications.js
import { PushNotifications } from '@capacitor/push-notifications';

class NotificationManager {
  async setupPushNotifications() {
    // Request permission
    let permStatus = await PushNotifications.checkPermissions();
    
    if (permStatus.receive === 'prompt') {
      permStatus = await PushNotifications.requestPermissions();
    }

    if (permStatus.receive !== 'granted') {
      throw new Error('User denied permissions!');
    }

    // Register for push notifications
    await PushNotifications.register();

    // Listen for registration
    PushNotifications.addListener('registration', (token) => {
      console.log('Push registration success, token: ' + token.value);
      // Send token to your backend
    });

    // Listen for incoming notifications
    PushNotifications.addListener('pushNotificationReceived', (notification) => {
      console.log('Push received: ', notification);
      // Handle TÜV deadline reminders
    });
  }
}
```

## Build Scripts

Add to package.json:
```json
{
  "scripts": {
    "build:web": "node scripts/build-web.js",
    "build:mobile": "npm run build:web && npx cap copy",
    "mobile:ios": "npm run build:mobile && npx cap open ios",
    "mobile:android": "npm run build:mobile && npx cap open android",
    "mobile:sync": "npx cap sync"
  }
}
```

## App Store Preparation

### iOS App Store
1. **Xcode Configuration**
   - Set deployment target to iOS 13+
   - Configure signing certificates
   - Add app icons (required sizes: 60x60, 120x120, 180x180)
   - Set privacy usage descriptions

2. **Info.plist Updates**
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>App needs camera access for QR code scanning</string>
   <key>NSLocationWhenInUseUsageDescription</key>
   <string>App needs location for vehicle position tracking</string>
   ```

### Android Play Store
1. **Android Manifest**
   - Set minimum SDK version 22 (Android 5.1)
   - Configure app permissions
   - Add adaptive icons

2. **Gradle Configuration**
   ```gradle
   android {
     compileSdkVersion 34
     defaultConfig {
       minSdkVersion 22
       targetSdkVersion 34
       versionCode 1
       versionName "1.0.0"
     }
   }
   ```

## Testing Strategy

### Device Testing
```bash
# iOS Simulator
npx cap run ios

# Android Emulator  
npx cap run android

# Physical devices (via USB debugging)
npx cap run ios --target="Physical Device"
npx cap run android --target="Physical Device"
```

### Progressive Web App Testing
```bash
# Test PWA features
npm run build:web
npx http-server web-dist -p 3000

# Use Chrome DevTools > Application > Service Workers
# Test offline functionality
# Test "Add to Home Screen"
```

## Deployment Timeline

### Week 1: Web App
- [ ] Extract renderer to web-dist
- [ ] Test API connectivity  
- [ ] Deploy to staging (Vercel/Netlify)

### Week 2: PWA Features
- [ ] Add service worker
- [ ] Implement offline storage
- [ ] Test mobile browsers

### Week 3: Mobile Setup
- [ ] Install Capacitor
- [ ] Configure iOS/Android projects
- [ ] Test on simulators

### Week 4: Native Features
- [ ] Implement SQLite storage
- [ ] Add network detection
- [ ] Test on physical devices

### Month 2: App Store
- [ ] Finalize app metadata
- [ ] Create store screenshots
- [ ] Submit for review

## Fire Department Specific Considerations

### Offline Requirements
- Vehicle checklists must work without internet
- Sync when connection restored
- Local SQLite with backup/restore

### Security
- Encrypt sensitive vehicle data
- Secure authentication tokens
- Audit trail for all changes

### Compliance
- GDPR compliance for user data
- Data residency (German servers)
- Regular security updates
