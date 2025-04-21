package dev.agustacandi.parkirkanapp.presentation.vehicle

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.agustacandi.parkirkanapp.data.vehicle.network.VehicleService
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import retrofit2.HttpException
import java.io.IOException

class VehiclePagingSource(
    private val vehicleService: VehicleService,
    private val pageSize: Int
) : PagingSource<Int, VehicleRecord>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VehicleRecord> {
        val page = params.key ?: 1

        return try {
            val response = vehicleService.getVehicles(page = page, perPage = pageSize)

            if (!response.success) {
                return LoadResult.Error(Exception(response.message))
            }

            val vehicleData = response.data
            val vehicleRecords = vehicleData.data

            LoadResult.Page(
                data = vehicleRecords,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (vehicleData.nextPageUrl == null) null else page + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, VehicleRecord>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}