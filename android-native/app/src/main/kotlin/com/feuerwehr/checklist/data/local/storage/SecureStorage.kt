package com.feuerwehr.checklist.data.local.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for user credentials and authentication tokens
 * Uses Android's EncryptedSharedPreferences for security
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val PREFS_NAME = "checklist_secure_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
        private const val KEY_REMEMBER_ME = "remember_me"
    }
    
    /**
     * Save user credentials for auto-login
     */
    fun saveCredentials(username: String, password: String, rememberMe: Boolean = true) {
        encryptedPrefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_REMEMBER_ME, rememberMe)
            .putBoolean(KEY_AUTO_LOGIN, rememberMe)
            .apply()
    }
    
    /**
     * Get stored credentials
     */
    fun getStoredCredentials(): Pair<String?, String?> {
        return if (isAutoLoginEnabled()) {
            val username = encryptedPrefs.getString(KEY_USERNAME, null)
            val password = encryptedPrefs.getString(KEY_PASSWORD, null)
            Pair(username, password)
        } else {
            Pair(null, null)
        }
    }
    
    /**
     * Save authentication token
     */
    fun saveToken(accessToken: String, refreshToken: String? = null, expiresInSeconds: Long? = null) {
        val expiresAt = expiresInSeconds?.let { 
            System.currentTimeMillis() + (it * 1000)
        } ?: (System.currentTimeMillis() + (24 * 60 * 60 * 1000)) // Default 24h
        
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
                putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            }
            .apply()
    }
    
    /**
     * Get stored access token
     */
    fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Get stored refresh token
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Check if token is expired
     */
    fun isTokenExpired(): Boolean {
        val expiresAt = encryptedPrefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        return System.currentTimeMillis() >= expiresAt - (5 * 60 * 1000) // 5 min buffer
    }
    
    /**
     * Check if auto-login is enabled
     */
    fun isAutoLoginEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_AUTO_LOGIN, false)
    }
    
    /**
     * Enable/disable auto-login
     */
    fun setAutoLoginEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_AUTO_LOGIN, enabled)
            .apply()
    }
    
    /**
     * Check if user has valid stored credentials
     */
    fun hasValidCredentials(): Boolean {
        val (username, password) = getStoredCredentials()
        return !username.isNullOrBlank() && !password.isNullOrBlank()
    }
    
    /**
     * Check if user has a valid token
     */
    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrBlank() && !isTokenExpired()
    }
    
    /**
     * Clear all stored data (logout)
     */
    fun clearAll() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .apply()
    }
    
    /**
     * Clear credentials but keep tokens (disable auto-login)
     */
    fun clearCredentials() {
        encryptedPrefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .remove(KEY_REMEMBER_ME)
            .putBoolean(KEY_AUTO_LOGIN, false)
            .apply()
    }
    
    /**
     * Get username for display purposes
     */
    fun getUsername(): String? {
        return encryptedPrefs.getString(KEY_USERNAME, null)
    }

    /**
     * Attempt auto-login using stored credentials
     * @param loginFunction Function to perform the actual login with credentials
     * @return User object if auto-login successful, null otherwise
     */
    suspend fun <T> autoLogin(loginFunction: suspend (username: String, password: String) -> T?): T? {
        return if (isAutoLoginEnabled() && hasValidCredentials()) {
            val (username, password) = getStoredCredentials()
            if (username != null && password != null) {
                try {
                    loginFunction(username, password)
                } catch (e: Exception) {
                    // Auto-login failed, disable it to prevent repeated attempts
                    setAutoLoginEnabled(false)
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }
}