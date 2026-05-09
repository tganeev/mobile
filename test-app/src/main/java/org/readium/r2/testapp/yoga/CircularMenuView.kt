package org.readium.r2.testapp.yoga

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.math.cos
import kotlin.math.sin

class CircularMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var items = listOf<CircularMenuItem>()
    private var centerTimerName = "Шаматха"
    private var centerTimerDuration = "31"
    private var centerTimerRemainingSeconds = 0L
    private var isCenterTimerRunning = false
    private var onCenterTimerClickListener: (() -> Unit)? = null
    private var onTimerClickListener: ((CircularMenuItem, Int) -> Unit)? = null

    // Для вращения мышкой
    private var rotationAngle = 0f
    private var lastTouchAngle = 0f
    private var isDragging = false
    private var draggedItemIndex = -1

    // Размеры
    private var radius = 0f
    private var centerRadius = 0f
    private var outerCircleRadius = 0f
    private var itemRadius = 0f      // Внешние круги
    private var centerItemRadius = 0f // Центральный круг

    // Эффект выпуклости
    private var pressedItemIndex = -1

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E1E1E")
        style = Paint.Style.FILL
    }

    private val centerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private val centerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textAlign = Paint.Align.CENTER
    }

    data class CircularMenuItem(
        val id: String,
        val name: String,
        val color: Int,
        val durationMinutes: Int,
        var remainingSeconds: Long = (durationMinutes * 60L),
        var isRunning: Boolean = false
    )

    fun setItems(items: List<CircularMenuItem>) {
        this.items = items
        invalidate()
    }

    fun updateItemRemainingTime(itemId: String, remainingSeconds: Long) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            items[index].remainingSeconds = remainingSeconds
            invalidate()
        }
    }

    fun setCenterTimer(name: String, durationMinutes: Int) {
        centerTimerName = name
        centerTimerDuration = durationMinutes.toString()
        centerTimerRemainingSeconds = durationMinutes * 60L
        invalidate()
    }

    fun updateCenterTimer(remainingSeconds: Long) {
        centerTimerRemainingSeconds = remainingSeconds
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        centerTimerDuration = String.format("%02d:%02d", minutes, seconds)
        invalidate()
    }

    fun setCenterTimerRunning(isRunning: Boolean) {
        isCenterTimerRunning = isRunning
        invalidate()
    }

    fun setOnCenterTimerClickListener(listener: () -> Unit) {
        onCenterTimerClickListener = listener
    }

    fun setOnTimerClickListener(listener: (CircularMenuItem, Int) -> Unit) {
        onTimerClickListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outerCircleRadius = min(width, height) / 2f * 0.85f
        radius = outerCircleRadius * 0.72f
        itemRadius = 130f      // Внешние круги увеличены до 130f
        centerItemRadius = 160f // Центральный круг увеличен до 160f
    }

    private fun createConvexGradient(color: Int, x: Float, y: Float, radius: Float): RadialGradient {
        return RadialGradient(
            x, y, radius,
            intArrayOf(
                ColorUtils.lighten(color, 0.4f),
                color,
                ColorUtils.darken(color, 0.25f)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun createConcaveGradient(color: Int, x: Float, y: Float, radius: Float): RadialGradient {
        return RadialGradient(
            x, y, radius,
            intArrayOf(
                ColorUtils.darken(color, 0.25f),
                color,
                ColorUtils.lighten(color, 0.2f)
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawCircle(centerX, centerY, outerCircleRadius, bgPaint)

        if (items.isNotEmpty()) {
            val angleStep = 2 * PI / items.size
            items.forEachIndexed { index, item ->
                val angle = (index * angleStep + Math.toRadians(rotationAngle.toDouble())).toFloat()
                val x = centerX + radius * cos(angle.toDouble()).toFloat()
                val y = centerY + radius * sin(angle.toDouble()).toFloat()

                val isPressed = pressedItemIndex == index
                val currentRadius = if (isPressed) itemRadius * 0.96f else itemRadius

                val gradient = if (isPressed) {
                    createConcaveGradient(item.color, x, y, currentRadius)
                } else {
                    createConvexGradient(item.color, x, y, currentRadius)
                }

                val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = gradient
                    style = Paint.Style.FILL
                    alpha = 230
                }
                canvas.drawCircle(x, y, currentRadius, circlePaint)

                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    alpha = 200
                }
                canvas.drawCircle(x, y, currentRadius, strokePaint)

                // Увеличенный шрифт для названия
                textPaint.textSize = 28f
                textPaint.alpha = 255
                textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                textPaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#80000000"))

                // Первая буква заглавная, остальные строчные
                val displayName = item.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }

                // Разбиваем длинные названия на две строки
                if (displayName.length > 10) {
                    val splitIndex = displayName.indexOf(' ', 6)
                    val firstLine = if (splitIndex > 0 && splitIndex < displayName.length - 3) {
                        displayName.substring(0, splitIndex)
                    } else {
                        displayName.substring(0, 9)
                    }
                    val secondLine = if (splitIndex > 0 && splitIndex < displayName.length - 3) {
                        displayName.substring(splitIndex + 1)
                    } else {
                        displayName.substring(9, min(displayName.length, 18))
                    }
                    canvas.drawText(firstLine, x, y - 28, textPaint)
                    canvas.drawText(secondLine, x, y + 2, textPaint)
                } else {
                    canvas.drawText(displayName, x, y - 28, textPaint)
                }

                // Время с увеличенным шрифтом
                subTextPaint.textSize = 30f
                subTextPaint.typeface = android.graphics.Typeface.MONOSPACE
                subTextPaint.alpha = 255
                subTextPaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#80000000"))
                subTextPaint.isFakeBoldText = true

                val minutes = item.remainingSeconds / 60
                val seconds = item.remainingSeconds % 60
                val timeText = if (item.isRunning) {
                    String.format("%02d:%02d", minutes, seconds)
                } else {
                    "${item.durationMinutes} мин"
                }
                canvas.drawText(timeText, x, y + 48, subTextPaint)
                subTextPaint.typeface = null

                // Индикатор активного таймера
                if (item.isRunning) {
                    val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#4CAF50")
                        style = Paint.Style.FILL
                        alpha = 255
                        setShadowLayer(12f, 0f, 0f, Color.parseColor("#804CAF50"))
                    }
                    canvas.drawCircle(x, y + 90, 18f, indicatorPaint)

                    val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#4CAF50")
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        alpha = 150
                    }
                    canvas.drawCircle(x, y + 90, 28f, pulsePaint)
                }
            }
        }

        // Центральный круг
        val centerGradient = RadialGradient(
            centerX, centerY - 5, centerItemRadius * 0.8f,
            intArrayOf(
                ColorUtils.lighten(Color.parseColor("#4CAF50"), 0.35f),
                Color.parseColor("#4CAF50"),
                ColorUtils.darken(Color.parseColor("#4CAF50"), 0.25f)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        centerBgPaint.shader = centerGradient
        canvas.drawCircle(centerX, centerY, centerItemRadius, centerBgPaint)
        canvas.drawCircle(centerX, centerY, centerItemRadius, centerStrokePaint)

        textPaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#80000000"))

        // Название центрального таймера
        textPaint.textSize = 24f
        textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(centerTimerName, centerX, centerY - 30, textPaint)

        // Время центрального таймера
        textPaint.textSize = 38f
        textPaint.typeface = android.graphics.Typeface.MONOSPACE
        canvas.drawText(centerTimerDuration, centerX, centerY + 25, textPaint)

        // Индикатор активности центра
        if (isCenterTimerRunning) {
            val runningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFEB3B")
                style = Paint.Style.STROKE
                strokeWidth = 6f
                alpha = 255
                setShadowLayer(10f, 0f, 0f, Color.parseColor("#80FFEB3B"))
            }
            canvas.drawCircle(centerX, centerY, centerItemRadius + 12, runningPaint)
        }

        textPaint.clearShadowLayer()
        subTextPaint.clearShadowLayer()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val centerX = width / 2f
        val centerY = height / 2f
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val distanceToCenter = hypot((touchX - centerX).toDouble(), (touchY - centerY).toDouble()).toFloat()
                if (distanceToCenter <= centerItemRadius + 20) {
                    onCenterTimerClickListener?.invoke()
                    return true
                }

                if (items.isNotEmpty()) {
                    val angleStep = 2 * PI / items.size
                    items.forEachIndexed { index, item ->
                        val angle = (index * angleStep + Math.toRadians(rotationAngle.toDouble())).toFloat()
                        val x = centerX + radius * cos(angle.toDouble()).toFloat()
                        val y = centerY + radius * sin(angle.toDouble()).toFloat()

                        val distanceToItem = hypot((touchX - x).toDouble(), (touchY - y).toDouble()).toFloat()
                        if (distanceToItem <= itemRadius + 20) {
                            pressedItemIndex = index
                            draggedItemIndex = index
                            invalidate()

                            val dx = touchX - centerX
                            val dy = touchY - centerY
                            lastTouchAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                            isDragging = true

                            onTimerClickListener?.invoke(item, index)
                            return true
                        }
                    }
                }

                isDragging = true
                val dx = touchX - centerX
                val dy = touchY - centerY
                lastTouchAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = touchX - centerX
                    val dy = touchY - centerY
                    val currentAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                    var delta = currentAngle - lastTouchAngle

                    while (delta > PI.toFloat()) delta -= (2 * PI).toFloat()
                    while (delta < -PI.toFloat()) delta += (2 * PI).toFloat()

                    rotationAngle += Math.toDegrees(delta.toDouble()).toFloat()
                    lastTouchAngle = currentAngle
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (pressedItemIndex != -1) {
                    pressedItemIndex = -1
                    invalidate()
                }
                draggedItemIndex = -1
                isDragging = false
            }
        }
        return super.onTouchEvent(event)
    }
}

object ColorUtils {
    fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1 - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1 - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1 - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}