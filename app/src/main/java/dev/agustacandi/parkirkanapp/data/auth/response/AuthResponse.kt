package dev.agustacandi.parkirkanapp.data.auth.response

import com.squareup.moshi.Json

data class AuthResponse(

	@Json(name="data")
	val data: Data,

	@Json(name="success")
	val success: Boolean,

	@Json(name="message")
	val message: String
)

data class Data(

	@Json(name="role")
	val role: String,

	@Json(name="updated_at")
	val updatedAt: String,

	@Json(name="phone")
	val phone: String,

	@Json(name="name")
	val name: String,

	@Json(name="created_at")
	val createdAt: String,

	@Json(name="email_verified_at")
	val emailVerifiedAt: String? = null,

	@Json(name="id")
	val id: Int,

	@Json(name="email")
	val email: String,

	@Json(name="token")
	val token: String
)

data class ErrorResponse(
	@Json(name="success")
	val success: Boolean,

	@Json(name="message")
	val message: String,

	@Json(name="data")
	val data: ErrorData
)

data class ErrorData(
	@Json(name="error")
	val error: String
)