# Migration Guide: WebView to Android-First

This guide helps developers and users transition from the WebView-based implementation to the new Android-first native app.

## Overview of Changes

### Architecture Transformation

**Old Architecture (WebView-Based):**
- Primary: Electron desktop app
- Mobile: Android WebView wrapper loading web content
- Single codebase: HTML/CSS/JS served to both platforms
- Limited offline support via cached HTML files

**New Architecture (Android-First):**
- Primary: Native Android app with Jetpack Compose
- Secondary: Web and desktop platforms as separate implementations
- Shared business logic via Kotlin Multiplatform
- Full offline support with Room database

## For Users

### Benefits of Migration
- **Performance**: Native Android speed vs WebView overhead
- **Offline Functionality**: Works completely offline with local database
- **Better UX**: Material Design 3 with proper Android navigation
- **Reliability**: No web compatibility issues or JavaScript errors

### Data Migration
Your existing data is preserved:
1. **Backend Data**: All vehicle, checklist, and user data remains unchanged
2. **Automatic Sync**: New Android app syncs with existing backend
3. **No Data Loss**: Complete migration of all inspection history

### Installation Process
1. Keep old app during transition period
2. Install new native Android app
3. Login with same credentials
4. Data automatically syncs from backend
5. Uninstall old app when comfortable with new version

## For Developers

### Directory Structure Changes

**Before:**
```
frontend/
├── src/main/            # Electron main process
├── src/renderer/        # Web UI components  
└── web-dist/           # Web deployment

android/
└── WebView wrapper only
```

**After:**
```
android-native/          # Primary platform
├── app/                # Native Android app
└── shared/             # Business logic

web/                    # Secondary platform
desktop/                # Secondary platform
android-webview-legacy/ # Backup of old implementation
```

### Code Migration Patterns

#### 1. UI Components
**Old (HTML/CSS/JS):**
```javascript
class FahrzeugePage {
    render() {
        return `<div class="vehicle-list">...</div>`;
    }
}
```

**New (Jetpack Compose):**
```kotlin
@Composable
fun VehicleListScreen() {
    LazyColumn {
        items(vehicles) { vehicle ->
            VehicleCard(vehicle)
        }
    }
}
```

#### 2. Data Storage
**Old (Web Storage/Electron):**
```javascript
localStorage.setItem('vehicles', JSON.stringify(vehicles));
```

**New (Room Database):**
```kotlin
@Dao
interface VehicleDao {
    @Query("SELECT * FROM fahrzeuge")
    fun getAllVehicles(): Flow<List<VehicleEntity>>
}
```

#### 3. API Calls
**Old (Fetch API):**
```javascript
const response = await fetch('/api/vehicles');
const vehicles = await response.json();
```

**New (Retrofit + Repository):**
```kotlin
interface VehicleApiService {
    @GET("vehicles")
    suspend fun getVehicles(): Response<List<Vehicle>>
}
```

#### 4. State Management
**Old (Global JavaScript objects):**
```javascript
window.dataManager = {
    vehicles: [],
    loadVehicles() { ... }
};
```

**New (ViewModel + StateFlow):**
```kotlin
@HiltViewModel
class VehicleListViewModel @Inject constructor(
    private val repository: VehicleRepository
) : ViewModel() {
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles = _vehicles.asStateFlow()
}
```

### Development Workflow Changes

#### Old Workflow:
1. Edit HTML/CSS/JS in `frontend/src/renderer/`
2. Run `npm run build` to compile TypeScript
3. Test in Electron: `npm start`
4. Build Android WebView wrapper: `./build.bat`

#### New Workflow:
1. **Primary Development**: Android Studio with `android-native/`
2. **Business Logic**: Add to `shared/` for cross-platform use
3. **Testing**: Native Android testing tools
4. **Secondary Platforms**: Update web/desktop to consume new APIs

### API Compatibility

The FastAPI backend remains largely unchanged, ensuring compatibility:

**Existing Endpoints (Still Work):**
- `POST /auth/login`
- `GET /vehicles`
- `POST /checklists`
- All existing CRUD operations

**Enhanced Endpoints (New):**
- `GET /api/mobile/sync` - Bulk sync for offline support
- `POST /api/mobile/batch` - Batch operations for mobile efficiency
- WebSocket endpoints for real-time sync

### Testing Migration

**Old Testing (Limited):**
- Manual testing in Electron
- Basic API tests in Python

**New Testing (Comprehensive):**
```kotlin
// Unit Tests
@Test
fun `login with valid credentials returns success`() {
    // Test ViewModel logic
}

// UI Tests  
@Test
fun vehicleListDisplaysCorrectData() {
    // Test Compose UI
}

// Integration Tests
@Test
fun vehicleSyncWorksOffline() {
    // Test offline sync
}
```

## Migration Checklist

### Phase 1: Setup
- [ ] Clone new Android-first codebase
- [ ] Set up Android Studio development environment
- [ ] Verify backend compatibility
- [ ] Run both old and new apps in parallel

### Phase 2: Feature Parity
- [ ] Implement core vehicle management
- [ ] Add checklist functionality
- [ ] Implement TÜV tracking
- [ ] Add user authentication
- [ ] Implement offline sync

### Phase 3: Enhanced Features
- [ ] Add Android-specific features (notifications, intents)
- [ ] Implement advanced offline capabilities
- [ ] Add data export/import
- [ ] Performance optimizations

### Phase 4: Deployment
- [ ] Beta testing with select users
- [ ] Data migration verification
- [ ] Production deployment
- [ ] Gradual rollout to all users

## Common Issues and Solutions

### 1. Build Issues
**Problem**: Gradle sync fails
**Solution**: Ensure Android SDK and build tools are up to date

### 2. Database Migration
**Problem**: Local data not syncing
**Solution**: Clear app data and force fresh sync from backend

### 3. Authentication
**Problem**: Login fails in new app
**Solution**: Verify backend URL configuration in build.gradle

### 4. Performance
**Problem**: App feels slow compared to expectations
**Solution**: Enable ProGuard for release builds, optimize database queries

## Rollback Plan

If issues occur during migration:

1. **Keep Old App**: Don't uninstall WebView version immediately
2. **Backend Compatibility**: Old and new apps can coexist
3. **Data Safety**: All data remains in backend, safe from app changes
4. **Gradual Migration**: Migrate user groups in phases, not all at once

## Support and Resources

- **Android Development**: [Android-Native README](../android-native/README.md)
- **Architecture Details**: [Android-First Architecture](../ANDROID_FIRST_ARCHITECTURE.md)
- **Backend API**: [Backend Documentation](../backend/README.md)
- **Legacy Reference**: [WebView Implementation](../android-webview-legacy/README.md)

The migration to Android-first represents a significant improvement in performance, reliability, and user experience while maintaining all existing functionality and data integrity.