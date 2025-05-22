package dev.agustacandi.parkirkanapp.data.auth.network

import com.squareup.moshi.Json
import dev.agustacandi.parkirkanapp.data.auth.response.AuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthService {
    @POST("login")
    suspend fun login(
        @Body request: LoginRequest,
    ): Response<AuthResponse>

    @POST("logout")
    suspend fun logout(): Response<Void>

    @POST("user/{userId}/fcm-token")
    suspend fun updateFcmToken(
        @Path("userId") userId: String,
        @Body request: UpdateFcmTokenRequest
    ): Response<Void>

    @POST("change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<Void>

}

data class LoginRequest(
    val email: String,
    val password: String,
    @Json(name = "fcm_token")
    val fcmToken: String
)

data class UpdateFcmTokenRequest(
    @Json(name = "fcm_token")
    val fcmToken: String
)

data class ChangePasswordRequest(
    @Json(name = "old_password")
    val oldPassword: String,
    @Json(name = "new_password")
    val newPassword: String
)
