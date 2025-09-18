package com.feuerwehr.checklist.data.remote.api

import com.feuerwehr.checklist.data.remote.dto.*
import retrofit2.http.*

/**
 * Retrofit API interface for Authentication endpoints
 * Mirrors backend FastAPI routes from app/api/routes/auth.py
 */
interface AuthApiService {

    /**
     * Login with username and password
     * POST /auth/login
     */
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequestDto): LoginResponseDto

    /**
     * Get current user info
     * GET /auth/me
     */
    @GET("auth/me")
    suspend fun getCurrentUser(): UserDto

    /**
     * Logout
     * POST /auth/logout
     */
    @POST("auth/logout")
    suspend fun logout(): Map<String, String>

    /**
     * List all users (requires admin permissions)
     * GET /auth/users
     */
    @GET("auth/users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): UserListDto
}