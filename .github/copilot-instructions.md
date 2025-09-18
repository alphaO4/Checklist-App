# Copilot Instructions for Checklist App

## Project Overview
This is a **Vehicle Inspection Checklist App** for German fire departments (Feuerwehr), enabling organized vehicle safety checks with TÜV (vehicle inspection) deadline tracking.

## Current Implementation Status (MAJOR ARCHITECTURE TRANSFORMATION)
- ✅ **Android-First Native**: Primary platform using Jetpack Compose + Material Design 3
- ✅ **Offline-First**: Room database with background sync to FastAPI backend
- ✅ **Modern Android Stack**: Hilt DI, Navigation Component, MVVM + Repository pattern
- ✅ **Multi-Platform Support**: Web and Desktop as secondary platforms sharing same backend APIs
- ✅ **Cross-Platform Business Logic**: Kotlin Multiplatform shared module for domain models and use cases
- ✅ **Backend**: Enhanced FastAPI with mobile-optimized endpoints and sync support
- ✅ **German Fire Department Domain**: Complete TÜV tracking, vehicle management, and checklist workflows

## Architecture & Tech Stack

### Primary Platform: Android Native (`android-native/`)
- **UI**: Jetpack Compose + Material Design 3 with Fire Department red theme
- **Architecture**: MVVM + Repository pattern with Hilt dependency injection  
- **Database**: Room (SQLite) entities mirroring backend SQLAlchemy models exactly
- **Networking**: Retrofit + OkHttp with offline caching and sync conflict resolution
- **Navigation**: Navigation Component with type-safe Compose destinations
- **Build**: Gradle KSP (not KAPT) for annotation processing to avoid JDK compatibility issues

### Secondary Platforms
- **Web** (`web/`): Vanilla JS/HTML/CSS SPA consuming REST APIs (formerly `frontend/`)
- **Desktop** (`desktop/`): Electron wrapper around web version  
- **Legacy** (`android-webview-legacy/`): Old WebView implementation (backup only)

### Backend (`backend/`)
- **Core**: Python/FastAPI + SQLAlchemy + SQLite (dev) / PostgreSQL (prod)
- **Enhanced**: Mobile sync endpoints, batch operations, conflict resolution
- **Authentication**: JWT-based with German role hierarchy (Benutzer → Gruppenleiter → Organisator → Admin)

## Domain Model (German Fire Department Context)
**CRITICAL**: All entity names, database tables, and UI text use German terminology. This is not translatable - it's the actual domain language.

### Entity Hierarchy
- **Benutzer** (Users) → **Gruppen** (Groups) → **Fahrzeuggruppen** (Vehicle Groups) → **Fahrzeuge** (Vehicles)
- **Checklisten** (Checklists) assigned to vehicles/groups  
- **TÜV-Termine** (mandatory German vehicle inspections) with expiration tracking
- Role hierarchy: `Benutzer` < `Gruppenleiter` < `Organisator` < `Admin`

### Backend SQLAlchemy ↔ Android Room Mapping
```kotlin
// EXACT 1:1 mapping - table names match backend
Backend (Python)              Android Room Entity (Kotlin)
├── benutzer                 ├── UserEntity(tableName="benutzer")
├── gruppen                  ├── GroupEntity(tableName="gruppen")  
├── fahrzeuggruppen          ├── VehicleGroupEntity(tableName="fahrzeuggruppen")
├── fahrzeuge                ├── VehicleEntity(tableName="fahrzeuge")
├── fahrzeugtyp              ├── VehicleTypeEntity(tableName="fahrzeugtyp")
├── tuv_termine              ├── TuvAppointmentEntity(tableName="tuv_termine")
├── checklisten              ├── ChecklistEntity(tableName="checklisten")
└── checklist_items          └── ChecklistItemEntity(tableName="checklist_items")
```

**Key Pattern**: Room entities in `data/local/entity/` have `syncStatus`, `lastModified`, `version` fields for offline-first sync that don't exist in backend models.


## Critical Development Workflows

### Building & Running (UPDATED FOR ANDROID-FIRST)

**Backend (start first):**
```bash
cd backend
.\start-backend.ps1                       # Auto-detects network IP for Android debugging
# OR manually specify:
python -m uvicorn app.main:app --host 10.20.1.108 --port 8000 --reload
# ⚠️ CRITICAL: Use network IP (not 127.0.0.1) for Android device access
```

**Android Native (Primary):**
```bash
cd android-native
./gradlew assembleDebug                   # Build native Android APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Android Development in Android Studio:**
1. Open `android-native/` folder in Android Studio (NOT the root)
2. Ensure backend runs on network IP (use `start-backend.ps1`)
3. Run/Debug directly from Android Studio

**Secondary Platforms:**
```bash
# Web (vanilla JS SPA)
cd web && npm start

# Desktop (Electron wrapper around web)
cd desktop && npm run build && npm start
```

### Build System Architecture

**Android Gradle KSP Pattern:**
- Uses **KSP** (not KAPT) to avoid JDK 11+ compatibility issues
- Hilt annotation processing: `ksp("com.google.dagger:hilt-android-compiler")`  
- Room code generation: `ksp("androidx.room:room-compiler")`
- Build variants: `debug` (local IP) vs `release` (production URL)

# Production HTTPS setup
BACKEND_HOST=api.example.com BACKEND_PROTOCOL=https npm run build
```

**Environment Files:**
- `.env.template` - Configuration template
- `.env.development` - Development defaults
- `.env.production` - Production defaults  
- `.env.local` - Local overrides (gitignored)

**Configuration Variables:**
- `BACKEND_HOST` - Server hostname/IP
- `BACKEND_PORT` - Server port (default: 8000)
- `BACKEND_PROTOCOL` - http/https
- `BACKEND_WS_PROTOCOL` - ws/wss

### Testing & Debugging
```bash
# Backend API tests (backend must be running)
cd backend
python test_api.py                        # Basic API functionality tests
python test_vehicle_editing.py           # Vehicle management tests

# Android debugging (requires backend running on network IP)
adb logcat -d | Select-String -Pattern "checklist|feuerwehr|ERROR"  # Check app logs
adb shell am force-stop com.feuerwehr.checklist.debug              # Force restart
adb uninstall com.feuerwehr.checklist.debug                        # Clean reinstall

# Test backend API directly
Invoke-WebRequest -Uri "http://10.20.1.108:8000/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin"}'
```

### Project Structure
```
android-native/
├── app/build.gradle.kts    # Gradle KSP build configuration
├── src/main/kotlin/com/feuerwehr/checklist/
│   ├── ChecklistApplication.kt        # Hilt Application entry point
│   ├── presentation/                  # UI layer (Compose screens + ViewModels)
│   │   ├── MainActivity.kt           # Single Activity with Compose navigation
│   │   ├── screen/                   # Jetpack Compose screens
│   │   ├── viewmodel/                # ViewModels with Hilt injection
│   │   ├── navigation/               # Navigation Component setup
│   │   └── theme/                    # Material Design 3 theme
│   ├── domain/                       # Business logic + use cases
│   │   ├── model/                    # Domain models
│   │   ├── repository/               # Repository interfaces
│   │   └── usecase/                  # Business use cases
│   ├── data/                         # Data layer implementation
│   │   ├── local/entity/             # Room entities (mirror backend tables)
│   │   ├── local/dao/                # Room DAOs
│   │   ├── remote/api/               # Retrofit API interfaces
│   │   └── repository/               # Repository implementations
│   └── di/                           # Hilt dependency injection modules
backend/app/
├── main.py                 # FastAPI app with enhanced mobile endpoints
├── models/                 # SQLAlchemy domain models (German naming)
├── api/routes/             # REST endpoints + sync endpoints for mobile
├── core/                   # Settings, security, dependencies  
└── services/               # Business logic, seed data
shared/                     # Kotlin Multiplatform business logic
├── src/commonMain/kotlin/  # Shared domain models and interfaces
└── build.gradle.kts        # KMM configuration
```

## Core Implementation Patterns

### Android MVVM + Hilt Pattern
```kotlin
// Repository with Room + Retrofit
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi,
    private val syncManager: SyncManager
) : UserRepository {
    override suspend fun getUsers(): Flow<List<User>> = 
        userDao.getAllUsers().map { entities -> entities.map { it.toDomain() } }
}

// ViewModel with Hilt injection
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authUseCase: AuthenticateUserUseCase
) : ViewModel() {
    // State management with StateFlow
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()
}
```

### Room Entity Pattern (Mirrors Backend)
```kotlin
// EXACT table name matching backend SQLAlchemy
@Entity(tableName = "benutzer")  // Matches backend 'benutzer' table
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val rolle: String,
    val createdAt: Instant,
    // Sync fields for offline-first
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val version: Int = 1
)
```

### Jetpack Compose Navigation Pattern
```kotlin
// Type-safe navigation with Hilt ViewModels
@Composable
fun ChecklistNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { 
            LoginScreen(
                onNavigateToHome = { navController.navigate("dashboard") }
            )
        }
        composable("dashboard") { 
            DashboardScreen(
                onNavigateToVehicles = { navController.navigate("vehicles") }
            )
        }
    }
}
```

### Offline-First Sync Pattern
```kotlin
// Repository handles offline-first with background sync
class VehicleRepositoryImpl {
    // Always return local data first
    override fun getVehicles(): Flow<List<Vehicle>> = 
        vehicleDao.getAllVehicles().map { entities -> entities.map { it.toDomain() } }
    
    // Background sync with conflict resolution
    override suspend fun syncVehicles() {
        try {
            val remoteVehicles = vehicleApi.getVehicles()
            syncManager.resolveConflicts(remoteVehicles, localVehicles)
        } catch (e: Exception) {
            // Graceful degradation - offline mode
        }
    }
}
```

### German Domain Terminology
- **Entities**: `Fahrzeug`, `Fahrzeugtyp`, `Fahrzeuggruppe`, `Gruppe`, `TÜV-Termin`
- **Database**: snake_case (`fahrzeug_id`, `fahrzeugtyp_id`, `ablauf_datum`)
- **UI Text**: Native German (`Fahrzeugtyp hinzufügen`, `TÜV-Termine verwalten`)
- **Status Values**: German (`ok`, `fehler`, `nicht_pruefbar`, `current`, `warning`, `expired`)

## Backend Integration Specifics

### FastAPI Route Structure
```python
# Pattern: Feature-based routers with dependency injection
from ...core.deps import get_current_user
from ...models.vehicle_type import FahrzeugTyp

@router.post("", response_model=FahrzeugTypSchema)
def create_vehicle_type(
    data: FahrzeugTypCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    # Permission checks for organisator/admin roles
    check_admin_permission(current_user)
```

### Authentication Flow
```typescript
// Login stores JWT token, all subsequent requests include Bearer header
const { access_token } = await backendClient.login(username, password);
this.token = access_token; // Stored in tokenStorage for persistence

// Auto-included in requests via authHeaders()
private authHeaders() {
  return this.token ? { 'Authorization': `Bearer ${this.token}` } : {};
}
```

### Domain Model (SQLAlchemy Relationships)
```python
# Core hierarchy: Benutzer → Gruppe → FahrzeugGruppe → Fahrzeug
# Key models in /backend/app/models/:
# - user.py: Benutzer with rolle hierarchy
# - group.py: Gruppe with gruppenleiter relationship  
# - vehicle.py: FahrzeugGruppe, Fahrzeug with fahrzeugtyp
# - checklist.py: TuvTermin, Checkliste, ChecklistItem, ChecklistAusfuehrung, ItemErgebnis
# - vehicle_type.py: FahrzeugTyp (MTF, RTB, LF, etc.)

# Example relationship pattern:
class Fahrzeug(Base):
    fahrzeugtyp = relationship("FahrzeugTyp", back_populates="fahrzeuge")
    fahrzeuggruppe = relationship("FahrzeugGruppe", back_populates="fahrzeuge")
    tuv_termine = relationship("TuvTermin", back_populates="fahrzeug", cascade="all, delete-orphan")
```

## Security & Production Considerations
- **Electron Security**: `contextIsolation: true`, `nodeIntegration: false`, restrictive CSP
- **Window Management**: Custom title bar with `-webkit-app-region: drag`
- **Error Handling**: User-friendly German messages, console logging for debugging
- **Role-based Access**: Backend enforces permissions, frontend adapts UI based on user role

## Development Tips
- **Android Studio**: Open `android-native/` folder (NOT the root) for proper Android development environment
- **Backend First**: Always start backend on network IP using `.\start-backend.ps1` before Android development
- **KSP Build System**: Use Kotlin Symbol Processing (not KAPT) to avoid JDK compatibility issues
- **Room Database**: Entity schemas must exactly match backend SQLAlchemy table names and structures
- **Hilt DI**: All ViewModels and repositories use `@Inject` constructor with proper module configuration
- **Offline-First**: Always return local Room data immediately, sync in background with conflict resolution
- **German Domain**: All entity names, database fields, and UI text use authentic German fire department terminology
- **Network Configuration**: Android devices require backend on network IP (use `ipconfig` to find local IP)

## Platform-Specific Debugging
### Android Native Issues
- **Build Issues**: Ensure using KSP (not KAPT) to avoid JDK compatibility problems
- **Network Access**: Backend must run on network IP (not 127.0.0.1) for Android device access
- **Common Errors**: `Failed to fetch` = backend URL incorrect; Room database crashes = entity schema mismatch
- **Debugging**: Use `adb logcat -d | Select-String -Pattern "checklist|feuerwehr|ERROR"` to check app logs
- **Clean Builds**: `./gradlew clean assembleDebug` or force-stop app with `adb shell am force-stop com.feuerwehr.checklist.debug`

### Android WebView Issues (Legacy)
- **Network Security**: Check `android/app/src/main/res/xml/network_security_config.xml` for HTTPS/HTTP domain configuration
- **Common Errors**: `Failed to fetch` = CORS/backend URL mismatch; `electronAPI not function` = web-api-adapter.js not loaded
- **Cache Issues**: Uninstall/reinstall app (`adb uninstall com.feuerwehr.checklist.debug`) to clear WebView cache
- **URL Configuration**: Debug builds use `http://10.20.1.108:8000`, release builds use `https://checklist.svoboda.click`

### Backend Static File Serving
```python
# FastAPI serves frontend at multiple paths for compatibility:
app.mount("/styles", StaticFiles(directory=os.path.join(frontend_path, "styles")))  # Static assets
@app.get("/") 
async def serve_frontend(): return FileResponse(os.path.join(frontend_path, "index.html"))  # SPA root
@app.get("/config.js")  
async def serve_config(): return FileResponse(os.path.join(frontend_path, "config.js"))   # Runtime config
```

## Known TODOs & Technical Debt
- **Testing**: Frontend testing framework needs implementation (backend has basic test files)
- ✅ **IP Configuration**: ~~All hardcoded IPs need to be replaced~~ - COMPLETED: Now uses configurable environment variables
- ✅ **Environment Variables**: ~~Centralize all URL configurations~~ - COMPLETED: `.env` files with build-time generation
- ✅ **CSP Updates**: ~~Content Security Policy hardcodes backend URLs~~ - COMPLETED: Dynamic CSP generation
- ✅ **Android WebView App**: ~~Mobile deployment~~ - COMPLETED: Native Android wrapper with offline support
- ✅ **Multi-Platform API**: ~~Web/mobile compatibility~~ - COMPLETED: web-api-adapter.js for platform abstraction
