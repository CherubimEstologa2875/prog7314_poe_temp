package com.example.runway.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import com.example.runway.R
import com.google.android.material.color.MaterialColors

/** A circular avatar showing a user's initials. */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var sizePx: Int = resources.getDimensionPixelSize(R.dimen.rw_avatar_size)

    init {
        gravity = Gravity.CENTER
        includeFontPadding = false
        typeface = android.graphics.Typeface.SERIF
        setBackgroundResource(R.drawable.rw_bg_avatar)
        setTextColor(
            MaterialColors.getColor(this, android.R.attr.colorBackground)
        )

        context.withStyledAttributes(attrs, R.styleable.AvatarView) {
            initials = getString(R.styleable.AvatarView_rwInitials).orEmpty()
            avatarSize = getDimensionPixelSize(
                R.styleable.AvatarView_rwAvatarSize,
                resources.getDimensionPixelSize(R.dimen.rw_avatar_size),
            )
        }
    }

    /** The initials shown. Trimmed to two characters and upper-cased. */
    var initials: String = ""
        set(value) {
            field = value
            text = value.take(MAX_INITIALS).uppercase()
        }

    /** Diameter in pixels. The text is scaled with it. */
    var avatarSize: Int
        get() = sizePx
        set(value) {
            sizePx = value
            setTextSize(TypedValue.COMPLEX_UNIT_PX, value * TEXT_SIZE_RATIO)
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force a square so the circle is never stretched.
        val spec = MeasureSpec.makeMeasureSpec(sizePx, MeasureSpec.EXACTLY)
        super.onMeasure(spec, spec)
    }

    private companion object {
        const val MAX_INITIALS = 2
        const val TEXT_SIZE_RATIO = 0.38f
    }
}
