package dev.agustacandi.parkirkanapp.data.auth

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.agustacandi.parkirkanapp.data.auth.network.AuthService
import dev.agustacandi.parkirkanapp.data.auth.network.ChangePasswordRequest
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
import android.util.Log

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
            Log.d("AuthRepositoryImpl", "Starting repository logout process")
            
            // Get current user ID and FCM token before clearing data
            val userId = userPreferences.getUserId()
            val currentToken = userPreferences.getFcmToken()
            Log.d("AuthRepositoryImpl", "Current user ID: $userId, FCM token: $currentToken")
            
            // Remove FCM token from server if available
            if (!currentToken.isNullOrEmpty() && userId.isNotEmpty()) {
                try {
                    Log.d("AuthRepositoryImpl", "Removing FCM token from server")
                    authService.updateFcmToken(
                        userId = userId,
                        request = UpdateFcmTokenRequest(fcmToken = "")
                    )
                } catch (e: Exception) {
                    Log.e("AuthRepositoryImpl", "Error removing FCM token", e)
                }
            }
            
            // Call logout API
            Log.d("AuthRepositoryImpl", "Calling logout API")
            val response = authService.logout()
            if (response.isSuccessful) {
                Log.d("AuthRepositoryImpl", "Logout API call successful")
                // Clear user data from preferences after successful API call
                Log.d("AuthRepositoryImpl", "Clearing user data from preferences")
                userPreferences.clearUserData()
                emit(Result.success(Unit))
            } else {
                Log.e("AuthRepositoryImpl", "Logout API call failed: ${response.message()}")
                emit(Result.failure(Exception("Logout failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Error in logout flow", e)
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
                Log.e("AuthRepositoryImpl", "Cannot update FCM token: User not logged in")
                emit(Result.failure(Exception("User not logged in")))
                return@flow
            }

            Log.d("AuthRepositoryImpl", "Updating FCM token for user $userId")
            val response = authService.updateFcmToken(
                userId = userId,
                request = UpdateFcmTokenRequest(fcmToken = fcmToken)
            )

            if (response.isSuccessful) {
                Log.d("AuthRepositoryImpl", "FCM token updated successfully")
                userPreferences.saveFcmToken(fcmToken)
                emit(Result.success(Unit))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                        val adapter = moshi.adapter(ErrorResponse::class.java)
                        val errorResponse = adapter.fromJson(errorBody)
                        errorResponse?.data?.error ?: errorResponse?.message ?: "Unknown error"
                    } catch (e: Exception) {
                        "Failed to parse error response: ${response.message()}"
                    }
                } else {
                    "Failed to update FCM token: ${response.message()}"
                }
                Log.e("AuthRepositoryImpl", errorMessage)
                emit(Result.failure(Exception(errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Error updating FCM token", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun changePassword(oldPassword: String, newPassword: String): Flow<Result<Unit>> = flow {
        try {
            val response = authService.changePassword(
                ChangePasswordRequest(
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )
            )

            if (response.isSuccessful) {
                emit(Result.success(Unit))
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
                    emit(Result.failure(Exception("Change password failed: ${response.message()}")))
                }
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