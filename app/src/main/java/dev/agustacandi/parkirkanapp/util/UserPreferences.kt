package dev.agustacandi.parkirkanapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.agustacandi.parkirkanapp.data.auth.response.Data
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val userAdapter = moshi.adapter(Data::class.java)

    // StateFlow untuk mengobservasi perubahan user
    private val _userFlow = MutableStateFlow<Data?>(null)
    val userFlow: StateFlow<Data?> = _userFlow

    // StateFlow untuk mengobservasi status login
    private val _isLoggedInFlow = MutableStateFlow(false)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow

    init {
        // Inisialisasi flow dengan nilai dari SharedPreferences
        _userFlow.value = getUser()
        _isLoggedInFlow.value = getAuthToken() != null
    }

    companion object {
        private const val PREF_NAME = "user_preferences"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER = "user"
        private const val KEY_SELECTED_VEHICLE_ID = "selected_vehicle_id"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    fun saveAuthToken(token: String) {
        prefs.edit {
            putString(KEY_AUTH_TOKEN, token)
        }
        _isLoggedInFlow.value = true
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUser(user: Data) {
        val userJson = userAdapter.toJson(user)
        prefs.edit {
            putString(KEY_USER, userJson)
        }
        _userFlow.value = user
    }

    fun getUser(): Data? {
        val userJson = prefs.getString(KEY_USER, null) ?: return null
        return try {
            userAdapter.fromJson(userJson)
        } catch (e: Exception) {
            null
        }
    }

    fun getUserId(): String {
        return getUser()?.id.toString()
    }

    fun saveFcmToken(token: String) {
        prefs.edit {
            putString(KEY_FCM_TOKEN, token)
        }
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun clearUserData() {
        prefs.edit {
            clear()
        }
        _userFlow.value = null
        _isLoggedInFlow.value = false
    }

    fun saveSelectedVehicleId(vehicleId: Int?) {
        prefs.edit {
            if (vehicleId != null) {
                putInt(KEY_SELECTED_VEHICLE_ID, vehicleId)
            } else {
                remove(KEY_SELECTED_VEHICLE_ID)
            }
        }
    }

    fun getSelectedVehicleId(): Int? {
        return if (prefs.contains(KEY_SELECTED_VEHICLE_ID)) {
            prefs.getInt(KEY_SELECTED_VEHICLE_ID, -1)
        } else {
            null
        }
    }

}