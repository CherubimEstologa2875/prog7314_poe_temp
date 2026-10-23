package com.example.runway.ui.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.example.runway.R
import com.google.android.material.color.MaterialColors

/** A colour chip: a filled circle with a thin border so white and black still show up. */
class SwatchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val circle = GradientDrawable().apply { shape = GradientDrawable.OVAL }
    private var sizePx: Int = resources.getDimensionPixelSize(R.dimen.rw_swatch_size)

    init {
        background = circle
        applyStroke()

        context.withStyledAttributes(attrs, R.styleable.SwatchView) {
            swatchColor = getColor(R.styleable.SwatchView_rwSwatchColor, 0)
            swatchSize = getDimensionPixelSize(
                R.styleable.SwatchView_rwSwatchSize,
                resources.getDimensionPixelSize(R.dimen.rw_swatch_size),
            )
        }
    }

    /** The colour shown, as an ARGB int. */
    var swatchColor: Int = 0
        set(value) {
            field = value
            circle.setColor(value)
        }

    // Thick accent ring, used by colour pickers to show which one is chosen.
    var picked: Boolean = false
        set(value) {
            field = value
            applyStroke()
        }

    var swatchSize: Int
        get() = sizePx
        set(value) {
            sizePx = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = MeasureSpec.makeMeasureSpec(sizePx, MeasureSpec.EXACTLY)
        super.onMeasure(spec, spec)
    }

    /** Sets the border, resolving its colour from the theme since GradientDrawable needs a literal colour. */
    private fun applyStroke() {
        if (picked) {
            circle.setStroke(
                resources.getDimensionPixelSize(R.dimen.rw_border_width) * 3,
                MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary),
            )
        } else {
            circle.setStroke(
                resources.getDimensionPixelSize(R.dimen.rw_border_width),
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline),
            )
        }
    }
}
