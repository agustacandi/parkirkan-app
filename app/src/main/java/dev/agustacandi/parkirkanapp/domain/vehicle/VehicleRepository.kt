package dev.agustacandi.parkirkanapp.domain.vehicle

import androidx.paging.PagingData
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun getVehicles(
        pageSize: Int = 5,
        scope: CoroutineScope
    ): Flow<PagingData<VehicleRecord>>

//    suspend fun addVehicle(vehicle: Vehicle): Vehicle
//    suspend fun updateVehicle(vehicle: Vehicle): Vehicle
//    suspend fun deleteVehicle(vehicleId: Int): Boolean
}