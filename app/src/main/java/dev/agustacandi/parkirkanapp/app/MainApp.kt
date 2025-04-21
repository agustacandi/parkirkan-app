package dev.agustacandi.parkirkanapp.app

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApp: Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any libraries or components here
        FirebaseApp.initializeApp(this)
    }
}