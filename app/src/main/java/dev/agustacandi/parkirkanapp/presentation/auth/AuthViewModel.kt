package dev.agustacandi.parkirkanapp.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import dev.agustacandi.parkirkanapp.util.FCMTokenManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fcmTokenManager: FCMTokenManager
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()


    @OptIn(ExperimentalCoroutinesApi::class)
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            fcmTokenManager.getFCMToken()
                .catch { e ->
                    _loginState.value = LoginState.Error("Failed to get FCM token: ${e.message}")
                }
                .flatMapLatest { tokenResult ->
                    if (tokenResult.isFailure) {
                        _loginState.value = LoginState.Error("Failed to get FCM token")
                        throw tokenResult.exceptionOrNull() ?: Exception("Unknown error")
                    }

                    // Token berhasil didapat, lanjutkan dengan login
                    val fcmToken = tokenResult.getOrThrow()
                    authRepository.login(email, password, fcmToken)
                }
                .onEach { result ->
                    if (result.isSuccess) {
                        _loginState.value = LoginState.Success
                    } else {
                        _loginState.value = LoginState.Error(
                            result.exceptionOrNull()?.message ?: "Login failed"
                        )
                    }
                }
                .catch { e ->
                    _loginState.value = LoginState.Error(e.message ?: "Unknown error")
                }
                .launchIn(viewModelScope)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading

            authRepository.logout()
                .onEach { result ->
                    if (result.isSuccess) {
                        _logoutState.value = LogoutState.Success
                    } else {
                        _logoutState.value = LogoutState.Error(
                            result.exceptionOrNull()?.message ?: "Logout failed"
                        )
                    }
                }
                .catch { e ->
                    _logoutState.value = LogoutState.Error(e.message ?: "Unknown error")
                }
                .launchIn(viewModelScope)
        }
    }

    // Cek apakah user sudah login sebelumnya
    fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val isLoggedIn = authRepository.isLoggedIn().first()
                Log.d("AuthViewModel", "isLoggedIn: $isLoggedIn")
                if (isLoggedIn) {
                    // Get current user to check role
                    val user = authRepository.getCurrentUser().first()
                    // Set role in login state (default to "user" if role is null)
                    _loginState.value = LoginState.AlreadyLoggedIn(user?.role ?: "user")
                } else {
                    _loginState.value = LoginState.Login
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Login: LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class AlreadyLoggedIn(val userRole: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class LogoutState {
    data object Idle : LogoutState()
    data object Loading : LogoutState()
    data object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}