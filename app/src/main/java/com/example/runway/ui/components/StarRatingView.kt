package com.example.runway.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import com.example.runway.R

/** A five-star rating that is read-only by default, or tappable when [isEditable] is set. */
class StarRatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    /** The available star sizes. */
    enum class StarSize(val dimenRes: Int) {
        SMALL(R.dimen.rw_star_size_small),
        MEDIUM(R.dimen.rw_star_size_medium),
        LARGE(R.dimen.rw_star_size_large),
    }

    private val stars = mutableListOf<ImageView>()
    private var onRatingChanged: ((Int) -> Unit)? = null

    /** The current value, clamped to 0..[MAX_STARS]. */
    var rating: Int = 0
        set(value) {
            val clamped = value.coerceIn(0, MAX_STARS)
            field = clamped
            render()
        }

    /** Whether taps change the rating. */
    var isEditable: Boolean = false
        set(value) {
            field = value
            render()
        }

    var starSize: StarSize = StarSize.MEDIUM
        set(value) {
            field = value
            buildStars()
            render()
        }

    // This init block sits after the properties because Kotlin initialises in source order.
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        var size = StarSize.MEDIUM
        var initial = 0
        var editable = false

        context.withStyledAttributes(attrs, R.styleable.StarRatingView) {
            initial = getInteger(R.styleable.StarRatingView_rwRating, 0)
            editable = getBoolean(R.styleable.StarRatingView_rwEditable, false)
            size = when (getInt(R.styleable.StarRatingView_rwStarSize, 1)) {
                0 -> StarSize.SMALL
                2 -> StarSize.LARGE
                else -> StarSize.MEDIUM
            }
        }

        // starSize must be set first because it builds the star views.
        starSize = size
        isEditable = editable
        rating = initial
    }

    /** Called when the user taps a star. Setting a listener also makes the rating editable. */
    fun onRatingChanged(listener: (Int) -> Unit) {
        onRatingChanged = listener
        isEditable = true
    }

    private fun buildStars() {
        removeAllViews()
        stars.clear()
        val px = resources.getDimensionPixelSize(starSize.dimenRes)
        val gap = resources.getDimensionPixelSize(R.dimen.rw_space_4)

        for (index in 0 until MAX_STARS) {
            val star = ImageView(context).apply {
                layoutParams = LayoutParams(px, px).apply {
                    if (index > 0) marginStart = gap
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                imageTintList = AppCompatResources.getColorStateList(context, R.color.rw_star_tint)
                setOnClickListener {
                    if (isEditable) {
                        rating = index + 1
                        onRatingChanged?.invoke(rating)
                    }
                }
            }
            stars.add(star)
            addView(star)
        }
    }

    private fun render() {
        if (stars.isEmpty()) buildStars()

        stars.forEachIndexed { index, star ->
            val filled = index < rating
            star.setImageResource(if (filled) R.drawable.ic_rw_star_filled else R.drawable.ic_rw_star)
            star.isSelected = filled
            star.isClickable = isEditable
            star.isFocusable = isEditable
            star.background = if (isEditable) rippleBackground() else null
            star.contentDescription = if (isEditable) {
                resources.getQuantityString(R.plurals.rw_star_rating, index + 1, index + 1)
            } else {
                null
            }
        }

        contentDescription = if (isEditable) {
            null
        } else {
            context.getString(R.string.rw_rating_current, rating, MAX_STARS)
        }
    }

    private fun rippleBackground() = with(android.util.TypedValue()) {
        context.theme.resolveAttribute(
            android.R.attr.selectableItemBackgroundBorderless, this, true
        )
        AppCompatResources.getDrawable(context, resourceId)
    }

    companion object {
        const val MAX_STARS = 5
    }
}
