package com.feuerwehr.checklist.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Authentication Data Transfer Objects
 * Mirrors backend Pydantic schemas from app/schemas/auth.py
 */

data class LoginRequestDto(
    val username: String,
    val password: String
)

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    val rolle: String,
    @SerializedName("gruppe_id") val gruppeId: Int?,
    @SerializedName("created_at") val createdAt: String
)

data class TokenDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer"
)

data class UserListDto(
    val items: List<UserDto>,
    val total: Int,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total_pages") val totalPages: Int
)