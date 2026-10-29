package com.example.runway.data.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

// Android 13+ asks the user before an app may post notifications (Android Developers, 2026b).

object NotificationPermission {

    /** Only Android 13 and newer have a runtime prompt. */
    val needsRuntimeRequest: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS

    /**
     * True when Android will actually show our notifications. Covers the runtime
     * permission and the app-wide switch in Android's settings, which older versions have too.
     */
    fun isAllowed(context: Context): Boolean {
        if (needsRuntimeRequest &&
            ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** The prompt is shown once by itself, after sign-in. After that only the Notifications screen asks. */
    fun hasAskedBefore(context: Context): Boolean = prefs(context).getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) = prefs(context).edit { putBoolean(KEY_ASKED, true) }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "runway_notification_permission"
    private const val KEY_ASKED = "asked"
}

/* Reference List
Android Developers, 2026b. Notification runtime permission. [online] Available at: <https://developer.android.com/develop/ui/views/notifications/notification-permission> [Accessed 29 October 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
