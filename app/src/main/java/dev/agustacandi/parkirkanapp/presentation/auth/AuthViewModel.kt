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
import com.google.firebase.messaging.FirebaseMessaging
import dev.agustacandi.parkirkanapp.fcm.TopicManager

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
            Log.d("AuthViewModel", "Starting login process")
            _loginState.value = LoginState.Loading

            fcmTokenManager.getFCMToken()
                .catch { e ->
                    Log.e("AuthViewModel", "Failed to get FCM token", e)
                    _loginState.value = LoginState.Error("Failed to get FCM token: ${e.message}")
                }
                .flatMapLatest { tokenResult ->
                    if (tokenResult.isFailure) {
                        Log.e("AuthViewModel", "FCM token result failed")
                        _loginState.value = LoginState.Error("Failed to get FCM token")
                        throw tokenResult.exceptionOrNull() ?: Exception("Unknown error")
                    }

                    // Token berhasil didapat, lanjutkan dengan login
                    val fcmToken = tokenResult.getOrThrow()
                    Log.d("AuthViewModel", "Got FCM token, proceeding with login")
                    authRepository.login(email, password, fcmToken)
                }
                .onEach { result ->
                    if (result.isSuccess) {
                        // Ambil data user termasuk role
                        val user = result.getOrNull()
                        val userRole = user?.role ?: "user"
                        Log.d("AuthViewModel", "Login successful with role: $userRole")
                        // Set success state first
                        TopicManager.applyForRole(userRole)
                        _loginState.value = LoginState.Success(userRole)
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("AuthViewModel", "Login failed: ${error?.message}")
                        _loginState.value = LoginState.Error(
                            error?.message ?: "Login failed"
                        )
                    }
                }
                .catch { e ->
                    Log.e("AuthViewModel", "Error in login flow", e)
                    _loginState.value = LoginState.Error(e.message ?: "Unknown error")
                }
                .launchIn(viewModelScope)
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting logout process")
            _logoutState.value = LogoutState.Loading

            // Unsubscribe from all FCM topics
            TopicManager.clearAll();

            authRepository.logout()
                .onEach { result ->
                    if (result.isSuccess) {
                        Log.d("AuthViewModel", "Logout successful, updating state to Success")
                        _logoutState.value = LogoutState.Success
                        // Reset login state after successful logout
                        _loginState.value = LoginState.Login
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("AuthViewModel", "Logout failed: ${error?.message}")
                        _logoutState.value = LogoutState.Error(
                            error?.message ?: "Logout failed"
                        )
                    }
                }
                .catch { e ->
                    Log.e("AuthViewModel", "Error in logout flow", e)
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
                    val userRole = user?.role ?: "user"
                    Log.d("AuthViewModel", "User already logged in with role: $userRole")
                    // Set role in login state (default to "user" if role is null)
                    _loginState.value = LoginState.AlreadyLoggedIn(userRole)
                } else {
                    Log.d("AuthViewModel", "User not logged in, navigating to login")
                    _loginState.value = LoginState.Login
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking login status", e)
                _loginState.value = LoginState.Login
            }
        }
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Login: LoginState()
    data object Loading : LoginState()
    data class Success(val userRole: String) : LoginState()
    data class AlreadyLoggedIn(val userRole: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class LogoutState {
    data object Idle : LogoutState()
    data object Loading : LogoutState()
    data object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}