package dev.agustacandi.parkirkanapp.presentation.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.domain.vehicle.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {
    // State for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Flow for vehicle data
    private var _vehiclesFlow: Flow<PagingData<VehicleRecord>>? = null

    /**
     * Retrieves a flow of paged vehicle data.
     * If the data is already loaded, returns cached data.
     * Otherwise, loads new data from the repository.
     */
    fun getVehicles(pageSize: Int = 5): Flow<PagingData<VehicleRecord>> {
        val existingResult = _vehiclesFlow
        if (existingResult != null) {
            return existingResult
        }

        val newResult = vehicleRepository.getVehicles(pageSize, viewModelScope)
        _vehiclesFlow = newResult
        return newResult
    }

    /**
     * Refreshes the vehicle data by clearing the cache and forcing a reload.
     */
    fun refreshData() {
        _vehiclesFlow = null
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