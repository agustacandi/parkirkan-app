package dev.agustacandi.parkirkanapp.util

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor() {

    /**
     * Mendapatkan token FCM saat ini sebagai Flow
     */
    fun getFCMToken(): Flow<Result<String>> = flow {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            emit(Result.success(token))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}