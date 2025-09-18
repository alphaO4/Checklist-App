# Feuerwehr Checklist Android App

Ein Android-Wrapper für die Feuerwehr Fahrzeugprüfungs-Anwendung mit Offline-Unterstützung.

## Übersicht

Diese Android-Anwendung ist ein WebView-Wrapper für die Feuerwehr Checklist App, die unter `https://checklist.svoboda.click` gehostet wird. Die App bietet:

- **Sichere HTTPS-Verbindung** mit TLS-Verschlüsselung
- **Offline-Modus** für die Nutzung ohne Internetverbindung
- **Native Android-Integration** mit Material Design
- **Automatische Synchronisation** wenn eine Verbindung verfügbar ist
- **Deep-Link-Unterstützung** für checklist.svoboda.click URLs

## Features

### Sicherheit
- Network Security Configuration für sichere HTTPS-Verbindungen
- Keine Cleartext-Traffic erlaubt
- Certificate Pinning vorbereitet
- Content Security Policy Enforcement

### Offline-Funktionalität
- Automatisches Caching der Hauptanwendung
- Offline-Seite bei fehlender Internetverbindung
- Background-Synchronisation mit WorkManager
- Lokale Speicherung wichtiger Daten

### Benutzeroberfläche
- Material Design 3 mit Feuerwehr-Farben (Rot-Schema)
- Splash Screen beim App-Start
- Verbindungsstatusanzeige
- Pull-to-Refresh Funktionalität
- Floating Action Button für Offline-Modus

## Installation und Build

### Voraussetzungen

- **Android Studio Giraffe (2022.3.1) oder neuer** (empfohlen)
- Android SDK API Level 24+ (Android 7.0)
- JDK 11 oder 17 (wird von Android Studio bereitgestellt)
- Kotlin 1.9.0+ (in Android Studio integriert)

### 🚀 Schnellstart mit Android Studio

1. **Android Studio öffnen** → **Open** → `e:\Github\Checklist-App\android`
2. **Gradle-Sync abwarten** (automatisch beim Öffnen)
3. **Build Variant** auswählen: "debug" für Entwicklung
4. **Run-Button** klicken (grüner Pfeil) → App wird gebaut und installiert

📖 **Detaillierte Anleitung:** Siehe [QUICK_START.md](QUICK_START.md) und [ANDROID_STUDIO.md](ANDROID_STUDIO.md)

### Build-Prozess mit Android Studio

1. **Projekt in Android Studio öffnen:**
   - Android Studio starten
   - "Open an Existing Project" auswählen
   - Ordner `e:\Github\Checklist-App\android` auswählen
   - Gradle-Sync abwarten (automatisch)

2. **Build-Konfiguration:**
   - **Build Menu** → **Select Build Variant**
   - **debug** für Entwicklung auswählen
   - **release** für Produktion auswählen

3. **APK erstellen:**
   - **Build Menu** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
   - Oder **Build Menu** → **Generate Signed Bundle / APK** für Release

4. **App ausführen:**
   - Gerät/Emulator verbinden
   - **Run** Button (grüner Pfeil) klicken
   - Oder **Run Menu** → **Run 'app'**

### Build-Varianten

- **Debug:** Unterstützt localhost/development URLs
- **Release:** Nur HTTPS-Verbindungen zu checklist.svoboda.click

## Projektstruktur

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/feuerwehr/checklist/
│   │   │   ├── MainActivity.kt          # Haupt-Activity mit WebView
│   │   │   ├── SplashActivity.kt        # Splash Screen
│   │   │   ├── utils/
│   │   │   │   ├── NetworkUtils.kt      # Netzwerk-Hilfsfunktionen
│   │   │   │   └── OfflineManager.kt    # Offline-Cache Management
│   │   │   └── webview/
│   │   │       ├── ChecklistWebViewClient.kt     # WebView-Client
│   │   │       └── ChecklistWebChromeClient.kt   # Chrome-Client
│   │   ├── res/
│   │   │   ├── layout/                  # UI-Layouts
│   │   │   ├── values/                  # Strings, Colors, Styles
│   │   │   ├── drawable/                # Icons und Grafiken
│   │   │   ├── mipmap-*/               # App-Icons
│   │   │   └── xml/                     # Network Security Config
│   │   └── AndroidManifest.xml         # App-Manifest
│   ├── build.gradle                     # App-Level Build Config
│   └── proguard-rules.pro              # ProGuard-Regeln
├── build.gradle                        # Projekt-Level Build Config
├── gradle.properties                   # Gradle-Eigenschaften
└── settings.gradle                     # Gradle-Einstellungen
```

## Konfiguration

### Network Security Configuration

Die App verwendet eine strenge Network Security Configuration:

```xml
<!-- Nur HTTPS für checklist.svoboda.click -->
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">checklist.svoboda.click</domain>
    <trust-anchors>
        <certificates src="system"/>
    </trust-anchors>
</domain-config>
```

### WebView-Konfiguration

- JavaScript aktiviert für App-Funktionalität
- DOM Storage und Database Storage aktiviert
- Mixed Content blockiert (nur HTTPS)
- File Access deaktiviert aus Sicherheitsgründen
- Custom User Agent: "FeuerwehrChecklistApp/1.0"

## Deployment

### APK-Erstellung für Produktion

1. **Keystore erstellen:**
   ```bash
   keytool -genkey -v -keystore checklist-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias checklist
   ```

2. **Release Build:**
   ```bash
   ./gradlew assembleRelease
   ```

3. **APK signieren:**
   ```bash
   jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore checklist-release-key.jks app-release-unsigned.apk checklist
   zipalign -v 4 app-release-unsigned.apk checklist-app-release.apk
   ```

### Play Store Deployment

Für Play Store Deployment:
- App Bundle erstellen: `./gradlew bundleRelease`
- App in Google Play Console hochladen
- Store Listing und Screenshots hinzufügen
- Review-Prozess durchlaufen

## Entwicklung

### Debug-Modus

Im Debug-Modus sind zusätzliche URLs erlaubt:
- `127.0.0.1` (localhost)
- `10.0.2.2` (Android Emulator)

### Testing

```bash
# Unit Tests ausführen
./gradlew test

# Instrumented Tests (erfordern verbundenes Gerät)
./gradlew connectedAndroidTest
```

### Logging

LogCat-Tags:
- `ChecklistApp`: Hauptanwendung
- `WebViewClient`: WebView-Events
- `OfflineManager`: Cache-Management
- `NetworkUtils`: Netzwerkstatus

## Offline-Funktionalität

### Cache-Management

- **Automatisches Caching:** Hauptanwendung wird bei ersten Laden gecacht
- **Asset-Caching:** CSS, JS und andere kritische Dateien werden gespeichert
- **Background-Sync:** Alle 6 Stunden bei verfügbarer Verbindung
- **Cache-Größe:** Begrenzt auf App-Cache-Verzeichnis

### Offline-Erkennung

```kotlin
// Netzwerkstatus prüfen
if (NetworkUtils.isNetworkAvailable(context)) {
    // Online-Modus
    webView.loadUrl("https://checklist.svoboda.click")
} else {
    // Offline-Modus
    offlineManager.loadOfflineContent(webView)
}
```

## Sicherheitshinweise

- **Certificate Pinning:** Kann in `network_security_config.xml` aktiviert werden
- **Root Detection:** Nicht implementiert (kann bei Bedarf hinzugefügt werden)
- **Code Obfuscation:** ProGuard-Regeln für Release-Builds aktiviert
- **Backup-Regeln:** Sichere App-Daten in Android-Backups

## Troubleshooting

### Häufige Probleme

1. **SSL/TLS Fehler:**
   - Network Security Config prüfen
   - System-Zeiteinstellungen überprüfen
   - CA-Zertifikate aktualisieren

2. **WebView lädt nicht:**
   - JavaScript aktiviert?
   - Netzwerkverbindung vorhanden?
   - URL in `shouldOverrideUrlLoading` erlaubt?

3. **Offline-Modus funktioniert nicht:**
   - Cache-Verzeichnis zugänglich?
   - Speicherplatz verfügbar?
   - WorkManager-Berechtigung erteilt?

### Logs überprüfen

```bash
adb logcat -s ChecklistApp:D WebViewClient:D
```

## Lizenz

Diese Android-App ist Teil des Feuerwehr Checklist Projekts und unterliegt derselben Lizenz wie das Hauptprojekt.

## Support

Bei Problemen oder Fragen:
1. Issues im GitHub-Repository erstellen
2. LogCat-Ausgaben beifügen
3. Device-Informationen angeben (Android-Version, Hersteller)
4. Schritte zur Reproduktion beschreiben