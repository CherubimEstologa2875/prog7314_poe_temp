package com.example.runway

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setUpNavigation(isFreshStart = savedInstanceState == null)
        setUpFab()
        watchConnection()
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
        }
    }

    /** Firebase decides whether a session survived, the local store only caches the profile. */
    private fun resolveStartDestination(): Int {
        val container = (application as RunwayApplication).container
        if (!container.googleAuthClient.hasActiveSession) {
            container.authSessionStore.clearSession()
            return R.id.signInFragment
        }

        val session = container.authSessionStore.currentSession ?: return R.id.signInFragment
        return if (session.biometricEnabled) R.id.biometricFragment else R.id.homeFragment
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

    private companion object {
        const val TAG = "MainActivity"

        /** Destinations that are not part of the four-tab shell: auth and the add-item
         * modal flow. The bottom bar and FAB are hidden on these, matching the mockup
         * (TabBar only renders inside TabShell, after AppGate hands off). */
        val CHROMELESS_DESTINATIONS = setOf(
            R.id.onboardingFragment,
            R.id.signInFragment,
            R.id.biometricFragment,
            R.id.cameraFragment,
            R.id.cutoutFragment,
            R.id.tagItemFragment,
        )
    }
}
