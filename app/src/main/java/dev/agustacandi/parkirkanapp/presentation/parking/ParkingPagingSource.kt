package dev.agustacandi.parkirkanapp.presentation.parking

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.agustacandi.parkirkanapp.data.parking.network.ParkingService
import dev.agustacandi.parkirkanapp.data.parking.response.ParkingRecord
import retrofit2.HttpException
import java.io.IOException

class ParkingPagingSource(
    private val parkingService: ParkingService,
    private val pageSize: Int
) : PagingSource<Int, ParkingRecord>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ParkingRecord> {
        val page = params.key ?: 1

        return try {
            val response = parkingService.getParkingRecords(page = page, perPage = pageSize)

            if (!response.success) {
                return LoadResult.Error(Exception(response.message))
            }

            val parkingData = response.data
            val parkingRecords = parkingData.data

            LoadResult.Page(
                data = parkingRecords,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (parkingData.nextPageUrl == null) null else page + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ParkingRecord>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}