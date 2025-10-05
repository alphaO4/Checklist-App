package com.feuerwehr.checklist.presentation.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feuerwehr.checklist.presentation.theme.ChecklistTheme
import com.feuerwehr.checklist.presentation.screen.login.LoginScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for LoginScreen
 * Tests user interactions and UI behavior
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loginScreen_displaysAllElements() {
        // Given
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "",
                    password = "",
                    isLoading = false,
                    errorMessage = null,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // Then - Verify all UI elements are displayed
        composeTestRule.onNodeWithText("Feuerwehr Checklist").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anmelden").assertIsDisplayed()
        composeTestRule.onNodeWithText("Benutzername").assertIsDisplayed()
        composeTestRule.onNodeWithText("Passwort").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anmelden", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun loginScreen_allowsTextInput() {
        // Given
        var username = ""
        var password = ""
        
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = username,
                    password = password,
                    isLoading = false,
                    errorMessage = null,
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // When - Enter username and password
        composeTestRule.onNodeWithText("Benutzername")
            .performTextInput("testuser")
        composeTestRule.onNodeWithText("Passwort")
            .performTextInput("testpassword")

        // Then - Verify input fields contain text
        composeTestRule.onNodeWithText("testuser").assertIsDisplayed()
        composeTestRule.onNodeWithText("testpassword").assertIsDisplayed()
    }

    @Test
    fun loginScreen_loginButtonClickable() {
        // Given
        var loginClicked = false
        
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "testuser",
                    password = "testpassword",
                    isLoading = false,
                    errorMessage = null,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = { loginClicked = true },
                    onErrorDismiss = {}
                )
            }
        }

        // When - Click login button
        composeTestRule.onNodeWithText("Anmelden", useUnmergedTree = true)
            .performClick()

        // Then - Verify login callback was called
        assert(loginClicked) { "Login click callback should have been called" }
    }

    @Test
    fun loginScreen_showsLoadingState() {
        // Given
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "testuser",
                    password = "testpassword",
                    isLoading = true,
                    errorMessage = null,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // Then - Verify loading indicator is shown
        composeTestRule.onNode(hasText("Anmelden") and hasClickAction())
            .assertIsNotEnabled()
    }

    @Test
    fun loginScreen_showsErrorMessage() {
        // Given
        val errorMessage = "Anmeldung fehlgeschlagen - Überprüfen Sie Ihre Zugangsdaten"
        
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "testuser",
                    password = "wrongpassword",
                    isLoading = false,
                    errorMessage = errorMessage,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // Then - Verify error message is displayed
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun loginScreen_dismissesErrorOnClick() {
        // Given
        var errorDismissed = false
        val errorMessage = "Anmeldung fehlgeschlagen"
        
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "testuser",
                    password = "wrongpassword",
                    isLoading = false,
                    errorMessage = errorMessage,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = { errorDismissed = true }
                )
            }
        }

        // When - Click to dismiss error
        composeTestRule.onNodeWithText(errorMessage).performClick()

        // Then - Verify error dismiss callback was called
        assert(errorDismissed) { "Error dismiss callback should have been called" }
    }

    @Test
    fun loginScreen_handlesGermanCharacters() {
        // Given
        var username = ""
        
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = username,
                    password = "",
                    isLoading = false,
                    errorMessage = null,
                    onUsernameChange = { username = it },
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // When - Enter username with German characters
        val germanUsername = "müller.björn"
        composeTestRule.onNodeWithText("Benutzername")
            .performTextInput(germanUsername)

        // Then - Verify German characters are displayed correctly
        composeTestRule.onNodeWithText(germanUsername).assertIsDisplayed()
    }

    @Test
    fun loginScreen_passwordFieldIsSecure() {
        // Given
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "",
                    password = "",
                    isLoading = false,
                    errorMessage = null,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // When - Enter password
        composeTestRule.onNodeWithText("Passwort")
            .performTextInput("secretpassword")

        // Then - Verify password is not visible as plain text
        // The password field should be using PasswordVisualTransformation
        composeTestRule.onNodeWithText("secretpassword").assertDoesNotExist()
    }

    @Test
    fun loginScreen_displaysFireDepartmentBranding() {
        // Given
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "",
                    password = "",
                    isLoading = false,
                    errorMessage = null,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // Then - Verify fire department specific elements
        composeTestRule.onNodeWithText("Feuerwehr Checklist").assertIsDisplayed()
        // Could also check for fire department logo/icon if present
    }

    @Test
    fun loginScreen_handlesLongErrorMessages() {
        // Given
        val longErrorMessage = "Ein sehr langer Fehlermeldung die möglicherweise über mehrere Zeilen " +
                "geht und trotzdem korrekt angezeigt werden sollte ohne das Layout zu brechen oder " +
                "wichtige Informationen zu verstecken."
        
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "",
                    password = "",
                    isLoading = false,
                    errorMessage = longErrorMessage,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // Then - Verify long error message is displayed properly
        composeTestRule.onNodeWithText(longErrorMessage).assertIsDisplayed()
    }

    @Test
    fun loginScreen_keyboardNavigation() {
        // Given
        composeTestRule.setContent {
            ChecklistTheme {
                LoginScreen(
                    username = "",
                    password = "",
                    isLoading = false,
                    errorMessage = null,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onErrorDismiss = {}
                )
            }
        }

        // When - Navigate between fields using keyboard
        composeTestRule.onNodeWithText("Benutzername")
            .performTextInput("testuser")
            .performImeAction()

        // Then - Focus should move to password field
        composeTestRule.onNodeWithText("Passwort")
            .assertIsFocused()
    }

    @Test
    fun loginScreen_roleBasedMessaging() {
        // Test different error messages for different user scenarios
        val errorMessages = listOf(
            "Keine Berechtigung für diese Aktion",
            "Benutzername oder Passwort ungültig", 
            "Netzwerkverbindung fehlgeschlagen",
            "Server nicht erreichbar"
        )

        errorMessages.forEach { errorMessage ->
            composeTestRule.setContent {
                ChecklistTheme {
                    LoginScreen(
                        username = "testuser",
                        password = "testpassword",
                        isLoading = false,
                        errorMessage = errorMessage,
                        onUsernameChange = {},
                        onPasswordChange = {},
                        onLoginClick = {},
                        onErrorDismiss = {}
                    )
                }
            }

            // Verify error message is displayed
            composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        }
    }
}