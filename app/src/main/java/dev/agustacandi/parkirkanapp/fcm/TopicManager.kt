package dev.agustacandi.parkirkanapp.fcm

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object TopicManager {
    private const val ALERT = "alert"
    private const val BROADCAST = "broadcast"
    private val _firebaseMessaging = FirebaseMessaging.getInstance()

    suspend fun applyForRole(role: String) {
        // Default: semua role pakai broadcast
        _firebaseMessaging.subscribeToTopic(BROADCAST).await()

        // Atur 'alert' hanya untuk security
        if (role == "security") {
            _firebaseMessaging.subscribeToTopic(ALERT).await()
        } else {
            _firebaseMessaging.unsubscribeFromTopic(ALERT).await()
        }
    }

    suspend fun clearAll() {
        // Lepas semua yang mungkin pernah dipakai
        _firebaseMessaging.unsubscribeFromTopic(ALERT).await()
        _firebaseMessaging.unsubscribeFromTopic(BROADCAST).await()
    }
}
