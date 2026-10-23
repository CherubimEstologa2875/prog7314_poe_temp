package com.example.runway.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.example.runway.R
import com.example.runway.databinding.ViewStatTileBinding

// A card showing one statistic: a large value, a label and an optional sub-label.
class StatTileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewStatTileBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.rw_bg_card)
        val horizontal = resources.getDimensionPixelSize(R.dimen.rw_space_12)
        val vertical = resources.getDimensionPixelSize(R.dimen.rw_space_12)
        setPadding(horizontal, vertical, horizontal, vertical)

        context.withStyledAttributes(attrs, R.styleable.StatTileView) {
            value = getString(R.styleable.StatTileView_rwValue)
            label = getString(R.styleable.StatTileView_rwLabel)
            subLabel = getString(R.styleable.StatTileView_rwSubLabel)
        }
    }

    /** The headline figure. A string so it can include a unit, e.g. "R840". */
    var value: CharSequence?
        get() = binding.statValue.text
        set(v) {
            binding.statValue.text = v
        }

    var label: CharSequence?
        get() = binding.statLabel.text
        set(v) {
            binding.statLabel.text = v
        }

    /** Optional third line. Hidden while null or blank. */
    var subLabel: CharSequence?
        get() = binding.statSubLabel.text
        set(v) {
            binding.statSubLabel.text = v
            binding.statSubLabel.isVisible = !v.isNullOrBlank()
        }
}
