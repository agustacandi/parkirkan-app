package dev.agustacandi.parkirkanapp.data.parking.network

import dev.agustacandi.parkirkanapp.data.parking.response.ParkingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ParkingService {
    @GET("parking")
    suspend fun getParkingRecords(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): ParkingResponse
}