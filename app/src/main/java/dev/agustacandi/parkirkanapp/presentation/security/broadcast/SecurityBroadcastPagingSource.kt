package dev.agustacandi.parkirkanapp.presentation.security.broadcast

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.agustacandi.parkirkanapp.data.broadcast.network.BroadcastService
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import retrofit2.HttpException
import java.io.IOException

class SecurityBroadcastPagingSource(
    private val broadcastService: BroadcastService,
    private val pageSize: Int
) : PagingSource<Int, Broadcast>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Broadcast> {
        val page = params.key ?: 1

        return try {
            val response = broadcastService.getBroadcasts(page = page, perPage = pageSize)

            if (!response.success) {
                return LoadResult.Error(Exception(response.message))
            }

            val broadcastData = response.data
            val broadcasts = broadcastData.data.map { broadcastRecord ->
                Broadcast(
                    id = broadcastRecord.id,
                    title = broadcastRecord.title,
                    description = broadcastRecord.description,
                    image = broadcastRecord.image,
                    createdAt = broadcastRecord.createdAt,
                    updatedAt = broadcastRecord.updatedAt
                )
            }

            LoadResult.Page(
                data = broadcasts,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (broadcastData.nextPageUrl == null) null else page + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Broadcast>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}