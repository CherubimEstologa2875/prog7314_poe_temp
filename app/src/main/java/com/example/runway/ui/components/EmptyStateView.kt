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
import com.example.runway.databinding.ViewEmptyStateBinding

/** Placeholder shown when a list or screen has no content: icon, title, body and an optional action. */
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val horizontal = resources.getDimensionPixelSize(R.dimen.rw_space_32)
        val vertical = resources.getDimensionPixelSize(R.dimen.rw_space_32)
        setPadding(horizontal, vertical, horizontal, vertical)

        context.withStyledAttributes(attrs, R.styleable.EmptyStateView) {
            val icon = getResourceId(R.styleable.EmptyStateView_rwIcon, 0)
            if (icon != 0) setIcon(icon)
            title = getString(R.styleable.EmptyStateView_rwTitle)
            body = getString(R.styleable.EmptyStateView_rwBody)
        }
    }

    var title: CharSequence?
        get() = binding.emptyTitle.text
        set(value) {
            binding.emptyTitle.text = value
        }

    /** Supporting copy below the title. Hidden while null or blank. */
    var body: CharSequence?
        get() = binding.emptyBody.text
        set(value) {
            binding.emptyBody.text = value
            binding.emptyBody.isVisible = !value.isNullOrBlank()
        }

    fun setIcon(@DrawableRes iconRes: Int) {
        binding.emptyIcon.setImageResource(iconRes)
    }

    /** Places a view, normally a button, below the body text. Pass null to remove it. */
    fun setAction(action: View?) {
        binding.emptyActionSlot.removeAllViews()
        if (action != null) binding.emptyActionSlot.addView(action)
        binding.emptyActionSlot.isVisible = action != null
    }
}
