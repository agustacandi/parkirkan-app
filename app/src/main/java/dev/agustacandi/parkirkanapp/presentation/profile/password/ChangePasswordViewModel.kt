package dev.agustacandi.parkirkanapp.presentation.profile.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChangePasswordState {
    data object Idle : ChangePasswordState()
    data object Loading : ChangePasswordState()
    data object Success : ChangePasswordState()
    data class Error(val message: String) : ChangePasswordState()
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState.Loading

            authRepository.changePassword(oldPassword, newPassword)
                .onEach { result ->
                    _changePasswordState.value = if (result.isSuccess) {
                        ChangePasswordState.Success
                    } else {
                        ChangePasswordState.Error(
                            result.exceptionOrNull()?.message ?: "Change password failed"
                        )
                    }
                }
                .catch { e ->
                    _changePasswordState.value = ChangePasswordState.Error(e.message ?: "An unexpected error occurred")
                }
                .launchIn(viewModelScope)
        }
    }

    fun resetState() {
        _changePasswordState.value = ChangePasswordState.Idle
    }
}