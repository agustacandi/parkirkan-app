package dev.agustacandi.parkirkanapp.domain.broadcast

import androidx.paging.PagingData
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.io.File

interface BroadcastRepository {
    fun getBroadcastsAll(pageSize: Int = 10, scope: CoroutineScope): Flow<PagingData<Broadcast>>
    suspend fun getRecentBroadcasts(limit: Int = 3): List<Broadcast>
    fun getBroadcasts(pageSize: Int = 5, scope: CoroutineScope): Flow<PagingData<Broadcast>>
    suspend fun getBroadcast(broadcastId: Int): Result<Broadcast>
    suspend fun addBroadcast(title: String, description: String, imageFile: File?): Result<Int>
    suspend fun updateBroadcast(broadcastId: Int, title: String, description: String, imageFile: File): Result<Broadcast>
    suspend fun updateBroadcastWithoutImage(broadcastId: Int, title: String, description: String): Result<Broadcast>
    suspend fun deleteBroadcast(broadcastId: Int): Result<Unit>
}