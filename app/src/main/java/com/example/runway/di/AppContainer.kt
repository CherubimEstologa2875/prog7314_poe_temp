package com.example.runway.di

import android.content.Context
import androidx.room.Room
import com.example.runway.data.auth.AuthSessionStore
import com.example.runway.data.auth.GoogleAuthClient
import com.example.runway.data.local.ItemImageStore
import com.example.runway.data.local.PreferencesColourStore
import com.example.runway.data.local.PreferencesSettingsStore
import com.example.runway.data.local.RunwayDatabase
import com.example.runway.data.local.SyncPreferences
import com.example.runway.data.network.ConnectivityObserver
import com.example.runway.data.remote.api.ApiClient
import com.example.runway.data.repository.ApiOutfitRepository
import com.example.runway.data.repository.ApiPlanRepository
import com.example.runway.data.repository.ApiProfileRepository
import com.example.runway.data.repository.ApiRatingRepository
import com.example.runway.data.repository.ApiWardrobeRepository
import com.example.runway.data.repository.OfflineItemRepository
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.domain.repository.PlanRepository
import com.example.runway.domain.repository.ProfileRepository
import com.example.runway.domain.repository.RatingRepository
import com.example.runway.domain.repository.SettingsRepository
import com.example.runway.domain.repository.WardrobeRepository
import com.google.firebase.auth.FirebaseAuth

// Manual dependency container for the Runway app.

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: RunwayDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            RunwayDatabase::class.java,
            RunwayDatabase.NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private val syncPreferences: SyncPreferences by lazy {
        SyncPreferences(appContext)
    }

    val itemRepository: ItemRepository by lazy {
        OfflineItemRepository(
            dao = database.itemDao(),
            api = ApiClient.api,
            syncPreferences = syncPreferences,
            currentUid = { FirebaseAuth.getInstance().currentUser?.uid },
        )
    }

    // Garment photos, kept on this device alongside the Room rows.
    // The colours on offer when tagging, including any the user mixed themselves.
    val colourRepository: com.example.runway.domain.repository.ColourRepository by lazy {
        PreferencesColourStore(appContext)
    }

    val itemImageStore: ItemImageStore by lazy {
        ItemImageStore(appContext)
    }

    // Saved outfits. These come straight from the API rather than from Room, so
    // unlike the wardrobe they need a connection.
    val outfitRepository: OutfitRepository by lazy {
        ApiOutfitRepository(ApiClient.api)
    }

    // Confidence ratings, API-backed so a rating follows the user to another device.
    val ratingRepository: RatingRepository by lazy {
        ApiRatingRepository(ApiClient.api)
    }

    // The outfit planner. API-backed so plans show up on every device.
    val planRepository: PlanRepository by lazy {
        ApiPlanRepository(ApiClient.api)
    }

    // Totals for the Home and Profile stat tiles.
    val wardrobeRepository: WardrobeRepository by lazy {
        ApiWardrobeRepository(ApiClient.api)
    }

    val profileRepository: ProfileRepository by lazy {
        ApiProfileRepository(ApiClient.api)
    }

    val googleAuthClient: GoogleAuthClient by lazy {
        // Credential Manager needs the FirebaseAuth instance to exchange the
        // Google credential for a Firebase user.
        GoogleAuthClient(appContext, FirebaseAuth.getInstance())
    }

    val authSessionStore: AuthSessionStore by lazy {
        AuthSessionStore(appContext)
    }

    val connectivity: ConnectivityObserver by lazy {
        ConnectivityObserver(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        PreferencesSettingsStore(appContext)
    }
}
