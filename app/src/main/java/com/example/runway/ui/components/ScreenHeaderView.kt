package com.example.runway.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.example.runway.R
import com.example.runway.databinding.ViewScreenHeaderBinding
import com.google.android.material.button.MaterialButton

/** Screen top bar with a back button, title, optional subtitle and trailing action buttons. */
class ScreenHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewScreenHeaderBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.rw_header_height)
        setBackgroundResource(R.drawable.rw_bg_header)
        val edge = resources.getDimensionPixelSize(R.dimen.rw_space_8)
        setPadding(edge, 0, edge, 0)

        context.withStyledAttributes(attrs, R.styleable.ScreenHeaderView) {
            title = getString(R.styleable.ScreenHeaderView_rwTitle)
            subtitle = getString(R.styleable.ScreenHeaderView_rwSubtitle)
            setLarge(getBoolean(R.styleable.ScreenHeaderView_rwLarge, false))
            setShowBack(getBoolean(R.styleable.ScreenHeaderView_rwShowBack, true))
        }
    }

    var title: CharSequence?
        get() = binding.headerTitle.text
        set(value) {
            binding.headerTitle.text = value
        }

    /** Optional second line. Hidden when null or blank. */
    var subtitle: CharSequence?
        get() = binding.headerSubtitle.text
        set(value) {
            binding.headerSubtitle.text = value
            binding.headerSubtitle.isVisible = !value.isNullOrBlank()
        }

    /** Switches the title to the larger display size. */
    fun setLarge(large: Boolean) {
        binding.headerTitle.setTextAppearance(
            if (large) R.style.TextAppearance_Runway_Display_Large
            else R.style.TextAppearance_Runway_Display
        )
    }

    /** Shows or hides the back button. Hiding keeps its space so the title does not shift. */
    fun setShowBack(show: Boolean) {
        binding.headerBack.visibility = if (show) View.VISIBLE else View.GONE
        // Without a back button the title lines up with the screen content below it.
        val start = resources.getDimensionPixelSize(if (show) R.dimen.rw_space_8 else R.dimen.rw_space_12)
        setPaddingRelative(start, paddingTop, paddingEnd, paddingBottom)
    }

    /** Called when the back button is tapped. Also shows it if it was hidden. */
    fun onBackClick(listener: () -> Unit) {
        setShowBack(true)
        binding.headerBack.setOnClickListener { listener() }
    }

    /** Adds a trailing icon button to the header and returns it. */
    fun addAction(
        iconRes: Int,
        contentDescription: CharSequence,
        onClick: () -> Unit,
    ): MaterialButton {
        val button = MaterialButton(
            context, null, com.google.android.material.R.attr.materialIconButtonStyle
        ).apply {
            setIconResource(iconRes)
            this.contentDescription = contentDescription
            setOnClickListener { onClick() }
        }
        binding.headerActions.addView(button)
        return button
    }

    /** Removes every trailing action button. */
    fun clearActions() = binding.headerActions.removeAllViews()
}
