package dev.agustacandi.parkirkanapp.presentation.security.broadcast.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.NavDestination
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class EditBroadcastState {
    data object Idle : EditBroadcastState()
    data object Loading : EditBroadcastState()
    data object Deleting : EditBroadcastState()
    data class Success(val broadcast: Broadcast) : EditBroadcastState()
    data object DeleteSuccess : EditBroadcastState()
    data class Error(val message: String) : EditBroadcastState()
    data class BroadcastLoaded(val broadcast: Broadcast) : EditBroadcastState()
}

@HiltViewModel
class EditBroadcastViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _editBroadcastState = MutableStateFlow<EditBroadcastState>(EditBroadcastState.Idle)
    val editBroadcastState: StateFlow<EditBroadcastState> = _editBroadcastState.asStateFlow()

    private val _broadcast = MutableStateFlow<Broadcast?>(null)
    val broadcast: StateFlow<Broadcast?> = _broadcast.asStateFlow()

    // Get broadcast ID from saved state handle
    private val broadcastId: String = checkNotNull(
        savedStateHandle[NavDestination.EditBroadcast.ARG_BROADCAST_ID]
    )

    init {
        loadBroadcast()
    }

    private fun loadBroadcast() {
        viewModelScope.launch {
            _editBroadcastState.value = EditBroadcastState.Loading
            broadcastRepository.getBroadcast(broadcastId.toInt())
                .onSuccess { broadcast ->
                    _broadcast.value = broadcast
                    _editBroadcastState.value = EditBroadcastState.BroadcastLoaded(broadcast)
                }
                .onFailure { exception ->
                    _editBroadcastState.value = EditBroadcastState.Error(
                        exception.message ?: "Failed to load broadcast"
                    )
                }
        }
    }

    fun updateBroadcast(title: String, description: String, imageFile: File) {
        viewModelScope.launch {
            _editBroadcastState.value = EditBroadcastState.Loading

            broadcastRepository.updateBroadcast(broadcastId.toInt(), title, description, imageFile)
                .onSuccess { broadcast ->
                    _editBroadcastState.value = EditBroadcastState.Success(broadcast)
                }
                .onFailure { exception ->
                    _editBroadcastState.value = EditBroadcastState.Error(
                        exception.message ?: "Failed to update broadcast"
                    )
                }
        }
    }

    fun updateBroadcastWithoutImage(title: String, description: String) {
        viewModelScope.launch {
            _editBroadcastState.value = EditBroadcastState.Loading

            broadcastRepository.updateBroadcastWithoutImage(broadcastId.toInt(), title, description)
                .onSuccess { broadcast ->
                    _editBroadcastState.value = EditBroadcastState.Success(broadcast)
                }
                .onFailure { exception ->
                    _editBroadcastState.value = EditBroadcastState.Error(
                        exception.message ?: "Failed to update broadcast"
                    )
                }
        }
    }

    fun deleteBroadcast() {
        viewModelScope.launch {
            _editBroadcastState.value = EditBroadcastState.Deleting

            broadcastRepository.deleteBroadcast(broadcastId.toInt())
                .onSuccess {
                    _editBroadcastState.value = EditBroadcastState.DeleteSuccess
                }
                .onFailure { exception ->
                    _editBroadcastState.value = EditBroadcastState.Error(
                        exception.message ?: "Failed to delete broadcast"
                    )
                }
        }
    }

    fun resetState() {
        _editBroadcastState.value = EditBroadcastState.Idle
    }
}