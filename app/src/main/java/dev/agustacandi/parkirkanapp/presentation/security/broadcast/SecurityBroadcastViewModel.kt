package dev.agustacandi.parkirkanapp.presentation.security.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SecurityBroadcastViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository
) : ViewModel() {
    // State for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Flow for broadcast data
    private var _broadcastsFlow: Flow<PagingData<Broadcast>>? = null

    /**
     * Retrieves a flow of paged broadcast data.
     * If the data is already loaded, returns cached data.
     * Otherwise, loads new data from the repository.
     */
    fun getBroadcasts(pageSize: Int = 5): Flow<PagingData<Broadcast>> {
        val existingResult = _broadcastsFlow
        if (existingResult != null) {
            return existingResult
        }

        val newResult = broadcastRepository.getBroadcasts(pageSize, viewModelScope)
        _broadcastsFlow = newResult
        return newResult
    }

    /**
     * Refreshes the broadcast data by clearing the cache and forcing a reload.
     */
    fun refreshData() {
        _broadcastsFlow = null
        // Reset error if present
        _errorMessage.value = null
    }

    /**
     * Handles errors by updating the error message state.
     * Pass null to clear the error.
     */
    fun handleError(error: String?) {
        _errorMessage.value = error
    }

    /**
     * Sets the loading state.
     */
    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}