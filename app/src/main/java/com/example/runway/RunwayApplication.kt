package com.example.runway

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.runway.data.notifications.NotificationChannels
import com.example.runway.di.AppContainer
import com.example.runway.domain.model.ThemeOption
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Registered in AndroidManifest.xml as android:name=".RunwayApplication".
class RunwayApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applySavedTheme()
        NotificationChannels.create(this)
        registerForPushWhenSignedIn()
    }

    // Fires once now if a session survived, and again on every sign-in, so the API always
    // has this device's current FCM token (SCRUM-161).
    private fun registerForPushWhenSignedIn() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                container.appScope.launch { container.pushTokenRepository.register() }
            }
        }
    }

    // Runs before any activity so the app opens in whatever theme was chosen last time.
    private fun applySavedTheme() {
        val mode = when (container.settingsRepository.currentSettings().theme) {
            ThemeOption.DAY -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeOption.NIGHT -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeOption.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
