package dev.agustacandi.parkirkanapp.data.broadcast.network

import dev.agustacandi.parkirkanapp.data.broadcast.response.BroadcastResponse
import dev.agustacandi.parkirkanapp.data.broadcast.response.DeleteBroadcastResponse
import dev.agustacandi.parkirkanapp.data.broadcast.response.SingleBroadcastResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface BroadcastService {
    @GET("broadcast-all")
    suspend fun getBroadcastsAll(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): BroadcastResponse

    @GET("broadcast")
    suspend fun getBroadcasts(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): BroadcastResponse

    @GET("broadcast/{id}")
    suspend fun getBroadcast(
        @Path("id") id: Int
    ): SingleBroadcastResponse

    @Multipart
    @POST("broadcast")
    suspend fun addBroadcast(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part image: MultipartBody.Part
    ): SingleBroadcastResponse

    @FormUrlEncoded
    @POST("broadcast")
    suspend fun addBroadcastWithoutImage(
        @Field("title") title: String,
        @Field("description") description: String
    ): SingleBroadcastResponse

    @Multipart
    @POST("broadcast/{id}")
    suspend fun updateBroadcast(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("_method") method: RequestBody,
        @Part image: MultipartBody.Part
    ): SingleBroadcastResponse

    @FormUrlEncoded
    @POST("broadcast/{id}")
    suspend fun updateBroadcastWithoutImage(
        @Path("id") id: Int,
        @Field("title") title: String,
        @Field("description") description: String,
        @Field("_method") method: String
    ): SingleBroadcastResponse

    @DELETE("broadcast/{id}")
    suspend fun deleteBroadcast(
        @Path("id") id: Int
    ): DeleteBroadcastResponse
}