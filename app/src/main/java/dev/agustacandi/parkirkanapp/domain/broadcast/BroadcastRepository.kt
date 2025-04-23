package dev.agustacandi.parkirkanapp.domain.broadcast

import androidx.paging.PagingData
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface BroadcastRepository {
    fun getBroadcasts(pageSize: Int = 10, scope: CoroutineScope): Flow<PagingData<Broadcast>>
    suspend fun getRecentBroadcasts(limit: Int = 3): List<Broadcast>
}