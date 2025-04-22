package dev.agustacandi.parkirkanapp.data.auth

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.agustacandi.parkirkanapp.data.auth.network.AuthService
import dev.agustacandi.parkirkanapp.data.auth.network.LoginRequest
import dev.agustacandi.parkirkanapp.data.auth.network.UpdateFcmTokenRequest
import dev.agustacandi.parkirkanapp.data.auth.response.Data
import dev.agustacandi.parkirkanapp.data.auth.response.ErrorResponse
import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import dev.agustacandi.parkirkanapp.util.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val userPreferences: UserPreferences
) : AuthRepository {
    override fun login(email: String, password: String, fcmToken: String): Flow<Result<Data>> =
        flow {
            try {
                val response = authService.login(LoginRequest(email, password, fcmToken))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!.data
                    userPreferences.saveAuthToken(authResponse.token)
                    userPreferences.saveUser(authResponse)
                    emit(Result.success(authResponse))
                } else {
                    // Parse error response
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            val adapter = moshi.adapter(ErrorResponse::class.java)
                            val errorResponse = adapter.fromJson(errorBody)

                            val errorMessage = errorResponse?.data?.error ?: errorResponse?.message ?: "Unknown error"
                            emit(Result.failure(Exception(errorMessage)))
                        } catch (e: Exception) {
                            emit(Result.failure(Exception("Failed to parse error response: ${response.message()}")))
                        }
                    } else {
                        emit(Result.failure(Exception("Login failed: ${response.message()}")))
                    }
                }

            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }.flowOn(Dispatchers.IO)

    override fun logout(): Flow<Result<Unit>> = flow {
        try {
            val response = authService.logout()
            if (response.isSuccessful) {
                // Clear user data from preferences
                userPreferences.clearUserData()
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Logout failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Update FCM token pada server
     * Mengembalikan Flow<Result<Unit>>
     */
    override fun updateFcmToken(fcmToken: String): Flow<Result<Unit>> = flow {
        try {
            val userId = userPreferences.getUserId()

            if (userId.isEmpty()) {
                emit(Result.failure(Exception("User not logged in")))
                return@flow
            }

            val response = authService.updateFcmToken(
                userId = userId,
                request = UpdateFcmTokenRequest(fcmToken = fcmToken)
            )

            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to update FCM token: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCurrentUser(): Flow<Data?> = flow {
        emit(userPreferences.getUser())
    }.flowOn(Dispatchers.IO)

    override fun isLoggedIn(): Flow<Boolean> = flow {
        emit(userPreferences.getAuthToken() != null)
    }.flowOn(Dispatchers.IO)
}