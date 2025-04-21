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
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
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

    override suspend fun addVehicle(name: String, licensePlate: String, imageFile: File): Result<VehicleRecord> = withContext(Dispatchers.IO) {
        try {
            // Persiapkan request body
            val nameRequestBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val licensePlateRequestBody = licensePlate.toRequestBody("text/plain".toMediaTypeOrNull())

            // Persiapkan file
            val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)

            // Buat request
            val response = vehicleService.addVehicle(nameRequestBody, licensePlateRequestBody, imagePart)

            // Periksa response
            if (response.success) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }}