package dev.agustacandi.parkirkanapp.domain.auth.usecase

interface AuthUseCase {
    fun login(email: String, password: String, fcmToken: String): Boolean
}