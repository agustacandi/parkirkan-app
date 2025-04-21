package dev.agustacandi.parkirkanapp.presentation.vehicle.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.domain.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class AddVehicleState {
    data object Idle : AddVehicleState()
    data object Loading : AddVehicleState()
    data class Success(val vehicle: VehicleRecord) : AddVehicleState()
    data class Error(val message: String) : AddVehicleState()
}

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    private val repository: VehicleRepository
) : ViewModel() {

    private val _addVehicleState = MutableStateFlow<AddVehicleState>(AddVehicleState.Idle)
    val addVehicleState: StateFlow<AddVehicleState> = _addVehicleState.asStateFlow()

    fun addVehicle(name: String, licensePlate: String, imageFile: File) {
        viewModelScope.launch {
            _addVehicleState.value = AddVehicleState.Loading

            repository.addVehicle(name, licensePlate, imageFile)
                .onSuccess { vehicle ->
                    _addVehicleState.value = AddVehicleState.Success(vehicle)
                }
                .onFailure { exception ->
                    _addVehicleState.value = AddVehicleState.Error(exception.message ?: "Unknown error occurred")
                }
        }
    }

    fun resetState() {
        _addVehicleState.value = AddVehicleState.Idle
    }
}