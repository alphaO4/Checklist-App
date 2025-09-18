# Android App Deployment Guide

## Feuerwehr Checklist Android Wrapper

Diese Anleitung beschreibt die Erstellung und Bereitstellung der Android-App für die Feuerwehr Checklist Anwendung.

## 🚀 Quick Start

### Voraussetzungen

1. **Android Studio** (Arctic Fox oder neuer)
2. **Java Development Kit (JDK) 11+**
3. **Android SDK** mit API Level 24+
4. **Git** für Repository-Management

### Erste Installation

1. **Android Studio Setup:**
   - Android Studio Giraffe (2022.3.1) oder neuer installieren
   - Android SDK API Level 24-34 installieren
   - Android Build Tools aktualisieren

2. **Projekt öffnen:**
   - Android Studio starten
   - "Open" → `e:\Github\Checklist-App\android` auswählen
   - Gradle-Sync abwarten (ca. 2-5 Minuten beim ersten Mal)
   - SDK-Downloads bestätigen falls erforderlich

3. **Projekt-Setup verifizieren:**
   - **File** → **Project Structure** → **SDK Location** prüfen
   - **Build** → **Clean Project**
   - **Build** → **Rebuild Project**

### Entwicklung (Debug Build) in Android Studio

1. **Build Variant auswählen:**
   - **View** → **Tool Windows** → **Build Variants**
   - "debug" auswählen für Entwicklung

2. **App bauen und testen:**
   - **Build** → **Make Project** (Ctrl+F9)
   - Oder **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**

3. **Auf Gerät/Emulator ausführen:**
   - Gerät per USB verbinden oder Emulator starten
   - **Run** → **Run 'app'** (Shift+F10)

4. **Logs anzeigen:**
   - **View** → **Tool Windows** → **Logcat**
   - Filter: "ChecklistApp" eingeben

## 📦 Produktions-Deployment

### 1. Release Keystore erstellen

```bash
# Keystore für App-Signierung erstellen
keytool -genkey -v -keystore checklist-release-key.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias checklist \
    -dname "CN=Feuerwehr Checklist, OU=IT, O=Feuerwehr, L=Stadt, ST=State, C=DE"

# Keystore sicher aufbewahren!
# Passwort notieren und sicher speichern
```

### 2. Release APK erstellen in Android Studio

1. **Build Variant ändern:**
   - **View** → **Tool Windows** → **Build Variants**
   - "release" auswählen

2. **Signed APK/Bundle erstellen:**
   - **Build** → **Generate Signed Bundle / APK...**
   - **APK** auswählen → **Next**
   - Keystore auswählen oder erstellen
   - Keystore-Daten eingeben → **Next**
   - **release** Build Type auswählen
   - **Finish**

3. **Alternative: Unsigned APK:**
   - **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
   - Manuelle Signierung mit Keystore erforderlich

### 3. APK signieren (automatisch mit Build-Script)

Wenn `checklist-release-key.jks` vorhanden ist, wird die APK automatisch signiert:

```bash
# Manuelle Signierung (falls nötig)
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore checklist-release-key.jks \
    app/build/outputs/apk/release/app-release-unsigned.apk \
    checklist

# APK optimieren
zipalign -v 4 \
    app/build/outputs/apk/release/app-release-unsigned.apk \
    app/build/outputs/apk/release/FeuerwehrChecklist-release.apk
```

## 🏪 Distribution

### Option 1: Direkte APK-Verteilung

**Für interne Feuerwehr-Verteilung:**

```bash
# Release APK erstellen
./scripts/build.sh release

# APK verteilen
# Datei: android/app/build/outputs/apk/release/FeuerwehrChecklist-release.apk
```

**Installation auf Geräten:**
1. APK-Datei auf Gerät übertragen
2. "Unbekannte Quellen" in Android-Einstellungen aktivieren
3. APK-Datei tippen und installieren

### Option 2: Google Play Store (Enterprise)

**Für größere Verteilung:**

1. **Play Console Account einrichten:**
   - Developer-Account bei Google Play Console erstellen
   - Einmalige Registrierungsgebühr (25$)

2. **App Bundle erstellen:**
   ```bash
   ./gradlew bundleRelease
   # Erzeugt: app/build/outputs/bundle/release/app-release.aab
   ```

3. **Play Console Upload:**
   - App-Bundle in Play Console hochladen
   - Store Listing konfigurieren
   - Screenshots und Beschreibungen hinzufügen
   - Review-Prozess durchlaufen (1-3 Tage)

### Option 3: Firebase App Distribution

**Für Beta-Testing:**

```bash
# Firebase CLI installieren
npm install -g firebase-tools

# Firebase-Projekt initialisieren
firebase login
firebase init

# APK zu Firebase Distribution hochladen
firebase appdistribution:distribute \
    app/build/outputs/apk/release/FeuerwehrChecklist-release.apk \
    --app YOUR_FIREBASE_APP_ID \
    --groups "feuerwehr-testers"
```

## 🔧 Konfiguration

### App-Konfiguration

**Wichtige Dateien:**
- `app/src/main/AndroidManifest.xml` - App-Berechtigungen und Konfiguration
- `app/src/main/res/xml/network_security_config.xml` - Netzwerk-Sicherheit
- `app/build.gradle` - Build-Konfiguration

### URL-Konfiguration

**Produktions-URL ändern:**
```kotlin
// In MainActivity.kt
companion object {
    private const val APP_URL = "https://checklist.svoboda.click"
}
```

**Für andere Domains:**
1. `APP_URL` in `MainActivity.kt` ändern
2. `network_security_config.xml` entsprechend anpassen
3. Deep-Links im `AndroidManifest.xml` aktualisieren

## 🔒 Sicherheit

### Zertifikat-Pinning aktivieren

**In `network_security_config.xml`:**
```xml
<pin-set expiration="2026-01-01">
    <pin digest="SHA-256">YOUR_CERTIFICATE_PIN_HERE</pin>
</pin-set>
```

### Zertifikat-Pin ermitteln:
```bash
# SSL-Zertifikat abrufen
openssl s_client -servername checklist.svoboda.click \
    -connect checklist.svoboda.click:443 </dev/null | \
    openssl x509 -pubkey -noout | \
    openssl rsa -pubin -outform der | \
    openssl dgst -sha256 -binary | \
    openssl enc -base64
```

## 📱 Device Testing

### Emulator Setup

```bash
# Android Emulator erstellen
avdmanager create avd -n "Test_Device" -k "system-images;android-30;google_apis;x86_64"

# Emulator starten
emulator -avd Test_Device

# APK installieren
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Physische Geräte

```bash
# USB-Debugging aktivieren (Entwickleroptionen)
# Gerät verbinden und autorisieren

# Geräte auflisten
adb devices

# APK installieren
adb install -r app/build/outputs/apk/release/FeuerwehrChecklist-release.apk

# App starten
adb shell am start -n "com.feuerwehr.checklist/.MainActivity"
```

## 🔄 Continuous Integration

### GitHub Actions Workflow

**.github/workflows/android.yml:**
```yaml
name: Android CI

on:
  push:
    branches: [ main ]
    paths: [ 'android/**' ]
  pull_request:
    branches: [ main ]
    paths: [ 'android/**' ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: ~/.gradle/caches
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle') }}
        restore-keys: ${{ runner.os }}-gradle
        
    - name: Grant execute permission for gradlew
      run: chmod +x android/gradlew
      
    - name: Build Debug APK
      working-directory: ./android
      run: ./gradlew assembleDebug
      
    - name: Run Tests
      working-directory: ./android
      run: ./gradlew test
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: android/app/build/outputs/apk/debug/
```

## 📊 Monitoring und Analytics

### Crash Reporting Setup

**Firebase Crashlytics:**
```gradle
// In app/build.gradle
implementation 'com.google.firebase:firebase-crashlytics:18.4.3'
implementation 'com.google.firebase:firebase-analytics:21.3.0'
```

### Performance Monitoring

**App-Metriken überwachen:**
- Startup-Zeit
- WebView-Ladezeiten
- Offline-Cache-Performance
- Netzwerk-Request-Erfolg

## 🐛 Troubleshooting

### Häufige Build-Probleme

**1. Gradle Sync Fehler:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

**2. Keystore-Probleme:**
```bash
# Keystore-Informationen anzeigen
keytool -list -v -keystore checklist-release-key.jks
```

**3. SDK-Version-Konflikte:**
```bash
# SDK Manager updaten
sdkmanager --update
sdkmanager "platforms;android-34"
```

### Runtime-Probleme

**WebView lädt nicht:**
- Network Security Config prüfen
- Internet-Berechtigung vorhanden?
- HTTPS-Zertifikat gültig?

**Offline-Modus funktioniert nicht:**
- Storage-Berechtigung erteilt?
- Cache-Verzeichnis zugänglich?
- Service Worker registriert?

## 📋 Checkliste vor Release

- [ ] **Build erfolgreich:** `./scripts/build.sh release`
- [ ] **App-Signierung:** Keystore vorhanden und APK signiert
- [ ] **Funktionstest:** Installation und Grundfunktionen testen
- [ ] **Netzwerk-Test:** Online/Offline-Modi testen
- [ ] **Sicherheitstest:** HTTPS-Verbindung und Zertifikate prüfen
- [ ] **Performance-Test:** App-Start und WebView-Laden messen
- [ ] **Kompatibilitätstest:** Verschiedene Android-Versionen testen
- [ ] **Dokumentation:** README und Deployment-Guide aktualisieren

## 📞 Support

Bei Problemen oder Fragen:

1. **Logs sammeln:**
   ```bash
   adb logcat -s ChecklistApp:D > android-app-logs.txt
   ```

2. **Issue erstellen:** GitHub-Issue mit Logs und Geräteinformationen

3. **Debug-Build testen:** Vor Release immer Debug-Version testen

---

**Wichtig:** Keystore-Datei und Passwörter sicher aufbewahren! Ohne diese können keine App-Updates veröffentlicht werden.