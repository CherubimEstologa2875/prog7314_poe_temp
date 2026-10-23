package com.example.runway.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.example.runway.R
import com.example.runway.databinding.ViewListRowBinding
import com.google.android.material.color.MaterialColors

// One row in a settings-style list: icon, label, optional value, then a chevron or a control.
class ListRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    // The layout's ids are prefixed `listRow` so they do not clash with screen layout ids.
    private val binding = ViewListRowBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val vertical = resources.getDimensionPixelSize(R.dimen.rw_space_10)
        setPadding(0, vertical, 0, vertical)
        // Ripple feedback for tappable rows.
        setBackgroundResource(outValueOf(android.R.attr.selectableItemBackground))

        context.withStyledAttributes(attrs, R.styleable.ListRowView) {
            val icon = getResourceId(R.styleable.ListRowView_rwIcon, 0)
            if (icon != 0) setIcon(icon)
            label = getString(R.styleable.ListRowView_rwLabel)
            value = getString(R.styleable.ListRowView_rwValue)
            showChevron = getBoolean(R.styleable.ListRowView_rwShowChevron, false)
            danger = getBoolean(R.styleable.ListRowView_rwDanger, false)
        }
    }

    var label: CharSequence?
        get() = binding.listRowLabel.text
        set(value) {
            binding.listRowLabel.text = value
        }

    /** Right-aligned secondary text. Hidden while null or blank. */
    var value: CharSequence?
        get() = binding.listRowValue.text
        set(value) {
            binding.listRowValue.text = value
            binding.listRowValue.isVisible = !value.isNullOrBlank()
        }

    /** Whether the trailing chevron is shown. */
    var showChevron: Boolean = false
        set(show) {
            field = show
            applyChevronVisibility()
        }

    /** The chevron only shows when the trailing slot is empty. */
    private fun applyChevronVisibility() {
        binding.listRowChevron.isVisible = showChevron && !binding.listRowTrailingSlot.isVisible
    }

    /** Tints the label and icon to mark the row as destructive. */
    var danger: Boolean = false
        set(isDanger) {
            field = isDanger
            val colour = MaterialColors.getColor(
                this,
                if (isDanger) androidx.appcompat.R.attr.colorPrimary
                else com.google.android.material.R.attr.colorOnSurface,
            )
            binding.listRowLabel.setTextColor(colour)
            binding.listRowIcon.imageTintList = android.content.res.ColorStateList.valueOf(colour)
        }

    fun setIcon(@DrawableRes iconRes: Int) {
        binding.listRowIcon.setImageResource(iconRes)
        binding.listRowIcon.isVisible = true
    }

    /** Puts a control, normally a switch, at the end of the row and hides the chevron. */
    fun setTrailing(view: View?) {
        binding.listRowTrailingSlot.removeAllViews()
        if (view != null) binding.listRowTrailingSlot.addView(view)
        binding.listRowTrailingSlot.isVisible = view != null
        applyChevronVisibility()
    }

    /** Marks the row tappable: sets the listener and shows the chevron. */
    fun onClick(listener: () -> Unit) {
        showChevron = true
        setOnClickListener { listener() }
    }

    /** Resolves a theme attribute to a drawable resource id. */
    private fun outValueOf(attr: Int): Int {
        val typed = android.util.TypedValue()
        context.theme.resolveAttribute(attr, typed, true)
        return typed.resourceId
    }
}
