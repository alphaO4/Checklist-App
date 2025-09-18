# Android Studio Project Configuration

## Feuerwehr Checklist - Android Studio Setup

Diese Datei enthält spezifische Anweisungen für die Entwicklung mit Android Studio.

## 🏗️ Android Studio Setup

### Minimale Anforderungen
- **Android Studio:** Giraffe (2022.3.1) oder neuer
- **JDK:** Version 11 oder 17 (wird von Android Studio bereitgestellt)
- **Android SDK:** API Level 24 (Android 7.0) bis 34 (Android 14)
- **RAM:** Mindestens 8 GB (16 GB empfohlen)

### Erste Einrichtung

1. **Android Studio installieren:**
   - Von [developer.android.com](https://developer.android.com/studio) herunterladen
   - Standard-Installation mit Android SDK durchführen

2. **SDK-Komponenten installieren:**
   - **Tools** → **SDK Manager**
   - **SDK Platforms:** Android 7.0 (API 24) bis Android 14 (API 34)
   - **SDK Tools:** Android Build Tools 30.0.3+, Android Emulator, Platform Tools

3. **Projekt öffnen:**
   ```
   Android Studio → Open → e:\Github\Checklist-App\android
   ```

## 🔧 Projekt-Konfiguration

### Build-Konfiguration

**Build Variants:**
- **debug:** Entwicklung mit localhost-Unterstützung
- **release:** Produktion nur HTTPS zu checklist.svoboda.click

**Gradle-Tasks (über Terminal in Android Studio):**
- `./gradlew assembleDebug` - Debug APK erstellen
- `./gradlew assembleRelease` - Release APK erstellen
- `./gradlew installDebug` - Debug APK installieren
- `./gradlew clean` - Build-Cache leeren

### Run Configurations

**Standard-Konfigurationen:**
1. **app (Debug)** - Normale Entwicklung
2. **app (Release)** - Release-Testing

**Custom Run Configuration erstellen:**
1. **Run** → **Edit Configurations**
2. **Add New Configuration** → **Android App**
3. Module: `Checklist-App.android.app`
4. Launch Activity: `com.feuerwehr.checklist.MainActivity`

### Debugging-Setup

**Logcat-Filter:**
```
Tag: ChecklistApp
Package: com.feuerwehr.checklist
Log Level: Debug
```

**Breakpoints setzen:**
- Kotlin-Code: Direkt in Android Studio
- WebView-Debugging: Chrome DevTools aktivieren

## 📱 Device/Emulator Setup

### AVD (Android Virtual Device) erstellen

1. **Tools** → **AVD Manager**
2. **Create Virtual Device**
3. **Phone** → **Pixel 4** (empfohlen)
4. **System Image:** API 30 (Android 11)
5. **Advanced Settings:**
   - RAM: 2048 MB
   - Internal Storage: 6 GB
   - SD Card: 512 MB

### Physisches Gerät verbinden

1. **USB-Debugging aktivieren:**
   - Einstellungen → Über das Telefon → 7x auf Build-Nummer tippen
   - Entwickleroptionen → USB-Debugging aktivieren

2. **Gerät autorisieren:**
   - Gerät per USB verbinden
   - Autorisierung auf Gerät bestätigen

3. **Verbindung testen:**
   ```bash
   adb devices
   # Sollte verbundenes Gerät anzeigen
   ```

## 🛠️ Entwicklungs-Workflow

### 1. Code-Änderungen

**Hauptdateien:**
- `MainActivity.kt` - Haupt-WebView-Logic
- `AndroidManifest.xml` - App-Berechtigungen
- `network_security_config.xml` - HTTPS-Konfiguration

**Änderungen testen:**
1. Code bearbeiten
2. **Build** → **Make Project** (Ctrl+F9)
3. **Run** → **Run 'app'** (Shift+F10)

### 2. WebView-Debugging

**Chrome DevTools aktivieren:**
```kotlin
// In MainActivity.kt (nur für Debug)
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

**Chrome DevTools öffnen:**
1. Chrome öffnen → `chrome://inspect`
2. Gerät und WebView auswählen
3. **Inspect** klicken

### 3. Layout-Änderungen

**Layout-Editor verwenden:**
1. `res/layout/activity_main.xml` öffnen
2. **Design**-Tab für visuellen Editor
3. **Code**-Tab für XML-Bearbeitung
4. **Split**-Tab für beide Ansichten

### 4. Resource-Verwaltung

**Neue Ressourcen hinzufügen:**
- **res/drawable/** - Icons und Grafiken
- **res/values/strings.xml** - Text-Übersetzungen
- **res/values/colors.xml** - Farbdefinitionen
- **res/layout/** - UI-Layouts

## 🔍 Debugging und Testing

### Logcat verwenden

**Filter erstellen:**
1. **Logcat** öffnen
2. Filter-Name: "ChecklistApp"
3. **Log Tag:** `ChecklistApp|WebViewClient|OfflineManager`
4. **Log Level:** Debug

**Custom Logs hinzufügen:**
```kotlin
import android.util.Log

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ChecklistApp"
    }
    
    private fun logDebug(message: String) {
        Log.d(TAG, message)
    }
}
```

### Performance-Monitoring

**Memory Profiler:**
1. **View** → **Tool Windows** → **Profiler**
2. App auswählen und starten
3. **Memory** auswählen für RAM-Analyse

**Network Inspector:**
1. **Profiler** → **Network**
2. HTTPS-Requests zu checklist.svoboda.click überwachen

### Layout Inspector

**UI-Hierarchie analysieren:**
1. **Tools** → **Layout Inspector**
2. Gerät und Prozess auswählen
3. Live-View der UI-Struktur

## 🚀 Build und Deployment

### Debug-Build

**Via Android Studio:**
1. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. APK-Pfad: `app/build/outputs/apk/debug/app-debug.apk`

**Installation:**
- Direkt über **Run** Button
- Oder manuell: **Build** → **Install APK**

### Release-Build

**Signed APK erstellen:**
1. **Build** → **Generate Signed Bundle / APK...**
2. **APK** auswählen
3. Keystore-Pfad: `checklist-release-key.jks`
4. Key-Alias: `checklist`
5. **release** Build Variant auswählen

**Keystore in Android Studio erstellen:**
1. **Build** → **Generate Signed Bundle / APK...**
2. **Create new...** bei Key store path
3. Formular ausfüllen und Keystore erstellen

## 🔧 Erweiterte Konfiguration

### Gradle-Eigenschaften

**gradle.properties anpassen:**
```properties
# Performance-Optimierung
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.configureondemand=true

# Android-spezifisch
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
```

### Build-Optimierung

**app/build.gradle erweitern:**
```gradle
android {
    buildTypes {
        debug {
            debuggable true
            minifyEnabled false
            // WebView-Debugging aktivieren
            buildConfigField "boolean", "WEBVIEW_DEBUG", "true"
        }
        
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            buildConfigField "boolean", "WEBVIEW_DEBUG", "false"
        }
    }
}
```

### Custom Build Types

**Staging-Umgebung hinzufügen:**
```gradle
android {
    buildTypes {
        staging {
            initWith debug
            debuggable false
            buildConfigField "String", "APP_URL", '"https://staging.checklist.svoboda.click"'
        }
    }
}
```

## 📋 Troubleshooting

### Häufige Android Studio Probleme

**1. Gradle Sync Failed:**
- **File** → **Invalidate Caches and Restart**
- **File** → **Sync Project with Gradle Files**

**2. SDK/NDK Fehler:**
- **File** → **Project Structure** → **SDK Location**
- Pfade prüfen und ggf. korrigieren

**3. Build Fehler:**
- **Build** → **Clean Project**
- **Build** → **Rebuild Project**

**4. Emulator-Probleme:**
- **Tools** → **AVD Manager** → **Wipe Data**
- Emulator neu starten

### Performance-Probleme

**Android Studio beschleunigen:**
1. **File** → **Settings** → **Appearance & Behavior** → **System Settings**
2. **Memory Settings:** Heap size auf 2048 MB erhöhen
3. **Compiler:** Build process heap size auf 2048 MB

## 📚 Nützliche Plugins

**Empfohlene Android Studio Plugins:**
1. **Kotlin** (vorinstalliert)
2. **Android APK Analyzer** (vorinstalliert)
3. **GitToolBox** - Git-Integration verbessern
4. **Rainbow Brackets** - Code-Lesbarkeit
5. **Material Theme UI** - Schöneres Interface

**Plugin-Installation:**
1. **File** → **Settings** → **Plugins**
2. **Marketplace** durchsuchen
3. Plugin installieren und IDE neustarten

---

## 💡 Tipps für effiziente Entwicklung

- **Shortcut-Liste:** **Help** → **Keymap Reference**
- **Live Templates:** Häufige Code-Schnipsel definieren
- **Code-Style:** **File** → **Settings** → **Editor** → **Code Style** → **Kotlin**
- **Git-Integration:** Direkt in Android Studio verwenden
- **Instant Run:** Für schnellere Debug-Builds (wenn verfügbar)