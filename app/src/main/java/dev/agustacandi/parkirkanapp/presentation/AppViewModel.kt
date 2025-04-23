package dev.agustacandi.parkirkanapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.domain.vehicle.VehicleRepository
import dev.agustacandi.parkirkanapp.util.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    // Selected vehicle state that persists across navigation and app restarts
    private val _selectedVehicle = MutableStateFlow<VehicleRecord?>(null)
    val selectedVehicle: StateFlow<VehicleRecord?> = _selectedVehicle.asStateFlow()

    // Load saved selection on initialization
    init {
        viewModelScope.launch {
            // Get saved vehicle ID
            val savedVehicleId = userPreferences.getSelectedVehicleId()

            // If we have a saved ID, try to load the vehicle
            if (savedVehicleId != null) {
                try {
                    val result = vehicleRepository.getVehicle(savedVehicleId)
                    if (result.isSuccess) {
                        _selectedVehicle.value = result.getOrNull()
                    }
                } catch (e: Exception) {
                    // Handle error - vehicle might no longer exist
                    userPreferences.saveSelectedVehicleId(null)
                }
            }
        }
    }

    // Function to update selected vehicle
    fun setSelectedVehicle(vehicle: VehicleRecord?) {
        _selectedVehicle.value = vehicle

        // Save selection to persistent storage
        userPreferences.saveSelectedVehicleId(vehicle?.id)
    }
}