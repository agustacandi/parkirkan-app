package dev.agustacandi.parkirkanapp.domain.auth.repository

import dev.agustacandi.parkirkanapp.data.auth.response.Data
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, password: String, fcmToken: String): Flow<Result<Data>>

    fun logout(): Flow<Result<Unit>>

    fun updateFcmToken(fcmToken: String): Flow<Result<Unit>>

    fun getCurrentUser(): Flow<Data?>

    fun isLoggedIn(): Flow<Boolean>
}