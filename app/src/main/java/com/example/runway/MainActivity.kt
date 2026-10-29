package com.example.runway

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.runway.databinding.ActivityMainBinding
import com.example.runway.data.auth.AuthDestination
import com.example.runway.data.auth.AuthFlow
import com.example.runway.data.notifications.NotificationPermission
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.ui.AppLanguage
import com.example.runway.ui.components.SyncBannerView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The app's single Activity. Hosts the whole nav graph - auth (splash / onboarding /
 * sign in / biometric), the four-tab shell and the add-item modal flow all live in one
 * NavHostFragment here - and owns the bottom bar, the add-item FAB and the sync banner.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Set when a notification was tapped; acted on once the user is past sign-in.
    private var pendingOpenNotifications = false

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "notification permission granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        syncSavedLanguage()

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setUpNavigation(isFreshStart = savedInstanceState == null)
        setUpFab()
        watchConnection()
        // Only on a fresh start: after rotation the same intent would open the screen again.
        if (savedInstanceState == null) handleNotificationTap(intent)
    }

    // A notification tapped while the activity is already running (SCRUM-162).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationTap(intent)
    }

    private fun handleNotificationTap(intent: Intent?) {
        if (intent?.action != ACTION_OPEN_NOTIFICATIONS) return
        Log.d(TAG, "opened from a notification")
        pendingOpenNotifications = true
        openPendingNotifications()
    }

    // Waits until sign-in and the biometric check are done, so a tap cannot skip them.
    private fun openPendingNotifications() {
        if (!pendingOpenNotifications) return
        val current = navController.currentDestination?.id ?: return
        if (current in AUTH_DESTINATIONS) return
        pendingOpenNotifications = false
        if (current != R.id.notificationsFragment) {
            // Posted so we never navigate from inside a destination-changed callback.
            binding.root.post { navController.navigate(R.id.notificationsFragment) }
        }
    }

    // Asks once, the first time the user reaches Home. Asking on the sign-in screen,
    // before they have seen the app, gets far more refusals (Android Developers, 2026).
    private fun askForNotificationsOnce() {
        if (!NotificationPermission.needsRuntimeRequest) return
        if (NotificationPermission.isAllowed(this) || NotificationPermission.hasAskedBefore(this)) return
        NotificationPermission.markAsked(this)
        notificationPermissionRequest.launch(NotificationPermission.PERMISSION)
    }

    // The language can also be changed from Android's own settings, so the saved setting follows what is showing.
    private fun syncSavedLanguage() {
        val settings = (application as RunwayApplication).container.settingsRepository
        val shown = AppLanguage.current() ?: return
        val saved = settings.currentSettings()
        if (shown == saved.language || shown !in RunwaySettings.SUPPORTED_LANGUAGES) return
        Log.d(TAG, "app is showing $shown but ${saved.language} was saved, updating the setting")
        lifecycleScope.launch { settings.update(saved.copy(language = shown)) }
    }

    // Shows the offline banner, and syncs whatever was queued as soon as the connection is back.
    private fun watchConnection() {
        val container = (application as RunwayApplication).container
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var wasOffline = false
                combine(container.connectivity.isOnline, container.itemRepository.observePendingCount()) { online, queued ->
                    online to queued
                }.collect { (online, queued) ->
                    when {
                        !online -> {
                            wasOffline = true
                            binding.syncBanner.state = SyncBannerView.State.Offline(queued)
                        }
                        wasOffline -> {
                            wasOffline = false
                            Log.d(TAG, "back online, syncing $queued queued changes")
                            binding.syncBanner.state = SyncBannerView.State.Syncing
                            container.itemRepository.refresh()
                            binding.syncBanner.state = SyncBannerView.State.Idle
                        }
                        else -> binding.syncBanner.state = SyncBannerView.State.Idle
                    }
                }
            }
        }
    }

    /** Connects the bottom navigation bar to the nav graph and hides the shell chrome
     * (bottom bar + FAB) on destinations that are not part of the four-tab shell. */
    private fun setUpNavigation(isFreshStart: Boolean) {
        val host = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = host.navController

        // Re-inflated so a returning user does not land on the sign-in screen.
        if (isFreshStart) {
            val graph = navController.navInflater.inflate(R.navigation.runway_nav_graph)
            graph.setStartDestination(resolveStartDestination())
            navController.graph = graph
        }

        binding.bottomNav.setupWithNavController(navController)

        // The center menu entry is intentionally only a spacer for the FAB. Keep
        // it inert while making the four real destinations explicit top-level
        // navigation targets.
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment,
                R.id.wardrobeFragment,
                R.id.outfitsFragment,
                R.id.profileFragment -> {
                    if (navController.currentDestination?.id != item.itemId) {
                        navController.navigate(item.itemId)
                    }
                    true
                }
                R.id.fabSpacer -> false
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "navigated to ${destination.label ?: destination.id}")
            val showChrome = destination.id !in CHROMELESS_DESTINATIONS
            binding.bottomNav.isVisible = showChrome
            binding.addFab.isVisible = showChrome

            if (destination.id == R.id.homeFragment) askForNotificationsOnce()
            openPendingNotifications()
        }
    }

    /** Firebase decides whether a session survived, the local store only caches the profile. */
    private fun resolveStartDestination(): Int {
        val container = (application as RunwayApplication).container
        if (!container.googleAuthClient.hasActiveSession) {
            container.authSessionStore.clearSession()
            return R.id.signInFragment
        }

        return when (AuthFlow.onLaunch(
            firebaseSessionActive = true,
            session = container.authSessionStore.currentSession,
        )) {
            AuthDestination.SIGN_IN -> R.id.signInFragment
            AuthDestination.HOME -> R.id.homeFragment
            AuthDestination.BIOMETRIC -> R.id.biometricFragment
        }
    }

    /** The centre FAB opens the add-item flow as a modal stack over the current tab. */
    private fun setUpFab() {
        binding.addFab.setOnClickListener {
            Log.d(TAG, "add-item FAB tapped")
            navController.navigate(R.id.addItemGraph)
        }
    }

    /** Pads the layout so the system bars do not draw over it. */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            // Top inset goes on the root, not the sync banner, because the banner is usually hidden.
            view.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            binding.bottomNav.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - isFinishing=$isFinishing")
    }

    companion object {
        /** The action on the intent behind every Runway notification. */
        const val ACTION_OPEN_NOTIFICATIONS = "com.example.runway.action.OPEN_NOTIFICATIONS"

        private const val TAG = "MainActivity"

        /** Screens a notification tap must not skip past. */
        private val AUTH_DESTINATIONS = setOf(
            R.id.onboardingFragment,
            R.id.signInFragment,
            R.id.biometricFragment,
        )

        /** Destinations that are not part of the four-tab shell: auth and the add-item
         * modal flow. The bottom bar and FAB are hidden on these, matching the mockup
         * (TabBar only renders inside TabShell, after AppGate hands off). */
        private val CHROMELESS_DESTINATIONS = setOf(
            R.id.onboardingFragment,
            R.id.signInFragment,
            R.id.biometricFragment,
            R.id.cameraFragment,
            R.id.cutoutFragment,
            R.id.tagItemFragment,
        )
    }
}

/* Reference List
Android Developers, 2026. Notification runtime permission. [online] Available at: <https://developer.android.com/develop/ui/views/notifications/notification-permission> [Accessed 29 October 2026].
*/
