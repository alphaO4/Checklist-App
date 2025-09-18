# Android-First Architecture Design

## Overview
Transform the Checklist App from Electron-first to Android-first architecture, making native Android the primary platform while maintaining cross-platform compatibility.

## Core Principles
1. **Android-Native First**: Native Android UI components, proper lifecycle, Material Design
2. **Offline-First**: Local Room database with sync to FastAPI backend  
3. **Shared Business Logic**: Kotlin Multiplatform or well-defined interfaces
4. **Progressive Enhancement**: Web and Desktop as secondary platforms consuming same APIs

## New Architecture Stack

### 1. Android (Primary Platform)
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM with ViewModels, Repository pattern
- **Local Database**: Room with Entity relationships matching backend SQLAlchemy models
- **Networking**: Retrofit + OkHttp with offline caching
- **Dependency Injection**: Hilt/Dagger
- **Navigation**: Jetpack Navigation Component

### 2. Shared Layer (Business Logic)
- **Option A**: Kotlin Multiplatform Mobile (KMM)
  - Shared data models, repository interfaces, business rules
  - Platform-specific implementations for Android, iOS future
- **Option B**: Standardized API contracts
  - Well-defined REST/GraphQL APIs that all platforms consume
  - Shared TypeScript/Kotlin interfaces

### 3. Backend (Unchanged Core, Enhanced)
- **Current FastAPI**: Keep existing SQLAlchemy models and endpoints
- **Enhanced**: Add mobile-optimized endpoints (pagination, batch operations)
- **Sync Layer**: Conflict resolution, incremental sync, offline support

### 4. Secondary Platforms
- **Web**: React/Vue.js SPA consuming same REST APIs
- **Desktop**: Electron wrapper around web version OR native using Compose Desktop
- **Future iOS**: Native iOS using shared Kotlin Multiplatform business logic

## Domain Model Mapping

### Backend SQLAlchemy → Android Room
```
Backend (Python)           Android (Kotlin)
├── Benutzer              ├── User @Entity
├── Gruppe                ├── Group @Entity  
├── FahrzeugGruppe        ├── VehicleGroup @Entity
├── Fahrzeug              ├── Vehicle @Entity
├── FahrzeugTyp           ├── VehicleType @Entity
├── TuvTermin             ├── TuvAppointment @Entity
├── Checkliste            ├── Checklist @Entity
├── ChecklistItem         ├── ChecklistItem @Entity
├── ChecklistAusfuehrung  ├── ChecklistExecution @Entity
└── ItemErgebnis          └── ItemResult @Entity
```

### Room Database Schema
- Mirror the exact relationships from SQLAlchemy
- Add sync metadata (lastModified, syncStatus, conflictVersion)
- Foreign key constraints matching backend

## Project Structure Reorganization

```
checklist-app/
├── android/                          # Primary platform
│   ├── app/
│   │   ├── src/main/kotlin/
│   │   │   ├── data/                 # Repository + Room
│   │   │   │   ├── local/            # Room entities, DAOs
│   │   │   │   ├── remote/           # Retrofit APIs
│   │   │   │   ├── repository/       # Repository implementations
│   │   │   │   └── sync/             # Offline sync logic
│   │   │   ├── domain/               # Business logic
│   │   │   │   ├── model/            # Domain models
│   │   │   │   ├── repository/       # Repository interfaces
│   │   │   │   └── usecase/          # Business use cases
│   │   │   ├── presentation/         # UI Layer
│   │   │   │   ├── ui/               # Composable screens
│   │   │   │   ├── viewmodel/        # ViewModels
│   │   │   │   └── navigation/       # Navigation setup
│   │   │   └── di/                   # Dependency injection
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── shared/                           # Shared business logic (KMM)
│   ├── src/
│   │   ├── commonMain/kotlin/        # Shared code
│   │   │   ├── domain/               # Business models
│   │   │   ├── repository/           # Repository interfaces  
│   │   │   └── usecase/              # Business use cases
│   │   ├── androidMain/kotlin/       # Android-specific
│   │   └── iosMain/kotlin/           # Future iOS-specific
│   └── build.gradle.kts
├── backend/                          # Enhanced FastAPI
│   ├── app/
│   │   ├── api/routes/mobile/        # Mobile-optimized endpoints
│   │   ├── sync/                     # Sync and conflict resolution
│   │   └── [existing structure]
├── web/                              # Secondary web platform  
│   ├── src/
│   │   ├── components/               # React/Vue components
│   │   ├── services/                 # API clients
│   │   └── stores/                   # State management
│   └── package.json
├── desktop/                          # Secondary desktop platform
│   ├── electron/                     # Electron wrapper of web
│   └── compose-desktop/              # OR Compose Desktop native
└── docs/                             # Architecture documentation
```

## Implementation Phases

### Phase 1: Core Android Foundation
1. Setup new Android project with modern stack
2. Implement Room database with all entities
3. Create repository pattern with offline-first approach  
4. Basic Retrofit API client
5. Simple Compose UI for main entities (Vehicles, Checklists)

### Phase 2: Advanced Android Features
1. Complete UI with Material Design 3
2. Implement sync mechanism
3. Offline caching and conflict resolution
4. Push notifications for TÜV deadlines
5. Export/Import functionality

### Phase 3: Enhanced Backend
1. Mobile-optimized API endpoints
2. Real-time sync APIs
3. Batch operations for mobile efficiency
4. Enhanced authentication (refresh tokens, device management)

### Phase 4: Secondary Platforms  
1. Web platform consuming same APIs
2. Desktop platform (Electron or Compose Desktop)
3. Shared deployment and CI/CD

## Key Benefits of Android-First Approach

1. **Native Performance**: Direct access to Android APIs, better UX
2. **Offline Capabilities**: Room database enables true offline functionality  
3. **Platform Integration**: Android notifications, file system, intents
4. **Scalability**: Easier to add iOS, maintain web as lightweight clients
5. **Developer Experience**: Modern Android tooling, type safety
6. **User Experience**: Native navigation, Material Design consistency

## Migration Strategy

1. **Parallel Development**: Build new Android app alongside existing system
2. **API Compatibility**: Ensure new mobile APIs work with existing backend
3. **Gradual Migration**: Users can switch between old and new apps during transition
4. **Data Migration**: Export/import tools to move user data
5. **Feature Parity**: Ensure new Android app has all features before deprecating old system

This architecture prioritizes Android while maintaining the flexibility to support other platforms as secondary clients consuming the same backend APIs.