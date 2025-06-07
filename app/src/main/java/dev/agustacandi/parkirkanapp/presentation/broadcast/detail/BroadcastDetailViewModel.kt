package dev.agustacandi.parkirkanapp.presentation.broadcast.detail

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
import javax.inject.Inject

@HiltViewModel
class BroadcastDetailViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val broadcastId: String = checkNotNull(savedStateHandle[NavDestination.BroadcastDetail.ARG_BROADCAST_ID])

    private val _uiState = MutableStateFlow<BroadcastDetailUiState>(BroadcastDetailUiState.Loading)
    val uiState: StateFlow<BroadcastDetailUiState> = _uiState.asStateFlow()

    init {
        loadBroadcastDetail()
    }

    fun retry() {
        loadBroadcastDetail()
    }

    private fun loadBroadcastDetail() {
        viewModelScope.launch {
            _uiState.value = BroadcastDetailUiState.Loading
            
            try {
                val result = broadcastRepository.getBroadcast(broadcastId.toInt())
                
                result.onSuccess { broadcast ->
                    _uiState.value = BroadcastDetailUiState.Success(broadcast)
                }.onFailure { error ->
                    _uiState.value = BroadcastDetailUiState.Error(
                        error.message ?: "Failed to load broadcast details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = BroadcastDetailUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
}

sealed class BroadcastDetailUiState {
    data object Loading : BroadcastDetailUiState()
    data class Success(val broadcast: Broadcast) : BroadcastDetailUiState()
    data class Error(val message: String) : BroadcastDetailUiState()
} 