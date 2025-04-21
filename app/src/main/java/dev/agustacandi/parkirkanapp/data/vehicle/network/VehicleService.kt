package dev.agustacandi.parkirkanapp.data.vehicle.network

import dev.agustacandi.parkirkanapp.data.vehicle.response.AddVehicleResponse
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface VehicleService {
    @GET("vehicle")
    suspend fun getVehicles(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): VehicleResponse

    @PUT("vehicle/{id}")
    @FormUrlEncoded
    suspend fun updateVehicle(
        @Path("id") id: Int,
        @Body request: VehicleRequest
    ): VehicleResponse

    @POST("vehicle")
    @Multipart
    suspend fun addVehicle(
        @Part("name") name: RequestBody,
        @Part("license_plate") licensePlate: RequestBody,
        @Part image: MultipartBody.Part    ): AddVehicleResponse

    @DELETE("vehicle/{id}")
    suspend fun deleteVehicle(
        @Path("id") id: Int
    ): VehicleResponse
}

data class VehicleRequest(
    val name: String,
    val licensePlate: String,
    val image: String
)