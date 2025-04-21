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

    @PATCH("user/{userId}/fcm-token")
    suspend fun updateFcmToken(
        @Path("userId") userId: String,
        @Body request: UpdateFcmTokenRequest
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