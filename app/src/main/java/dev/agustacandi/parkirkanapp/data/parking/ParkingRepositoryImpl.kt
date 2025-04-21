package dev.agustacandi.parkirkanapp.data.parking

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.agustacandi.parkirkanapp.data.parking.network.ParkingService
import dev.agustacandi.parkirkanapp.data.parking.response.ParkingRecord
import dev.agustacandi.parkirkanapp.domain.parking.repository.ParkingRepository
import dev.agustacandi.parkirkanapp.presentation.parking.ParkingPagingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkingRepositoryImpl @Inject constructor(
    private val parkingService: ParkingService
): ParkingRepository {
    override fun getParkingRecords(pageSize: Int, scope: CoroutineScope): Flow<PagingData<ParkingRecord>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2
            ),
            pagingSourceFactory = { ParkingPagingSource(parkingService, pageSize) }
        ).flow
            .flowOn(Dispatchers.IO)
            .cachedIn(scope)
    }
}