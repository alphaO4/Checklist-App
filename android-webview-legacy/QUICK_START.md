# 🚒 Quick Start: Android Studio Development

## Projekt öffnen und starten

### 1. Android Studio vorbereiten
```
1. Android Studio starten
2. "Open an existing Android Studio project"
3. Ordner auswählen: e:\Github\Checklist-App\android
4. "OK" klicken
5. Gradle-Sync abwarten (2-5 Minuten beim ersten Mal)
```

### 2. SDK und Dependencies prüfen
```
1. File → Project Structure → SDK Location
2. Prüfen: Android SDK path korrekt?
3. Tools → SDK Manager
4. Installiert: API 24-34, Build Tools 30.0.3+
5. File → Sync Project with Gradle Files
```

### 3. App bauen und ausführen
```
1. Build Variant wählen: "debug" (unten links)
2. Gerät/Emulator verbinden
3. Run-Button (grüner Pfeil) klicken
4. App wird installiert und gestartet
```

## 🔧 Android Studio Interface

### Wichtige Bereiche
- **Project Tree** (links): Dateien und Ordner
- **Editor** (mitte): Code-Bearbeitung  
- **Build Variants** (unten links): Debug/Release wechseln
- **Logcat** (unten): App-Logs anzeigen
- **Device Manager** (rechts): Emulator verwalten

### Key Shortcuts
- `Ctrl + F9` - Project bauen
- `Shift + F10` - App ausführen
- `Ctrl + Shift + F9` - Clean + Rebuild
- `Alt + 6` - Logcat öffnen
- `Ctrl + Shift + A` - Aktion suchen

## 📱 Device Setup

### Emulator erstellen
```
1. Tools → Device Manager
2. "Create Virtual Device"
3. Pixel 4 → Next
4. API 30 (Android 11) → Download → Next
5. Finish
6. Play-Button zum Starten
```

### Physisches Gerät
```
1. USB-Debugging aktivieren:
   - Einstellungen → Über das Telefon
   - 7x auf "Build-Nummer" tippen
   - Zurück → Entwickleroptionen
   - USB-Debugging aktivieren

2. Gerät verbinden:
   - USB-Kabel anschließen
   - Autorisierung am Gerät bestätigen
   - In Android Studio sollte Gerät erscheinen
```

## 🔍 Debugging

### Logcat verwenden
```
1. View → Tool Windows → Logcat
2. Filter erstellen:
   - Name: "ChecklistApp"
   - Log Tag: "ChecklistApp"
   - Log Level: Debug
3. App starten und Logs beobachten
```

### WebView debuggen
```
1. Chrome öffnen
2. Adresse: chrome://inspect
3. Gerät mit App finden
4. "inspect" klicken
5. Chrome DevTools öffnen sich
```

### Breakpoints setzen
```
1. Zeile im Kotlin-Code klicken (roter Punkt)
2. Debug-Button (Käfer-Symbol) starten
3. App pausiert bei Breakpoint
4. Variables-Panel zeigt Werte
```

## 🏗️ Build-Prozess

### Debug Build
```
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. APK Pfad: app/build/outputs/apk/debug/app-debug.apk
3. Automatische Installation via Run-Button
```

### Release Build (Signed)
```
1. Build → Generate Signed Bundle / APK...
2. APK auswählen → Next
3. Keystore auswählen/erstellen:
   - Key store path: checklist-release-key.jks
   - Key alias: checklist  
   - Passwörter eingeben
4. release Build Type → Finish
5. APK: app/build/outputs/apk/release/
```

### Keystore erstellen
```
1. Generate Signed Bundle → Create new...
2. Ausfüllen:
   - Key store path: checklist-release-key.jks
   - Password: [sicheres Passwort]
   - Key alias: checklist
   - Validity: 25 Jahre
   - Certificate info ausfüllen
3. OK → Keystore wird erstellt
```

## 📁 Projekt-Struktur

### Wichtige Dateien
```
app/src/main/
├── java/com/feuerwehr/checklist/
│   ├── MainActivity.kt          # Haupt-App-Logic
│   ├── SplashActivity.kt        # Startbildschirm
│   └── utils/                   # Hilfsfunktionen
├── res/
│   ├── layout/                  # UI-Layouts
│   ├── values/                  # Strings, Colors, Styles
│   └── xml/                     # Network Security Config
└── AndroidManifest.xml          # App-Berechtigungen
```

### Konfiguration ändern
```
- App-Name: res/values/strings.xml
- URL ändern: MainActivity.kt (APP_URL)
- Farben: res/values/colors.xml  
- Netzwerk: res/xml/network_security_config.xml
```

## 🚀 Häufige Tasks

### Code-Änderung testen
```
1. Datei bearbeiten (z.B. MainActivity.kt)
2. Ctrl + F9 (Build)
3. Shift + F10 (Run)
4. Änderung in App sichtbar
```

### APK für Verteilung
```
1. Build Variants → "release"
2. Build → Generate Signed Bundle / APK
3. Keystore auswählen
4. APK aus app/build/outputs/apk/release/ verwenden
```

### Logs anzeigen
```
1. App starten
2. Logcat öffnen (Alt + 6)
3. Filter "ChecklistApp" auswählen
4. Log-Nachrichten beobachten
```

### Performance prüfen
```
1. View → Tool Windows → Profiler
2. App-Prozess auswählen
3. Memory/CPU/Network tabs verwenden
4. Performance-Probleme identifizieren
```

## ⚠️ Troubleshooting

### Gradle Sync Fehler
```
1. File → Invalidate Caches and Restart
2. File → Sync Project with Gradle Files
3. Build → Clean Project
4. Build → Rebuild Project
```

### App startet nicht
```
1. Logcat nach Fehlern durchsuchen
2. Device Manager → Emulator neu starten
3. Build → Clean Project → Rebuild
4. Run → Run 'app' erneut versuchen
```

### WebView lädt nicht
```
1. Netzwerk-Verbindung prüfen
2. Logcat nach "ChecklistApp" filtern
3. URL in MainActivity.kt prüfen
4. network_security_config.xml validieren
```

### Build dauert zu lange
```
1. File → Settings → Build, Execution, Deployment
2. Gradle → JVM heap size: 2048 MB
3. gradle.properties:
   org.gradle.parallel=true
   org.gradle.daemon=true
```

## 📋 Checkliste vor Release

- [ ] Debug-Build funktioniert
- [ ] Release-Build erstellt
- [ ] APK signiert
- [ ] Auf verschiedenen Geräten getestet
- [ ] Online/Offline-Modi geprüft
- [ ] Logs auf Fehler durchsucht
- [ ] Performance akzeptabel
- [ ] Keystore sicher gespeichert

---

**💡 Tipp:** Die meisten Probleme lösen sich durch "Clean Project" und "Rebuild Project"!

**🔗 Weitere Hilfe:** 
- Android Studio Dokumentation: https://developer.android.com/studio
- Kotlin Referenz: https://kotlinlang.org/docs/reference/