package com.example.runway.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.example.runway.R
import com.google.android.material.color.MaterialColors

/** A custom-drawn circular progress ring for determinate progress. */
class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arcBounds = RectF()
    private var sizePx = resources.getDimensionPixelSize(R.dimen.rw_progress_ring_size)

    init {
        val defaultWidth = resources.getDimensionPixelSize(R.dimen.rw_progress_ring_width).toFloat()
        trackPaint.strokeWidth = defaultWidth
        progressPaint.strokeWidth = defaultWidth
        trackPaint.color =
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
        progressPaint.color =
            MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)

        context.withStyledAttributes(attrs, R.styleable.ProgressRingView) {
            progress = getFloat(R.styleable.ProgressRingView_rwProgress, 0f)
            val width = getDimension(R.styleable.ProgressRingView_rwRingWidth, defaultWidth)
            trackPaint.strokeWidth = width
            progressPaint.strokeWidth = width
        }
    }

    /** Completion from 0f to 1f. Values outside that range are clamped. */
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = MeasureSpec.makeMeasureSpec(sizePx, MeasureSpec.EXACTLY)
        super.onMeasure(spec, spec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Inset by half the stroke width, otherwise the ring is clipped at the edges.
        val inset = progressPaint.strokeWidth / 2f
        arcBounds.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(arcBounds, 0f, FULL_CIRCLE, false, trackPaint)
        if (progress > 0f) {
            // -90 degrees starts the sweep at 12 o'clock instead of 3 o'clock.
            canvas.drawArc(arcBounds, START_ANGLE, FULL_CIRCLE * progress, false, progressPaint)
        }
    }

    private companion object {
        const val START_ANGLE = -90f
        const val FULL_CIRCLE = 360f
    }
}
