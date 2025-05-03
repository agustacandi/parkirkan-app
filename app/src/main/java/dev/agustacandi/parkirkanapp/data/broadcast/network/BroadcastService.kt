package dev.agustacandi.parkirkanapp.data.broadcast.network

import com.squareup.moshi.Json
import dev.agustacandi.parkirkanapp.data.broadcast.response.BroadcastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BroadcastService {
    @GET("broadcast-all")
    suspend fun getBroadcasts(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): BroadcastResponse
}