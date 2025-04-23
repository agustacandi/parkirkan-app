package dev.agustacandi.parkirkanapp.presentation.vehicle.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.NavDestination
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.domain.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class EditVehicleState {
    data object Idle : EditVehicleState()
    data object Loading : EditVehicleState()
    data object Deleting : EditVehicleState()
    data class Success(val vehicle: VehicleRecord) : EditVehicleState()
    data object DeleteSuccess : EditVehicleState()
    data class Error(val message: String) : EditVehicleState()
    data class VehicleLoaded(val vehicle: VehicleRecord) : EditVehicleState()
}

@HiltViewModel
class EditVehicleViewModel @Inject constructor(
    private val repository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _editVehicleState = MutableStateFlow<EditVehicleState>(EditVehicleState.Idle)
    val editVehicleState: StateFlow<EditVehicleState> = _editVehicleState.asStateFlow()

    private val _vehicle = MutableStateFlow<VehicleRecord?>(null)
    val vehicle: StateFlow<VehicleRecord?> = _vehicle.asStateFlow()

    // Get vehicle ID from saved state handle
    private val vehicleId: String = checkNotNull(
        savedStateHandle[NavDestination.EditVehicle.ARG_VEHICLE_ID]
    )

    init {
        loadVehicle()
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            _editVehicleState.value = EditVehicleState.Loading
            repository.getVehicle(vehicleId.toInt())
                .onSuccess { vehicle ->
                    _vehicle.value = vehicle
                    _editVehicleState.value = EditVehicleState.VehicleLoaded(vehicle)
                }
                .onFailure { exception ->
                    _editVehicleState.value = EditVehicleState.Error(
                        exception.message ?: "Failed to load vehicle"
                    )
                }
        }
    }

    fun updateVehicle(name: String, licensePlate: String, imageFile: File?) {
        viewModelScope.launch {
            _editVehicleState.value = EditVehicleState.Loading

            val result = if (imageFile != null) {
                repository.updateVehicle(vehicleId.toInt(), name, licensePlate, imageFile)
            } else {
                repository.updateVehicleWithoutImage(vehicleId.toInt(), name, licensePlate)
            }

            result.onSuccess { vehicle ->
                _editVehicleState.value = EditVehicleState.Success(vehicle)
            }.onFailure { exception ->
                _editVehicleState.value = EditVehicleState.Error(
                    exception.message ?: "Failed to update vehicle"
                )
            }
        }
    }

    fun deleteVehicle() {
        viewModelScope.launch {
            _editVehicleState.value = EditVehicleState.Deleting

            repository.deleteVehicle(vehicleId.toInt())
                .onSuccess {
                    _editVehicleState.value = EditVehicleState.DeleteSuccess
                }
                .onFailure { exception ->
                    _editVehicleState.value = EditVehicleState.Error(
                        exception.message ?: "Failed to delete vehicle"
                    )
                }
        }
    }

    fun resetState() {
        _editVehicleState.value = EditVehicleState.Idle
    }
}