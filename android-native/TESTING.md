# Testing Framework Documentation

## Overview

This document outlines the comprehensive testing framework for the Feuerwehr Checklist Android application.

## Testing Structure

### 1. **Unit Tests** (`src/test/kotlin/`)
- **Purpose**: Test individual components in isolation
- **Framework**: JUnit 4 with Mockito/MockK for mocking
- **Scope**: ViewModels, Repositories, Use Cases, Utilities

### 2. **Integration Tests** (`src/androidTest/kotlin/`)
- **Purpose**: Test component interactions with Android framework
- **Framework**: AndroidJUnit4 with Espresso for UI testing
- **Scope**: Database operations, UI flows, API integration

### 3. **UI Tests** (`src/androidTest/kotlin/`)
- **Purpose**: End-to-end user interface testing
- **Framework**: Compose Test API + Espresso
- **Scope**: Screen navigation, user interactions, visual validation

## Test Dependencies

### Core Testing Libraries
```gradle
// Unit Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("org.mockito:mockito-core:5.7.0")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("com.google.truth:truth:1.1.4")

// Hilt Testing
testImplementation("com.google.dagger:hilt-android-testing:2.48.1")
kspTest("com.google.dagger:hilt-android-compiler:2.48.1")

// Database Testing
testImplementation("androidx.room:room-testing:2.6.0")

// Network Testing
testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
testImplementation("org.robolectric:robolectric:4.11.1")

// Android Instrumentation Tests
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
```

## Testing Patterns

### 1. **ViewModel Testing Pattern**
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    
    @Mock private lateinit var repository: Repository
    private lateinit var viewModel: ViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ViewModel(repository)
    }
    
    @Test
    fun `test case description`() = runTest {
        // Arrange
        whenever(repository.method()).thenReturn(expected)
        
        // Act
        viewModel.action()
        testScheduler.advanceUntilIdle()
        
        // Assert
        assertEquals(expected, viewModel.state.value.property)
    }
}
```

### 2. **Repository Testing Pattern**
```kotlin
class RepositoryTest {
    @Mock private lateinit var dao: Dao
    @Mock private lateinit var api: ApiService
    private lateinit var repository: Repository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = RepositoryImpl(dao, api)
    }
    
    @Test
    fun `test repository operation`() = runTest {
        // Test data layer interactions
        val testData = createTestData()
        whenever(api.getData()).thenReturn(testData)
        
        val result = repository.fetchData()
        
        assertTrue(result.isSuccess)
        verify(dao).insertData(any())
    }
}
```

### 3. **Database Testing Pattern**
```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var database: Database
    private lateinit var dao: Dao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java
        ).allowMainThreadQueries().build()
        dao = database.dao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun testDatabaseOperation() = runTest {
        // Test Room database operations
        val testEntity = createTestEntity()
        dao.insert(testEntity)
        
        val result = dao.getById(testEntity.id)
        assertEquals(testEntity.property, result?.property)
    }
}
```

## Running Tests

### Command Line
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "*.ViewModelTest"

# Run with coverage
./gradlew testDebugUnitTestCoverage

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### Android Studio
- **Unit Tests**: Right-click on test file/method → "Run Tests"
- **Coverage**: Right-click → "Run with Coverage"
- **All Tests**: Run → Edit Configurations → Add JUnit configuration

## Test Categories

### 1. **Critical Path Tests** (Priority 1)
- Authentication flow
- Data synchronization
- Database FK constraints
- Vehicle CRUD operations

### 2. **Business Logic Tests** (Priority 2)
- TÜV deadline calculations
- User role permissions
- Checklist execution flows
- German domain model validation

### 3. **UI/Integration Tests** (Priority 3)
- Screen navigation
- Form validation
- Network error handling
- Offline mode behavior

## Testing Best Practices

### Do's ✅
- Test one thing at a time
- Use descriptive test names with backticks
- Follow AAA pattern (Arrange, Act, Assert)
- Mock external dependencies
- Use proper test dispatchers for coroutines
- Test both success and failure scenarios
- Verify interactions with `verify()`

### Don'ts ❌
- Don't test framework code (Room, Retrofit, etc.)
- Don't use real network calls in unit tests
- Don't test implementation details
- Don't write flaky tests with timing issues
- Don't ignore test failures
- Don't write tests that depend on each other

## Continuous Integration

### Test Automation
- All tests run on every pull request
- Test coverage reports generated automatically
- Failed tests block merges
- Performance regression detection

### Quality Gates
- Minimum 80% test coverage for new code
- All critical path tests must pass
- No flaky tests allowed in main branch
- Integration tests run on multiple device configurations

## Troubleshooting

### Common Issues
1. **"Cannot inline bytecode"** → Check JVM target compatibility
2. **Mock not working** → Verify `@Mock` annotation and `MockitoAnnotations.openMocks()`
3. **Coroutine tests failing** → Use `testScheduler.advanceUntilIdle()`
4. **Database tests crashing** → Ensure proper entity relationships

### Performance Tips
- Use `runTest` for coroutine testing
- Prefer MockK over Mockito for Kotlin
- Use in-memory database for speed
- Mock expensive operations
- Run tests in parallel when possible

## Test Coverage Goals

- **Unit Tests**: 90% coverage
- **Integration Tests**: 70% coverage  
- **Critical Paths**: 100% coverage
- **Overall Project**: 85% coverage

This testing framework ensures high-quality, maintainable code while providing confidence in the application's reliability and correctness.