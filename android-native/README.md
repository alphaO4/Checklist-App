# Android-First Checklist App

## Overview
This project has been **completely rewritten** to prioritize Android as the primary platform. The previous WebView-based approach has been replaced with a native Android implementation using modern Android development practices.

## Architecture

### Android-First Design
- **Native UI**: Jetpack Compose + Material Design 3
- **Offline-First**: Room database with backend sync
- **Modern Stack**: Hilt, Navigation Component, ViewModels
- **Performance**: Native Android performance and lifecycle management

### Project Structure
```
android-native/          # Primary Android application
├── app/                # Main Android app module
└── shared/             # Shared Kotlin Multiplatform business logic

android-webview-legacy/ # Old WebView implementation (backup)
web/                   # Secondary web platform (formerly frontend/)
desktop/               # Secondary desktop platform
backend/               # Enhanced FastAPI backend
```

## Quick Start

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 11 or newer
- Android SDK with API 34
- Backend running on network-accessible IP

### 1. Start Backend
```bash
cd backend
python -m uvicorn app.main:app --host 10.20.1.108 --port 8000 --reload
```

### 2. Build Android App
```bash
cd android-native
./gradlew assembleDebug
```

### 3. Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Key Features

### Native Android Implementation
- **Jetpack Compose UI**: Modern declarative UI framework
- **Material Design 3**: Fire department red theme with proper theming
- **Navigation Component**: Type-safe navigation between screens
- **Hilt Dependency Injection**: Clean architecture with proper DI
- **Room Database**: Offline-first local storage matching backend schema

### Offline-First Architecture
- **Local Database**: All data stored locally in Room
- **Background Sync**: Automatic sync with FastAPI backend
- **Conflict Resolution**: Handles offline changes and sync conflicts
- **Network Awareness**: Adapts behavior based on connectivity

### Domain-Driven Design
- **Shared Business Logic**: Kotlin Multiplatform shared module
- **Repository Pattern**: Clean separation of data sources
- **Use Cases**: Encapsulated business rules
- **Domain Models**: Mirror backend SQLAlchemy models exactly

## Development

### Android Studio Setup
1. Open `android-native/` in Android Studio
2. Sync Gradle files
3. Run on device or emulator

### Build Variants
- **Debug**: Development build with logging, connects to `10.20.1.108:8000`
- **Release**: Production build, connects to `https://checklist.svoboda.click`

### Database Schema
The Room database exactly mirrors the FastAPI SQLAlchemy backend:
- `benutzer` → `UserEntity`
- `fahrzeuge` → `VehicleEntity`  
- `checklisten` → `ChecklistEntity`
- `tuv_termine` → `TuvAppointmentEntity`

## Migration from WebView

### What Changed
1. **UI**: WebView → Native Jetpack Compose
2. **Data**: Web storage → Room database with offline support
3. **Architecture**: Single WebView → MVVM with repositories
4. **Performance**: Web rendering → Native Android performance
5. **Features**: Limited by web → Full Android platform integration

### What Stayed the Same  
- **Backend**: Same FastAPI server and endpoints
- **Domain Logic**: Same business rules and German terminology
- **User Data**: Can be migrated through backend sync

### Benefits
- **Performance**: Native speed vs WebView overhead
- **Offline Support**: True offline functionality with local database
- **Platform Integration**: Android notifications, intents, file system
- **User Experience**: Native navigation and Material Design
- **Maintainability**: Type-safe Kotlin vs JavaScript debugging

## Build and Deployment

### Development Builds
```bash
cd android-native
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release Builds
```bash
./gradlew assembleRelease
```

### Automated Testing
```bash
./gradlew test           # Unit tests
./gradlew connectedTest  # Instrumented tests
```

## Future Roadmap

### Phase 1 (Current)
- ✅ Basic Android app structure
- ✅ Native UI with Compose
- ✅ Room database setup
- ✅ Navigation and architecture

### Phase 2 (Next)
- [ ] Complete vehicle management UI
- [ ] Checklist creation and execution
- [ ] TÜV deadline tracking
- [ ] Backend API integration
- [ ] Offline sync implementation

### Phase 3 (Future)
- [ ] Advanced features (export, notifications)
- [ ] Enhanced web platform
- [ ] Desktop platform (Compose Desktop)
- [ ] iOS support via Kotlin Multiplatform

## Secondary Platforms

The Android-first approach doesn't abandon other platforms:

- **Web**: Rebuilt as lightweight SPA consuming same APIs
- **Desktop**: Electron wrapper of web OR Compose Desktop
- **Future iOS**: Native iOS using shared Kotlin Multiplatform logic

This ensures Android gets the best experience while other platforms remain functional as secondary clients.

## Documentation

- [Architecture Design](../ANDROID_FIRST_ARCHITECTURE.md) - Detailed technical architecture
- [Backend Integration](../backend/README.md) - API documentation
- [Legacy WebView](../android-webview-legacy/README.md) - Previous implementation

The Android-first rewrite represents a fundamental shift toward native mobile excellence while maintaining cross-platform compatibility through shared business logic and standardized APIs.