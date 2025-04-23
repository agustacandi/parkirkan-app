package dev.agustacandi.parkirkanapp.domain.vehicle

import androidx.paging.PagingData
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VehicleRepository {
    fun getVehicles(
        pageSize: Int = 5,
        scope: CoroutineScope
    ): Flow<PagingData<VehicleRecord>>

    suspend fun addVehicle(name: String, licensePlate: String, imageFile: File): Result<VehicleRecord>

    suspend fun getVehicle(vehicleId: Int): Result<VehicleRecord>

    suspend fun updateVehicle(vehicleId: Int, name: String, licensePlate: String, imageFile: File): Result<VehicleRecord>

    suspend fun updateVehicleWithoutImage(vehicleId: Int, name: String, licensePlate: String): Result<VehicleRecord>

    suspend fun deleteVehicle(vehicleId: Int): Result<Unit>

}