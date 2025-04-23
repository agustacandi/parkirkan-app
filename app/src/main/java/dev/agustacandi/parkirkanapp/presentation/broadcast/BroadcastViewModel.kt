package dev.agustacandi.parkirkanapp.presentation.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository
) : ViewModel() {

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Broadcasts paging data
    private var _broadcastsFlow: Flow<PagingData<Broadcast>>? = null

    // Get paginated broadcasts
    fun getBroadcasts(pageSize: Int = 10): Flow<PagingData<Broadcast>> {
        val existingResult = _broadcastsFlow
        if (existingResult != null) {
            return existingResult
        }

        val newResult = broadcastRepository.getBroadcasts(pageSize, viewModelScope)
            .cachedIn(viewModelScope)

        _broadcastsFlow = newResult
        return newResult
    }

    // Refresh data
    fun refreshData() {
        _broadcastsFlow = null
        _errorMessage.value = null
    }

    // Handle errors
    fun handleError(error: String?) {
        _errorMessage.value = error
    }

    // Set loading state
    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}