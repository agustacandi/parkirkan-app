package dev.agustacandi.parkirkanapp.presentation.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.data.parking.response.ParkingRecord
import dev.agustacandi.parkirkanapp.domain.parking.repository.ParkingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ParkingViewModel @Inject constructor(
    private val repository: ParkingRepository
) : ViewModel() {
    // State untuk status loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State untuk error
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Flow untuk data parking
    private var _parkingRecordsFlow: Flow<PagingData<ParkingRecord>>? = null

    fun getParkingRecords(pageSize: Int = 5): Flow<PagingData<ParkingRecord>> {
        val existingResult = _parkingRecordsFlow
        if (existingResult != null) {
            return existingResult
        }

        val newResult = repository.getParkingRecords(pageSize, viewModelScope)
        _parkingRecordsFlow = newResult
        return newResult
    }

    // Function untuk refresh data jika diperlukan
    fun refreshData() {
        _parkingRecordsFlow = null
        // Reset error jika ada
        _errorMessage.value = null
    }

    // Function untuk menangani error
    fun handleError(error: String) {
        _errorMessage.value = error
    }

    // Function untuk memulai loading state
    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}