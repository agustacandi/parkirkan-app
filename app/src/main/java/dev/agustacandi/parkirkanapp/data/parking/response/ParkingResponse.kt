package dev.agustacandi.parkirkanapp.data.parking.response

import com.squareup.moshi.Json

data class ParkingResponse(
	val success: Boolean,
	val data: ParkingData,
	val message: String
)

data class ParkingData(
	@Json(name = "current_page")
	val currentPage: Int,
	val data: List<ParkingRecord>,
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

data class ParkingRecord(
	val id: Int,
	@Json(name = "check_in_time")
	val checkInTime: String,
	@Json(name = "check_in_image")
	val checkInImage: String,
	@Json(name = "check_out_time")
	val checkOutTime: String? = null,
	@Json(name = "check_out_image")
	val checkOutImage: String? = null,
	@Json(name = "is_check_out_confirmed")
	val isCheckOutConfirmed: Boolean,
	val status: String,
	@Json(name = "vehicle_id")
	val vehicleId: Int,
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