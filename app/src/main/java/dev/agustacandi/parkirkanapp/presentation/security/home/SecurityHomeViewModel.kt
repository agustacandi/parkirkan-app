package dev.agustacandi.parkirkanapp.presentation.security.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.presentation.home.BroadcastState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityHomeViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository
) : ViewModel() {

    // Broadcast state
    private val _broadcastState = MutableStateFlow<BroadcastState>(BroadcastState.Idle)
    val broadcastState: StateFlow<BroadcastState> = _broadcastState.asStateFlow()

    // Fetch recent broadcasts
    fun fetchRecentBroadcasts() {
        viewModelScope.launch {
            _broadcastState.value = BroadcastState.Loading

            try {
                val broadcasts = broadcastRepository.getRecentBroadcasts(limit = 3)
                _broadcastState.value = BroadcastState.Success(broadcasts)
            } catch (e: Exception) {
                _broadcastState.value = BroadcastState.Error(e.message ?: "Failed to load broadcasts")
            }
        }
    }
}