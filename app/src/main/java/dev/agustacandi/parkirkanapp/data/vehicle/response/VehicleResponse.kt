package dev.agustacandi.parkirkanapp.data.vehicle.response

import com.squareup.moshi.Json

data class VehicleResponse(
    val success: Boolean,
    val data: VehicleData,
    val message: String
)

data class VehicleData(
    @Json(name = "current_page")
    val currentPage: Int,
    val data: List<VehicleRecord>,
    @Json(name = "first_page_url")
    val firstPageUrl: String,
    val from: Int,
    @Json(name = "last_page")
    val lastPage: Int,
    @Json(name = "last_page_url")
    val lastPageUrl: String,
    val links: List<PageLink>,
    @Json(name = "next_page_url")
    val nextPageUrl: String?,
    val path: String,
    @Json(name = "per_page")
    val perPage: Int,
    @Json(name = "prev_page_url")
    val prevPageUrl: String?,
    val to: Int,
    val total: Int
)

data class VehicleRecord(
    val id: Int,
    val name: String,
    @Json(name = "license_plate")
    val licensePlate: String,
    val image: String,
    @Json(name = "user_id")
    val userId: Int,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "updated_at")
    val updatedAt: String
)

data class PageLink(
    val url: String?,
    val label: String,
    val active: Boolean
)

// Add Response
data class AddVehicleResponse(
    val success: Boolean,
    val data: VehicleRecord,
    val message: String
)

// Single vehicle response for getting and updating vehicles
data class SingleVehicleResponse(
    val success: Boolean,
    val data: VehicleRecord,
    val message: String
)

// Response for delete operations
data class DeleteVehicleResponse(
    val success: Boolean,
    val message: String
)