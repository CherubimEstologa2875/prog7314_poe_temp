package com.example.runway.ui.catalog

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.runway.R
import com.example.runway.databinding.ActivityCatalogBinding
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.components.SyncBannerView
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch

// Shows every component in the library on one screen, with a day/night toggle.
class CatalogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        enableEdgeToEdge()
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        binding.catalogHeader.onBackClick { finish() }

        bindThemeToggle()
        bindChips()
        bindRating()
        bindInputs()
        bindRows()
        bindFeedback()
        bindEmptyState()
        bindIconGrid()
    }

    /** Switches night mode for the whole app. */
    private fun bindThemeToggle() {
        val current = AppCompatDelegate.getDefaultNightMode()
        binding.catalogSegmented.check(
            when (current) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.segmentDay
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.segmentNight
                else -> R.id.segmentSystem
            }
        )

        binding.catalogSegmented.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.segmentDay -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.segmentNight -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (mode != AppCompatDelegate.getDefaultNightMode()) {
                Log.d(TAG, "night mode -> $mode")
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    /** Filter chips, one per wardrobe category. */
    private fun bindChips() {
        CATEGORIES.forEachIndexed { index, label ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = index == 0
                setEnsureMinTouchTargetSize(false)
            }
            binding.catalogChips.addView(chip)
        }
    }

    private fun bindRating() {
        fun showValue(value: Int) {
            binding.catalogStarsValue.text =
                getString(R.string.rw_catalog_stars_value, value, com.example.runway.ui.components.StarRatingView.MAX_STARS)
        }
        showValue(binding.catalogStars.rating)
        binding.catalogStars.onRatingChanged { value ->
            Log.d(TAG, "rating changed to $value")
            showValue(value)
        }
    }

    /** Shows the three field states: normal, error and multi-line. */
    private fun bindInputs() {
        binding.catalogField.placeholder = getString(R.string.rw_catalog_field_placeholder)

        // Pre-set so the error style is visible straight away.
        binding.catalogFieldError.error = getString(R.string.rw_catalog_error_text)
        binding.catalogFieldError.editText.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        binding.catalogNotes.setMultiline()
    }

    private fun bindRows() {
        binding.catalogRowNavigation.onClick {
            RunwayToast.show(binding.root, R.string.rw_catalog_row_notifications)
        }
        binding.catalogRowValue.onClick {
            RunwayToast.show(binding.root, R.string.rw_catalog_row_language)
        }
        binding.catalogRowSwitch.setTrailing(
            MaterialSwitch(this).apply {
                isChecked = true
                setOnCheckedChangeListener { _, checked ->
                    Log.d(TAG, "wash reminders -> $checked")
                }
            }
        )
        binding.catalogRowDanger.onClick {
            RunwayDialogs.confirm(
                context = this,
                titleRes = R.string.rw_catalog_delete_title,
                bodyRes = R.string.rw_catalog_delete_body,
                confirmLabelRes = R.string.rw_action_delete,
                destructive = true,
            ) {
                RunwayToast.show(binding.root, R.string.rw_action_delete, R.drawable.ic_rw_trash)
            }
        }
    }

    private fun bindFeedback() {
        binding.btnToast.setOnClickListener {
            RunwayToast.show(binding.root, R.string.rw_catalog_toast_text)
        }
        binding.btnSheet.setOnClickListener {
            CatalogSheet().show(supportFragmentManager, CatalogSheet.TAG)
        }
        binding.btnDialog.setOnClickListener {
            RunwayDialogs.confirm(
                context = this,
                titleRes = R.string.rw_catalog_dialog_title,
                bodyRes = R.string.rw_catalog_dialog_body,
                confirmLabelRes = R.string.rw_catalog_dialog_confirm,
            ) {
                RunwayToast.show(binding.root, R.string.rw_catalog_dialog_confirm)
            }
        }
        binding.btnDestructive.setOnClickListener {
            RunwayDialogs.confirm(
                context = this,
                titleRes = R.string.rw_catalog_delete_title,
                bodyRes = R.string.rw_catalog_delete_body,
                confirmLabelRes = R.string.rw_action_delete,
                destructive = true,
            ) {
                RunwayToast.show(binding.root, R.string.rw_action_delete, R.drawable.ic_rw_trash)
            }
        }

        // Forced into the offline state so the banner is actually visible here.
        binding.catalogBanner.state = SyncBannerView.State.Offline(queued = 3)
    }

    private fun bindEmptyState() {
        binding.catalogEmpty.setAction(
            com.google.android.material.button.MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonStyle
            ).apply {
                setText(R.string.rw_catalog_empty_action)
                setIconResource(R.drawable.ic_rw_plus)
                setOnClickListener {
                    RunwayToast.show(binding.root, R.string.rw_catalog_empty_action)
                }
            }
        )
    }

    /** Lays every icon out in rows of [ICONS_PER_ROW]. */
    private fun bindIconGrid() {
        val size = resources.getDimensionPixelSize(R.dimen.rw_space_24)
        val gap = resources.getDimensionPixelSize(R.dimen.rw_space_12)

        ICONS.chunked(ICONS_PER_ROW).forEach { rowIcons ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = gap }
            }
            rowIcons.forEach { iconRes ->
                row.addView(
                    ImageView(this).apply {
                        setImageResource(iconRes)
                        contentDescription = null
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        layoutParams = LinearLayout.LayoutParams(0, size, 1f)
                    }
                )
            }
            // Pad the last row so its icons keep the same width.
            repeat(ICONS_PER_ROW - rowIcons.size) {
                row.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, size, 1f)
                })
            }
            binding.catalogIcons.addView(row)
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.catalogRoot) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            binding.catalogScroll.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private companion object {
        const val TAG = "CatalogActivity"
        const val ICONS_PER_ROW = 8

        val CATEGORIES = listOf(
            "All", "Tops", "Bottoms", "Outerwear", "Dresses", "Shoes", "Accessories"
        )

        /** Every icon in the library. */
        val ICONS = listOf(
            R.drawable.ic_rw_home, R.drawable.ic_rw_hanger, R.drawable.ic_rw_plus,
            R.drawable.ic_rw_sparkles, R.drawable.ic_rw_user, R.drawable.ic_rw_chevron_left,
            R.drawable.ic_rw_chevron_right, R.drawable.ic_rw_search, R.drawable.ic_rw_filter,
            R.drawable.ic_rw_sort, R.drawable.ic_rw_close, R.drawable.ic_rw_heart,
            R.drawable.ic_rw_heart_filled, R.drawable.ic_rw_star, R.drawable.ic_rw_star_filled,
            R.drawable.ic_rw_camera, R.drawable.ic_rw_image, R.drawable.ic_rw_trash,
            R.drawable.ic_rw_edit, R.drawable.ic_rw_calendar, R.drawable.ic_rw_bell,
            R.drawable.ic_rw_settings, R.drawable.ic_rw_globe, R.drawable.ic_rw_moon,
            R.drawable.ic_rw_sun, R.drawable.ic_rw_check, R.drawable.ic_rw_cloud,
            R.drawable.ic_rw_rain, R.drawable.ic_rw_wind, R.drawable.ic_rw_sync,
            R.drawable.ic_rw_wifi_off, R.drawable.ic_rw_swap, R.drawable.ic_rw_gift,
            R.drawable.ic_rw_fingerprint, R.drawable.ic_rw_layers_up, R.drawable.ic_rw_layers_down,
            R.drawable.ic_rw_rotate, R.drawable.ic_rw_resize, R.drawable.ic_rw_chart,
            R.drawable.ic_rw_droplet, R.drawable.ic_rw_alert, R.drawable.ic_rw_logout,
            R.drawable.ic_rw_palette, R.drawable.ic_rw_minus, R.drawable.ic_rw_wand,
            R.drawable.ic_rw_google,
        )
    }
}
