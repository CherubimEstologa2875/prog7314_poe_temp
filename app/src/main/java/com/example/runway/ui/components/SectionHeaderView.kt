package com.example.runway.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.example.runway.R
import com.example.runway.databinding.ViewSectionHeaderBinding

/** A heading between groups of content, with an optional trailing action such as "See all". */
class SectionHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSectionHeaderBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        // Bottom-aligned because the title and action label are different text sizes.
        gravity = Gravity.BOTTOM
        setPadding(
            0,
            resources.getDimensionPixelSize(R.dimen.rw_space_20),
            0,
            resources.getDimensionPixelSize(R.dimen.rw_space_10),
        )

        context.withStyledAttributes(attrs, R.styleable.SectionHeaderView) {
            title = getString(R.styleable.SectionHeaderView_rwTitle)
            actionText = getString(R.styleable.SectionHeaderView_rwActionText)
        }
    }

    var title: CharSequence?
        get() = binding.sectionTitle.text
        set(value) {
            binding.sectionTitle.text = value
        }

    /** The trailing action label. Hidden while null or blank. */
    var actionText: CharSequence?
        get() = binding.sectionAction.text
        set(value) {
            binding.sectionAction.text = value
            binding.sectionAction.isVisible = !value.isNullOrBlank()
        }

    /** Called when the trailing action is tapped. */
    fun onActionClick(listener: () -> Unit) {
        binding.sectionAction.setOnClickListener { listener() }
    }
}
