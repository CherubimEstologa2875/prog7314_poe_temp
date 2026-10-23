package com.example.runway.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.runway.R
import com.example.runway.databinding.ViewSyncBannerBinding

/** The offline / syncing strip shown under the header. */
class SyncBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSyncBannerBinding.inflate(LayoutInflater.from(context), this)

    /** What the banner is reporting. */
    sealed interface State {
        /** Connected with nothing queued. */
        data object Idle : State

        /** Offline, with [queued] writes waiting in the local queue. */
        data class Offline(val queued: Int) : State

        /** Back online and draining the queue. */
        data object Syncing : State
    }

    /** The banner's current state. Setting [State.Idle] hides the view. */
    var state: State = State.Idle
        set(value) {
            field = value
            when (value) {
                is State.Idle -> isVisible = false

                is State.Offline -> {
                    isVisible = true
                    binding.bannerIcon.setImageResource(R.drawable.ic_rw_wifi_off)
                    binding.bannerText.text = if (value.queued > 0) {
                        resources.getQuantityString(
                            R.plurals.rw_sync_queued, value.queued, value.queued
                        )
                    } else {
                        context.getString(R.string.rw_sync_offline)
                    }
                }

                is State.Syncing -> {
                    isVisible = true
                    binding.bannerIcon.setImageResource(R.drawable.ic_rw_sync)
                    binding.bannerText.setText(R.string.rw_sync_syncing)
                }
            }
        }

    // This init block sits after [state] because Kotlin initialises in source order.
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(
            com.google.android.material.color.MaterialColors.getColor(
                this, androidx.appcompat.R.attr.colorPrimary
            )
        )
        val horizontal = resources.getDimensionPixelSize(R.dimen.rw_screen_padding_horizontal)
        val vertical = resources.getDimensionPixelSize(R.dimen.rw_space_6)
        setPadding(horizontal, vertical, horizontal, vertical)
        state = State.Idle
    }
}
