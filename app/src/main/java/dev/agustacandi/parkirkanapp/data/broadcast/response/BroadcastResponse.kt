package dev.agustacandi.parkirkanapp.data.broadcast.response

import com.squareup.moshi.Json

data class BroadcastResponse(
    val success: Boolean,
    val data: BroadcastData,
    val message: String
)

data class BroadcastData(
    @Json(name = "current_page")
    val currentPage: Int,
    val data: List<BroadcastRecord>,
    @Json(name = "first_page_url")
    val firstPageUrl: String,
    val from: Int? = null,
    @Json(name = "last_page")
    val lastPage: Int,
    @Json(name = "last_page_url")
    val lastPageUrl: String,
    val links: List<BroadcastPageLink>,
    @Json(name = "next_page_url")
    val nextPageUrl: String?,
    val path: String,
    @Json(name = "per_page")
    val perPage: Int,
    @Json(name = "prev_page_url")
    val prevPageUrl: String?,
    val to: Int? = null,
    val total: Int
)

data class BroadcastRecord(
    val id: Int,
    val title: String,
    val description: String,
    val image: String?,
    @Json(name = "user_id")
    val userId: Int,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "updated_at")
    val updatedAt: String
)

data class BroadcastPageLink(
    val url: String?,
    val label: String,
    val active: Boolean
)

// Single broadcast response
data class SingleBroadcastResponse(
    val success: Boolean,
    val data: BroadcastRecord,
    val message: String
)

// Response for delete operations
data class DeleteBroadcastResponse(
    val success: Boolean,
    val message: String
)