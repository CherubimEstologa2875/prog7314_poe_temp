package com.example.runway.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.runway.R
import com.example.runway.domain.model.OutfitCanvas
import com.example.runway.domain.model.OutfitLayer
import com.google.android.material.color.MaterialColors
import kotlin.math.cos
import kotlin.math.sin

// Draws an outfit's garments as a flat-lay collage. Editable in the builder, read-only everywhere else.
class OutfitCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var layers: List<OutfitLayer> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    // Cut-out photos by item id. A missing one is drawn as a plain placeholder.
    var images: Map<String, Bitmap?> = emptyMap()
        set(value) {
            field = value
            invalidate()
        }

    var selectedId: String? = null
        set(value) {
            field = value
            invalidate()
        }

    // Off for thumbnails, so a scrolling list isn't grabbing touches.
    var editable: Boolean = false

    private var onSelect: ((String?) -> Unit)? = null
    private var onMove: ((String, Float, Float) -> Unit)? = null

    private val radius = resources.getDimension(R.dimen.rw_radius_card)
    private val stroke = resources.getDimension(R.dimen.rw_border_width)

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(this@OutfitCanvasView, R.attr.rwSurface)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = MaterialColors.getColor(this@OutfitCanvasView, com.google.android.material.R.attr.colorOutline)
    }
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(this@OutfitCanvasView, R.attr.rwElevated)
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke * 2
        pathEffect = DashPathEffect(floatArrayOf(stroke * 6, stroke * 4), 0f)
        color = MaterialColors.getColor(this@OutfitCanvasView, androidx.appcompat.R.attr.colorPrimary)
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val bounds = RectF()
    private val clip = Path()
    private val garment = RectF()

    // Where the finger landed relative to the garment's centre, so it doesn't jump when dragged.
    private var dragId: String? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    fun onLayerSelected(listener: (String?) -> Unit) {
        onSelect = listener
    }

    fun onLayerMoved(listener: (itemId: String, x: Float, y: Float) -> Unit) {
        onMove = listener
    }

    // Always 4:5, like the mockup, whatever width it's given.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = width * 5 / 4
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        bounds.set(stroke / 2, stroke / 2, w - stroke / 2, h - stroke / 2)
        clip.reset()
        clip.addRoundRect(bounds, radius, radius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(bounds, radius, radius, backgroundPaint)

        canvas.save()
        canvas.clipPath(clip)
        OutfitCanvas.drawOrder(layers).forEach { drawLayer(canvas, it) }
        canvas.restore()

        canvas.drawRoundRect(bounds, radius, radius, borderPaint)
    }

    private fun drawLayer(canvas: Canvas, layer: OutfitLayer) {
        sizeOf(layer)
        canvas.save()
        canvas.translate(layer.x * width, layer.y * height)
        canvas.rotate(layer.rotation)

        val bitmap = images[layer.itemId]
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, garment, bitmapPaint)
        } else {
            canvas.drawRoundRect(garment, radius / 2, radius / 2, placeholderPaint)
            canvas.drawRoundRect(garment, radius / 2, radius / 2, borderPaint)
        }

        if (layer.itemId == selectedId) canvas.drawRect(garment, selectionPaint)
        canvas.restore()
    }

    // Fills `garment` with the layer's rectangle, centred on 0,0.
    private fun sizeOf(layer: OutfitLayer) {
        val w = width * GARMENT_WIDTH * layer.scale
        val bitmap = images[layer.itemId]
        val aspect = if (bitmap != null && bitmap.width > 0) bitmap.height.toFloat() / bitmap.width else 1.15f
        val h = w * aspect
        garment.set(-w / 2, -h / 2, w / 2, h / 2)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editable || width == 0) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = layerAt(event.x, event.y)
                dragId = hit?.itemId
                if (hit != null) {
                    dragOffsetX = event.x / width - hit.x
                    dragOffsetY = event.y / height - hit.y
                    // Keeps a parent scroll view from stealing the drag.
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                onSelect?.invoke(hit?.itemId)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val id = dragId ?: return true
                onMove?.invoke(id, event.x / width - dragOffsetX, event.y / height - dragOffsetY)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_UP && dragId == null) performClick()
                dragId = null
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    // Checks from the top layer down, undoing each garment's rotation before the bounds check.
    private fun layerAt(x: Float, y: Float): OutfitLayer? =
        OutfitCanvas.drawOrder(layers).asReversed().firstOrNull { layer ->
            sizeOf(layer)
            val dx = x - layer.x * width
            val dy = y - layer.y * height
            val angle = Math.toRadians(-layer.rotation.toDouble())
            val localX = (dx * cos(angle) - dy * sin(angle)).toFloat()
            val localY = (dx * sin(angle) + dy * cos(angle)).toFloat()
            garment.contains(localX, localY)
        }

    private companion object {
        // A garment at scale 1 is just under half the canvas wide.
        const val GARMENT_WIDTH = 0.46f
    }
}
