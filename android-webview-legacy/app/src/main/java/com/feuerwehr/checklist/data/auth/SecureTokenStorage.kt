package com.feuerwehr.checklist.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64

/**
 * Secure token storage using Android Keystore for JWT token encryption
 */
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val KEYSTORE_ALIAS = "FeuerwehrChecklistTokenKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DATASTORE_NAME = "auth_preferences"
        
        private val ENCRYPTED_TOKEN_KEY = stringPreferencesKey("encrypted_token")
        private val TOKEN_IV_KEY = stringPreferencesKey("token_iv")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)
    }
    
    private val dataStore = context.dataStore
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    
    init {
        generateKeyIfNeeded()
    }
    
    /**
     * Generate encryption key in Android Keystore if it doesn't exist
     */
    private fun generateKeyIfNeeded() {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
                
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    /**
     * Store JWT token securely using Android Keystore encryption
     */
    suspend fun storeToken(token: String, userId: String, username: String) {
        val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedToken = cipher.doFinal(token.toByteArray())
        val iv = cipher.iv
        
        dataStore.edit { preferences ->
            preferences[ENCRYPTED_TOKEN_KEY] = Base64.encodeToString(encryptedToken, Base64.DEFAULT)
            preferences[TOKEN_IV_KEY] = Base64.encodeToString(iv, Base64.DEFAULT)
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
        }
    }
    
    /**
     * Retrieve and decrypt JWT token
     */
    suspend fun getToken(): String? {
        return try {
            val preferences = dataStore.data.first()
            val encryptedTokenString = preferences[ENCRYPTED_TOKEN_KEY] ?: return null
            val ivString = preferences[TOKEN_IV_KEY] ?: return null
            
            val encryptedToken = Base64.decode(encryptedTokenString, Base64.DEFAULT)
            val iv = Base64.decode(ivString, Base64.DEFAULT)
            
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            
            val decryptedToken = cipher.doFinal(encryptedToken)
            String(decryptedToken)
        } catch (e: Exception) {
            null // Token corruption or other error
        }
    }
    
    /**
     * Get stored user ID
     */
    suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }
    
    /**
     * Get stored username
     */
    suspend fun getUsername(): String? {
        return dataStore.data.first()[USERNAME_KEY]
    }
    
    /**
     * Check if user is logged in (has valid token)
     */
    fun isLoggedIn(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[ENCRYPTED_TOKEN_KEY] != null && preferences[TOKEN_IV_KEY] != null
        }
    }
    
    /**
     * Clear all stored authentication data
     */
    suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.remove(ENCRYPTED_TOKEN_KEY)
            preferences.remove(TOKEN_IV_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
        }
    }
    
    /**
     * Get user info flow
     */
    fun getUserInfo(): Flow<AuthUserInfo?> {
        return dataStore.data.map { preferences ->
            val userId = preferences[USER_ID_KEY]
            val username = preferences[USERNAME_KEY]
            
            if (userId != null && username != null) {
                AuthUserInfo(userId, username)
            } else {
                null
            }
        }
    }
}

/**
 * Basic user info stored in preferences
 */
data class AuthUserInfo(
    val userId: String,
    val username: String
)