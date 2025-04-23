package dev.agustacandi.parkirkanapp.data.vehicle.network

import dev.agustacandi.parkirkanapp.data.vehicle.response.AddVehicleResponse
import dev.agustacandi.parkirkanapp.data.vehicle.response.AllVehiclesResponse
import dev.agustacandi.parkirkanapp.data.vehicle.response.DeleteVehicleResponse
import dev.agustacandi.parkirkanapp.data.vehicle.response.SingleVehicleResponse
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
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

    @GET("vehicle/{id}")
    suspend fun getVehicle(
        @Path("id") id: Int
    ): SingleVehicleResponse

    @Multipart
    @POST("vehicle")
    suspend fun addVehicle(
        @Part("name") name: RequestBody,
        @Part("license_plate") licensePlate: RequestBody,
        @Part image: MultipartBody.Part
    ): AddVehicleResponse

    @Multipart
    @POST("vehicle/{id}")
    suspend fun updateVehicle(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("license_plate") licensePlate: RequestBody,
        @Part("_method") method: RequestBody,
        @Part image: MultipartBody.Part
    ): SingleVehicleResponse

    @FormUrlEncoded
    @POST("vehicle/{id}")
    suspend fun updateVehicleWithoutImage(
        @Path("id") id: Int,
        @Field("name") name: String,
        @Field("license_plate") licensePlate: String,
        @Field("_method") method: String = "PUT"
    ): SingleVehicleResponse

    @DELETE("vehicle/{id}")
    suspend fun deleteVehicle(
        @Path("id") id: Int
    ): DeleteVehicleResponse

    @GET("vehicle-all")
    suspend fun getAllVehicles(): AllVehiclesResponse
}

data class VehicleRequest(
    val name: String,
    val licensePlate: String,
    val image: String
)