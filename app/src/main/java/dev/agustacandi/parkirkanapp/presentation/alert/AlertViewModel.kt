package dev.agustacandi.parkirkanapp.presentation.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.domain.parking.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AlertAction {
    data object NavigateBack : AlertAction()
    data object ShowSuccess : AlertAction()
    data object ShowError : AlertAction()
}

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository
) : ViewModel() {

    private val _actionFlow = MutableSharedFlow<AlertAction>()
    val actionFlow: SharedFlow<AlertAction> = _actionFlow.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun confirmCheckOut(licensePlate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = parkingRepository.confirmCheckOut(licensePlate)
                if (result) {
                    _actionFlow.emit(AlertAction.ShowSuccess)
                } else {
                    _errorMessage.value = "Failed to confirm check-out"
                    _actionFlow.emit(AlertAction.ShowError)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
                _actionFlow.emit(AlertAction.ShowError)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectCheckOut() {
        viewModelScope.launch {
            _actionFlow.emit(AlertAction.NavigateBack)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}