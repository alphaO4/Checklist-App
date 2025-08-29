 # Checklist App für Fahrzeugcheck

## Ideen / Features
- [ ] Wenn TÜV schon vorhanden nur sagen ob stimmt/falsch
- [ ] Drop down bzw. Checkbox für Checklisten-Items
- [ ] Schreib Zugriff für bestimmte Nutzer um Listen anzulegen bzw. zu erweitern
- [ ] Jeder Nutzer hat Account
- [ ] Jeder Account ist in einer Gruppe
- [ ] Jede Gruppe ist einer Fahrzeuggruppe zugeordnet
- [ ] Fahrzeuggruppe enthält mehrere Fahrzeuge (z.B. MTF, RTB und FR)
- [ ] Gruppenleiter und Organisator kann Gruppenzuordnung ändern
- [ ] Abgelaufene TÜV Termine werden extra angezeigt

## Zusätzliche Anforderungen

### 🔐 Benutzer & Berechtigung
- [ ] Registrierung, Login und Passwort-Reset
- [ ] Rollenverwaltung (Benutzer, Gruppenleiter, Organisator, Admin)
- [ ] Berechtigungen definieren (wer kann Checklisten/Fahrzeuge erstellen/bearbeiten/löschen)
- [ ] Zugriffskontrolle für Gruppenzuweisungen

### ✅ Checklisten-Features
- [ ] Checklisten erstellen, bearbeiten und löschen
- [ ] Checklisten bestimmten Fahrzeugen oder Gruppen zuweisen
- [ ] Checklisten-Items als erledigt/unerledigt markieren
- [ ] Historie/Log von abgeschlossenen Checklisten
- [ ] Vorlagen für wiederkehrende Checks

### 🔔 Benachrichtigungen
- [ ] Erinnerungen für ablaufende oder abgelaufene TÜV-Termine
- [ ] Benachrichtigungen für zugewiesene Aufgaben oder Checklisten-Updates
- [ ] E-Mail/Push-Benachrichtigungen

### 🔄 Datensynchronisation
- [ ] Echtzeit-Datensync (WebSockets)
- [ ] Offline-Unterstützung mit späterer Synchronisation
- [ ] Konflikterkennung bei gleichzeitigen Änderungen

### 📱 Mobile Unterstützung
- [ ] Responsive Design für mobile Geräte
- [ ] Touch-optimierte Bedienung
- [ ] Offline-Funktionalität für mobile Nutzung

### 🔍 Audit & Logging
- [ ] Änderungen an Checklisten, Fahrzeugen und Gruppenzuweisungen protokollieren
- [ ] Benutzeraktivitäten nachverfolgbar machen
- [ ] Exportfunktion für Reports

### 🔒 Sicherheit & Backup
- [ ] Regelmäßige Datensicherung
- [ ] Sichere Speicherung sensibler Daten
- [ ] DSGVO-konforme Datenhaltung
- [ ] Verschlüsselung der Datenübertragung

### 🌐 API & Integration
- [ ] REST oder GraphQL API für Frontend-Backend-Kommunikation
- [ ] Dokumentierte API-Endpoints
- [ ] Möglichkeit zur Integration externer Systeme

### 🌍 Lokalisierung
- [ ] Mehrsprachige Unterstützung (Deutsch, Englisch)
- [ ] Datums- und Zeitformate regional anpassbar

## Infra/Tech
- **Frontend**: Electron (für Web und Mobile Apps)
- **Real-time**: WebSockets für Live-Sync von Tasks
- **Hosting**: Ubuntu VM auf Proxmox
- **Datenbank**: PostgreSQL
- **Backend**: Python/FastAPI
- **Authentication**: JWT

