package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val identifier: String, // username or email
    val password: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Int,
    val name: String,
    val username: String,
    val avatarId: String? = null,
    val profileImageUrl: String? = null,
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val role: String = "USER"
)

@Serializable
data class UserSession(
    val userId: Int,
    val name: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val avatarId: String? = null,
    val isLoggedIn: Boolean = false
)
