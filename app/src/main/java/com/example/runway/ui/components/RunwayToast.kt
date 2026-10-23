package com.example.runway.ui.components

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.example.runway.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar


object RunwayToast {

    private const val TAG = "RunwayToast"

    fun show(
        view: View,
        message: CharSequence,
        @DrawableRes iconRes: Int = R.drawable.ic_rw_check,
        anchor: View? = null,
    ) {
        Log.d(TAG, "show: \"$message\"")

        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        if (anchor != null) snackbar.anchorView = anchor

        // Inverts against the page: a dark pill in Day, a light one in Night.
        val pillFill = MaterialColors.getColor(
            snackbar.view, com.google.android.material.R.attr.colorOnSurface
        )
        val pillText = MaterialColors.getColor(snackbar.view, android.R.attr.colorBackground)

        snackbar.view.apply {
            setBackgroundResource(R.drawable.rw_bg_toast)
            backgroundTintList = null
            elevation = resources.getDimension(R.dimen.rw_space_6)
            val margin = resources.getDimensionPixelSize(R.dimen.rw_space_16)
            updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                setMargins(margin, margin, margin, margin)
            }
        }

        snackbar.view.background?.setTint(pillFill)

        snackbar.view
            .findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            ?.apply {
                setTextAppearance(R.style.TextAppearance_Runway_Secondary)
                setTextColor(pillText)
                setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
                TextViewCompat.setCompoundDrawableTintList(
                    this, android.content.res.ColorStateList.valueOf(pillText)
                )
                compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.rw_space_8)
                maxLines = MAX_LINES
            }

        snackbar.show()
    }

    /** Convenience overload for a string resource. */
    fun show(
        view: View,
        @StringRes messageRes: Int,
        @DrawableRes iconRes: Int = R.drawable.ic_rw_check,
        anchor: View? = null,
    ) = show(view, view.context.getString(messageRes), iconRes, anchor)

    private const val MAX_LINES = 2
}
