package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val identifier: String, // username or email
    val password: String
)

@Serializable
data class SendRegisterCodeRequest(
    val email: String,
    val username: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val code: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
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
    val isPremium: Boolean = false,
    val isLoggedIn: Boolean = false,
    val profileImageUrl: String? = null,
    val isGuest: Boolean = false
) {
    /** True when the user is signed in with a real backend account. */
    val isAuthenticated: Boolean
        get() = isLoggedIn && !isGuest && userId > 0

    /** True when the user can access the app (either authenticated or local-only guest). */
    val hasActiveSession: Boolean
        get() = isAuthenticated || (isGuest && userId == GUEST_USER_ID)

    companion object {
        const val GUEST_USER_ID = -1
    }
}
