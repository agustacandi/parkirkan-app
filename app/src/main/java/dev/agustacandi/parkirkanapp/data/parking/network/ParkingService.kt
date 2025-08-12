package dev.agustacandi.parkirkanapp.data.parking.network

import com.squareup.moshi.Json
import dev.agustacandi.parkirkanapp.data.parking.response.ParkingResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ParkingService {
    @GET("parking")
    suspend fun getParkingRecords(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): ParkingResponse

    @POST("parking/is-check-in")
    suspend fun isVehicleCheckedIn(@Body request: IsCheckedInRequest): IsCheckedInResponse

    @POST("parking/confirm-check-out")
    suspend fun confirmCheckOut(@Body request: ConfirmCheckOutRequest): ConfirmCheckOutResponse

    @POST("parking/report-check-out")
    suspend fun reportCheckOut(@Body request: ReportCheckOutRequest): ReportCheckOutResponse
}

data class IsCheckedInRequest(
    @Json(name = "license_plate")
    val licensePlate: String
)

data class IsCheckedInResponse(
    val success: Boolean,
    val message: String,
    val data: IsCheckedInData
)

data class IsCheckedInData(
    @Json(name = "is_checked_in")
    val isCheckedIn: Boolean
)

data class ConfirmCheckOutRequest(
    @Json(name = "license_plate")
    val licensePlate: String,
)

data class ConfirmCheckOutResponse(
    val success: Boolean,
    val message: String
)

data class ReportCheckOutRequest(
    @Json(name = "license_plate")
    val licensePlate: String,
)

data class ReportCheckOutResponse(
    val success: Boolean,
    val message: String
)