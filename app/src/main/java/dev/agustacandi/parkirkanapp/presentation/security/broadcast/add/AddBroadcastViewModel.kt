package dev.agustacandi.parkirkanapp.presentation.security.broadcast.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class AddBroadcastState {
    data object Idle : AddBroadcastState()
    data object Loading : AddBroadcastState()
    data class Success(val broadcastId: Int) : AddBroadcastState()
    data class Error(val message: String) : AddBroadcastState()
}

@HiltViewModel
class AddBroadcastViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository
) : ViewModel() {

    private val _addBroadcastState = MutableStateFlow<AddBroadcastState>(AddBroadcastState.Idle)
    val addBroadcastState: StateFlow<AddBroadcastState> = _addBroadcastState.asStateFlow()

    fun addBroadcast(title: String, description: String, imageFile: File?) {
        viewModelScope.launch {
            _addBroadcastState.value = AddBroadcastState.Loading

            broadcastRepository.addBroadcast(title, description, imageFile)
                .onSuccess { broadcastId ->
                    _addBroadcastState.value = AddBroadcastState.Success(broadcastId)
                }
                .onFailure { exception ->
                    _addBroadcastState.value = AddBroadcastState.Error(
                        exception.message ?: "Gagal menambahkan broadcast"
                    )
                }
        }
    }

    fun resetState() {
        _addBroadcastState.value = AddBroadcastState.Idle
    }
}