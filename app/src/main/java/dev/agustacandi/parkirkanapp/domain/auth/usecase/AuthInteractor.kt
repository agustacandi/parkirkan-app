package dev.agustacandi.parkirkanapp.domain.auth.usecase

import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import javax.inject.Inject

class AuthInteractor @Inject constructor(private val authRepository: AuthRepository)  : AuthUseCase {
    override fun login(email: String, password: String, fcmToken: String): Boolean {
        // Implement your login logic here
        return true // Placeholder return value
    }
}