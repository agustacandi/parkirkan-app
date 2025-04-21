package dev.agustacandi.parkirkanapp.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import retrofit2.Response

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: Throwable) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

fun <T> Flow<Response<T>>.asNetworkResult(): Flow<NetworkResult<T>> {
    return this
        .map { response ->
            if (response.isSuccessful) {
                response.body()?.let {
                    NetworkResult.Success(it)
                } ?: NetworkResult.Error(Exception("Response body is null"))
            } else {
                NetworkResult.Error(Exception("Error: ${response.code()} ${response.message()}"))
            }
        }
        .onStart { emit(NetworkResult.Loading) }
        .catch { e -> emit(NetworkResult.Error(e)) }
}

fun <T, R> Flow<NetworkResult<T>>.mapSuccess(transform: (T) -> R): Flow<NetworkResult<R>> {
    return map { result ->
        when (result) {
            is NetworkResult.Success -> NetworkResult.Success(transform(result.data))
            is NetworkResult.Error -> NetworkResult.Error(result.exception)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}