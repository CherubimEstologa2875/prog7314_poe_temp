package com.example.runway.ui

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

// Switches the language the whole app is shown in, separate from the phone's own language.
object AppLanguage {

    private const val TAG = "AppLanguage"

    // AppCompat redraws every open screen in the new language and remembers it for next time.
    fun apply(code: String) {
        Log.d(TAG, "switching app language to $code")
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    // Null means the app has never been given a language and is just following the phone.
    fun current(): String? = AppCompatDelegate.getApplicationLocales()[0]?.language
}
