package dev.agustacandi.parkirkanapp.data.broadcast

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.agustacandi.parkirkanapp.data.broadcast.network.BroadcastService
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.presentation.broadcast.BroadcastPagingSource
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BroadcastRepositoryImpl @Inject constructor(
    private val broadcastService: BroadcastService
) : BroadcastRepository {

    override fun getBroadcasts(pageSize: Int, scope: CoroutineScope): Flow<PagingData<Broadcast>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2
            ),
            pagingSourceFactory = {
                BroadcastPagingSource(
                    broadcastService = broadcastService,
                    pageSize = pageSize
                )
            }
        ).flow
            .flowOn(Dispatchers.IO)
            .cachedIn(scope)
    }

    override suspend fun getRecentBroadcasts(limit: Int): List<Broadcast> = withContext(Dispatchers.IO) {
        try {
            val response = broadcastService.getBroadcasts(page = 1, perPage = limit)

            if (!response.success) {
                throw Exception(response.message)
            }

            // Map API response to domain model
            return@withContext response.data.data.map { broadcastRecord ->
                Broadcast(
                    id = broadcastRecord.id,
                    title = broadcastRecord.title,
                    description = broadcastRecord.description,
                    image = broadcastRecord.image,
                    createdAt = broadcastRecord.createdAt,
                    updatedAt = broadcastRecord.updatedAt
                )
            }
        } catch (e: Exception) {
            throw e
        }
    }
}