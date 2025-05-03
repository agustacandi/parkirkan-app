package dev.agustacandi.parkirkanapp.data.broadcast

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.agustacandi.parkirkanapp.data.broadcast.network.BroadcastService
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.presentation.broadcast.BroadcastAllPagingSource
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import dev.agustacandi.parkirkanapp.presentation.security.broadcast.SecurityBroadcastPagingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BroadcastRepositoryImpl @Inject constructor(
    private val broadcastService: BroadcastService
) : BroadcastRepository {

    override fun getBroadcastsAll(pageSize: Int, scope: CoroutineScope): Flow<PagingData<Broadcast>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2
            ),
            pagingSourceFactory = {
                BroadcastAllPagingSource(
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
            val response = broadcastService.getBroadcastsAll(page = 1, perPage = limit)

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

    override fun getBroadcasts(pageSize: Int, scope: CoroutineScope): Flow<PagingData<Broadcast>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2
            ),
            pagingSourceFactory = {
                SecurityBroadcastPagingSource(
                    broadcastService = broadcastService,
                    pageSize = pageSize
                )
            }
        ).flow
            .flowOn(Dispatchers.IO)
            .cachedIn(scope)
    }

    override suspend fun getBroadcast(broadcastId: Int): Result<Broadcast> = withContext(Dispatchers.IO) {
        try {
            val response = broadcastService.getBroadcast(broadcastId)

            if (!response.success) {
                return@withContext Result.failure(Exception(response.message))
            }

            val broadcastRecord = response.data

            // Map API response to domain model
            val broadcast = Broadcast(
                id = broadcastRecord.id,
                title = broadcastRecord.title,
                description = broadcastRecord.description,
                image = broadcastRecord.image,
                createdAt = broadcastRecord.createdAt,
                updatedAt = broadcastRecord.updatedAt
            )

            return@withContext Result.success(broadcast)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun addBroadcast(title: String, description: String, imageFile: File?): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                // Prepare request parts
                val titleRequestBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionRequestBody = description.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = if (imageFile != null) {
                    // With image
                    val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)

                    broadcastService.addBroadcast(
                        title = titleRequestBody,
                        description = descriptionRequestBody,
                        image = imagePart
                    )
                } else {
                    // Without image
                    broadcastService.addBroadcastWithoutImage(
                        title = title,
                        description = description
                    )
                }

                return@withContext if (response.success) {
                    Result.success(response.data.id)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

    override suspend fun updateBroadcast(
        broadcastId: Int,
        title: String,
        description: String,
        imageFile: File
    ): Result<Broadcast> = withContext(Dispatchers.IO) {
        try {
            // Prepare request parts
            val titleRequestBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionRequestBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val methodRequestBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

            // Image
            val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)

            val response = broadcastService.updateBroadcast(
                id = broadcastId,
                title = titleRequestBody,
                description = descriptionRequestBody,
                method = methodRequestBody,
                image = imagePart
            )

            return@withContext if (response.success) {
                val broadcastRecord = response.data

                // Map API response to domain model
                val broadcast = Broadcast(
                    id = broadcastRecord.id,
                    title = broadcastRecord.title,
                    description = broadcastRecord.description,
                    image = broadcastRecord.image,
                    createdAt = broadcastRecord.createdAt,
                    updatedAt = broadcastRecord.updatedAt
                )

                Result.success(broadcast)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun updateBroadcastWithoutImage(
        broadcastId: Int,
        title: String,
        description: String
    ): Result<Broadcast> = withContext(Dispatchers.IO) {
        try {
            val response = broadcastService.updateBroadcastWithoutImage(
                id = broadcastId,
                title = title,
                description = description,
                method = "PUT"
            )

            return@withContext if (response.success) {
                val broadcastRecord = response.data

                // Map API response to domain model
                val broadcast = Broadcast(
                    id = broadcastRecord.id,
                    title = broadcastRecord.title,
                    description = broadcastRecord.description,
                    image = broadcastRecord.image,
                    createdAt = broadcastRecord.createdAt,
                    updatedAt = broadcastRecord.updatedAt
                )

                Result.success(broadcast)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun deleteBroadcast(broadcastId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = broadcastService.deleteBroadcast(broadcastId)

            return@withContext if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}