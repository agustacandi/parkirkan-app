package dev.agustacandi.parkirkanapp.domain.parking.repository

import androidx.paging.PagingData
import dev.agustacandi.parkirkanapp.data.parking.response.ParkingRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface ParkingRepository {
    fun getParkingRecords(pageSize: Int = 5, scope: CoroutineScope): Flow<PagingData<ParkingRecord>>

    suspend fun isVehicleCheckedIn(licensePlate: String): Boolean

    suspend fun confirmCheckOut(licensePlate: String): Boolean

    suspend fun reportCheckOut(licensePlate: String): Boolean
}