package dev.agustacandi.parkirkanapp.data.vehicle

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.agustacandi.parkirkanapp.data.vehicle.network.VehicleService
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.domain.vehicle.VehicleRepository
import dev.agustacandi.parkirkanapp.presentation.vehicle.VehiclePagingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepositoryImpl @Inject constructor(private val vehicleService: VehicleService): VehicleRepository {
    override fun getVehicles(
        pageSize: Int,
        scope: CoroutineScope
    ): Flow<PagingData<VehicleRecord>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2
            ),
            pagingSourceFactory = { VehiclePagingSource(vehicleService = vehicleService, pageSize = pageSize) }
        ).flow
            .flowOn(Dispatchers.IO)
            .cachedIn(scope)
    }
}