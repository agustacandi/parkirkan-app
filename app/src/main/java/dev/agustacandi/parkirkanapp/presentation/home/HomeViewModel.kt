package dev.agustacandi.parkirkanapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.domain.parking.repository.ParkingRepository
import dev.agustacandi.parkirkanapp.domain.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data models for broadcasts
data class Broadcast(
    val id: Int,
    val title: String,
    val description: String,
    val image: String?,
    val createdAt: String,
    val updatedAt: String
)

// Vehicle list state
sealed class VehicleListState {
    data object Idle : VehicleListState()
    data object Loading : VehicleListState()
    data class Success(val vehicles: List<VehicleRecord>) : VehicleListState()
    data class Error(val message: String) : VehicleListState()
}

// Checked-in status state
sealed class CheckedInState {
    data object Idle : CheckedInState()
    data object Loading : CheckedInState()
    data object CheckedIn : CheckedInState()
    data object NotCheckedIn : CheckedInState()
    data class Error(val message: String) : CheckedInState()
}

// Broadcast state
sealed class BroadcastState {
    data object Idle : BroadcastState()
    data object Loading : BroadcastState()
    data class Success(val broadcasts: List<Broadcast>) : BroadcastState()
    data class Error(val message: String) : BroadcastState()
}

// Confirm check-out state
sealed class ConfirmCheckOutState {
    data object Idle : ConfirmCheckOutState()
    data object Loading : ConfirmCheckOutState()
    data object Success : ConfirmCheckOutState()
    data class Error(val message: String) : ConfirmCheckOutState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val parkingRepository: ParkingRepository,
    private val broadcastRepository: BroadcastRepository
) : ViewModel() {

    // Vehicle list state
    private val _vehicleListState = MutableStateFlow<VehicleListState>(VehicleListState.Idle)
    val vehicleListState: StateFlow<VehicleListState> = _vehicleListState.asStateFlow()

    // Checked-in status state
    private val _isCheckedInState = MutableStateFlow<CheckedInState>(CheckedInState.Idle)
    val isCheckedInState: StateFlow<CheckedInState> = _isCheckedInState.asStateFlow()

    // Broadcast state
    private val _broadcastState = MutableStateFlow<BroadcastState>(BroadcastState.Idle)
    val broadcastState: StateFlow<BroadcastState> = _broadcastState.asStateFlow()

    // Confirm check-out state
    private val _confirmCheckOutState = MutableStateFlow<ConfirmCheckOutState>(ConfirmCheckOutState.Idle)
    val confirmCheckOutState: StateFlow<ConfirmCheckOutState> = _confirmCheckOutState.asStateFlow()

    // Fetch list of vehicles
    fun fetchVehicles() {
        viewModelScope.launch {
            _vehicleListState.value = VehicleListState.Loading

            try {
                // This is a simplified implementation assuming we have a method to get all vehicles at once
                // In a real-world scenario, you might need to adapt this to work with your actual repository implementation
                val vehicles = vehicleRepository.getVehiclesList()
                _vehicleListState.value = VehicleListState.Success(vehicles)
            } catch (e: Exception) {
                _vehicleListState.value = VehicleListState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    // Check if a vehicle is checked in
    fun checkVehicleCheckedInStatus(licensePlate: String) {
        viewModelScope.launch {
            _isCheckedInState.value = CheckedInState.Loading

            try {
                val isCheckedIn = parkingRepository.isVehicleCheckedIn(licensePlate)
                _isCheckedInState.value = if (isCheckedIn) {
                    CheckedInState.CheckedIn
                } else {
                    CheckedInState.NotCheckedIn
                }
            } catch (e: Exception) {
                _isCheckedInState.value = CheckedInState.Error(e.message ?: "Failed to check vehicle status")
            }
        }
    }

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

    // Confirm check-out for a vehicle
    fun confirmCheckOut(licensePlate: String) {
        viewModelScope.launch {
            _confirmCheckOutState.value = ConfirmCheckOutState.Loading

            try {
                val result = parkingRepository.confirmCheckOut(licensePlate)
                if (result) {
                    _confirmCheckOutState.value = ConfirmCheckOutState.Success
                    // After successful confirmation, update the checked-in status
                    _isCheckedInState.value = CheckedInState.NotCheckedIn
                } else {
                    _confirmCheckOutState.value = ConfirmCheckOutState.Error("Failed to confirm check-out")
                }
            } catch (e: Exception) {
                _confirmCheckOutState.value = ConfirmCheckOutState.Error(e.message ?: "An error occurred")
            }
        }
    }
}