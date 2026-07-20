package es.colorear.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import es.colorear.app.game.ColoringGameState
import es.colorear.app.game.DosAssets
import es.colorear.app.game.DosUiComposer
import es.colorear.app.graphics.IndexedCanvas
import kotlin.math.abs
import kotlin.math.min

@SuppressLint("ViewConstructor")
class ColoringGameView(
    context: Context,
    private val onExit: () -> Unit,
) : View(context) {
    private val assets = DosAssets(context)
    private val state = ColoringGameState(assets.masterPalette, assets.drawings)
    private val composer = DosUiComposer(assets, state)
    private val bitmap = Bitmap.createBitmap(
        IndexedCanvas.WIDTH,
        IndexedCanvas.HEIGHT,
        Bitmap.Config.ARGB_8888,
    ).apply { density = Bitmap.DENSITY_NONE }
    private val argbPixels = IntArray(IndexedCanvas.WIDTH * IndexedCanvas.HEIGHT)
    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isDither = false
        isFilterBitmap = false
    }
    private val bitmapMatrix = Matrix()
    private val hintScrimPaint = Paint().apply { color = 0x66000000 }
    private val hintBackgroundPaint = Paint().apply {
        color = 0xF5001018.toInt()
        style = Paint.Style.FILL
    }
    private val hintBorderPaint = Paint().apply {
        color = 0xFF00FFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        isAntiAlias = true
    }
    private val hintTitlePaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val hintGesturePaint = Paint().apply {
        color = 0xFFFFFF00.toInt()
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val hintBodyPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private var cursorX = 160
    private var cursorY = 100
    private var banner = DosUiComposer.PICKER_DEFAULT_BANNER
    private var downAt = 0L
    private var downX = 0
    private var downY = 0
    private var gestureConsumed = false
    private var paintHintUntil = 0L
    private var paintHintUnskippableUntil = 0L
    private var hintShownForCurrentDrawing = false

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        contentDescription = "Juego Colorear"
    }

    override fun onDraw(androidCanvas: Canvas) {
        super.onDraw(androidCanvas)
        val now = SystemClock.uptimeMillis()
        val indexed = when (state.screen) {
            ColoringGameState.Screen.TRANSITION -> drawTransition(now)
            else -> composer.compose(
                cursorX,
                cursorY,
                banner,
                includeCursor = true,
                blink = isBlinking(now),
            )
        }
        val paletteLookup = when (state.screen) {
            ColoringGameState.Screen.TRANSITION -> transitionPalette(now)
            ColoringGameState.Screen.MENU -> state.palette.toArgb(drawingDarken = menuDarken(now))
            else -> state.palette.toArgb()
        }
        indexed.pixels.forEachIndexed { index, value ->
            argbPixels[index] = paletteLookup[value.toInt() and 0xFF]
        }
        bitmap.setPixels(
            argbPixels,
            0,
            IndexedCanvas.WIDTH,
            0,
            0,
            IndexedCanvas.WIDTH,
            IndexedCanvas.HEIGHT,
        )

        androidCanvas.drawColor(0xFF000000.toInt())
        val transform = fitTransform()
        bitmapMatrix.reset()
        bitmapMatrix.setScale(transform.scale, transform.scale)
        bitmapMatrix.postTranslate(transform.originX, transform.originY)
        androidCanvas.drawBitmap(bitmap, bitmapMatrix, bitmapPaint)
        if (state.screen == ColoringGameState.Screen.PAINT && now < paintHintUntil) {
            drawPaintHint(androidCanvas)
        }

        if (state.screen != ColoringGameState.Screen.PAINT || now < paintHintUntil) {
            postInvalidateDelayed(80L)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN &&
            event.buttonState and MotionEvent.BUTTON_SECONDARY != 0
        ) {
            updateTouchCursor(event)
            toggleMenu()
            gestureConsumed = true
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                updateTouchCursor(event)
                downAt = SystemClock.uptimeMillis()
                downX = cursorX
                downY = cursorY
                gestureConsumed = false
                updateBanner()
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    updateTouchCursor(event)
                    updateBanner()
                }
                invalidate()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    toggleMenu()
                    gestureConsumed = true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!gestureConsumed) updateTouchCursor(event)
                val heldFor = SystemClock.uptimeMillis() - downAt
                val moved = abs(cursorX - downX) + abs(cursorY - downY)
                if (!gestureConsumed) {
                    val now = SystemClock.uptimeMillis()
                    if (state.screen == ColoringGameState.Screen.PAINT &&
                        now < paintHintUntil
                    ) {
                        if (now >= paintHintUnskippableUntil) {
                            paintHintUntil = 0L
                        }
                    } else if (heldFor >= LONG_PRESS_MILLIS && moved <= LONG_PRESS_SLOP) {
                        toggleMenu()
                    } else {
                        handleTap(cursorX, cursorY)
                        performClick()
                    }
                }
                gestureConsumed = false
                updateBanner()
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                gestureConsumed = false
            }
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_HOVER_MOVE &&
            event.source and InputDevice.SOURCE_CLASS_POINTER != 0
        ) {
            updateCursor(event.x, event.y, clampToCanvas = false)
            updateBanner()
            invalidate()
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun handleBack(): Boolean = when (state.screen) {
        ColoringGameState.Screen.PICKER -> false
        ColoringGameState.Screen.TRANSITION,
        ColoringGameState.Screen.MENU,
        ColoringGameState.Screen.PAINT -> {
            state.returnToPicker()
            paintHintUntil = 0L
            banner = DosUiComposer.PICKER_DEFAULT_BANNER
            invalidate()
            true
        }
    }

    private fun handleTap(x: Int, y: Int) {
        when (state.screen) {
            ColoringGameState.Screen.PICKER -> handlePickerTap(x, y)
            ColoringGameState.Screen.MENU -> handleMenuTap(x, y)
            ColoringGameState.Screen.PAINT -> {
                val pixel = state.activeDrawing.image.pixels[y * IndexedCanvas.WIDTH + x].toInt() and 0xFF
                state.recolorPixel(pixel)
            }
            ColoringGameState.Screen.TRANSITION -> Unit
        }
    }

    private fun handlePickerTap(x: Int, y: Int) {
        if (x in 282..313 && y in 151..179) {
            onExit()
            return
        }
        val file = fileAt(x, y, 114, 28)
        if (file != null) {
            hintShownForCurrentDrawing = false
            state.selectDrawing(file, SystemClock.uptimeMillis())
            banner = DosUiComposer.MENU_DEFAULT_BANNER
        }
    }

    private fun handleMenuTap(x: Int, y: Int) {
        val file = fileAt(x, y, 209, 23)
        if (file != null) {
            hintShownForCurrentDrawing = false
            state.selectDrawing(file, SystemClock.uptimeMillis())
            return
        }

        composer.compose(x, y, banner, includeCursor = false, blink = false)
        val colorIndex = composer.canvas.get(x, y)
        when {
            x in 282..313 && y in 151..179 -> onExit()
            x in 281..312 && y in 118..145 -> state.undo()
            x in 214..269 && y in 147..181 -> state.resetDrawing()
            colorIndex in 64..95 -> state.chooseTone(colorIndex)
            colorIndex in 48..63 -> Unit
            else -> {
                state.startPainting()
                checkAndShowHint()
            }
        }
    }

    private fun toggleMenu() {
        when (state.screen) {
            ColoringGameState.Screen.MENU -> {
                state.startPainting()
                checkAndShowHint()
            }
            ColoringGameState.Screen.PAINT -> state.openMenu(SystemClock.uptimeMillis())
            else -> Unit
        }
        if (state.screen != ColoringGameState.Screen.PAINT) paintHintUntil = 0L
        banner = if (state.screen == ColoringGameState.Screen.MENU) {
            DosUiComposer.MENU_DEFAULT_BANNER
        } else {
            ""
        }
        invalidate()
    }

    private fun checkAndShowHint() {
        if (!hintShownForCurrentDrawing) {
            hintShownForCurrentDrawing = true
            val now = SystemClock.uptimeMillis()
            paintHintUntil = now + PAINT_HINT_MILLIS
            paintHintUnskippableUntil = now + 1000L
        }
    }

    private fun updateBanner() {
        banner = composer.bannerAt(cursorX, cursorY)
    }

    private fun drawTransition(now: Long): IndexedCanvas {
        val elapsed = now - state.transitionStartedAt
        if (elapsed >= TRANSITION_TOTAL_MILLIS) {
            state.finishTransition(now)
            banner = DosUiComposer.MENU_DEFAULT_BANNER
            return composer.compose(cursorX, cursorY, banner, includeCursor = true, blink = false)
        }
        return if (elapsed < FADE_MILLIS) {
            composer.compose(cursorX, cursorY, DosUiComposer.PICKER_DEFAULT_BANNER, includeCursor = false, blink = false)
        } else {
            val revealFraction = (elapsed - FADE_MILLIS).toFloat() / REVEAL_MILLIS
            composer.composeTransition((revealFraction * IndexedCanvas.HEIGHT).toInt())
        }
    }

    private fun transitionPalette(now: Long): IntArray {
        val elapsed = now - state.transitionStartedAt
        return if (elapsed < FADE_MILLIS) {
            state.palette.toArgb(brightness = 1f - elapsed.toFloat() / FADE_MILLIS)
        } else {
            state.palette.toArgb(outlineOnly = true)
        }
    }

    private fun menuDarken(now: Long): Int {
        val elapsed = (now - state.menuOpenedAt).coerceAtLeast(0L)
        return ((elapsed.coerceAtMost(MENU_DARKEN_MILLIS) * 20) / MENU_DARKEN_MILLIS).toInt()
    }

    private fun isBlinking(now: Long): Boolean = now % BLINK_PERIOD_MILLIS < BLINK_DURATION_MILLIS

    private fun drawPaintHint(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val boxWidth = (width * 0.78f).coerceAtMost(760f * density)
        val maximumHeight = height * 0.72f
        val minimumHeight = (190f * density).coerceAtMost(maximumHeight)
        val boxHeight = (height * 0.46f).coerceIn(minimumHeight, maximumHeight)
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f
        val box = RectF(left, top, left + boxWidth, top + boxHeight)
        val corner = 14f * density

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), hintScrimPaint)
        canvas.drawRoundRect(box, corner, corner, hintBackgroundPaint)
        canvas.drawRoundRect(box, corner, corner, hintBorderPaint)

        hintTitlePaint.textSize = boxHeight * 0.16f
        hintGesturePaint.textSize = boxHeight * 0.14f
        hintBodyPaint.textSize = boxHeight * 0.10f
        val centerX = box.centerX()
        canvas.drawText("RECUERDA", centerX, top + boxHeight * 0.25f, hintTitlePaint)
        canvas.drawText("TOCA CON DOS DEDOS", centerX, top + boxHeight * 0.50f, hintGesturePaint)
        canvas.drawText("para volver a la paleta", centerX, top + boxHeight * 0.70f, hintBodyPaint)
        canvas.drawText("de colores", centerX, top + boxHeight * 0.84f, hintBodyPaint)
    }

    private fun fileAt(x: Int, y: Int, left: Int, top: Int): Int? {
        if (x !in left..(left + 63) || y !in top..(top + 79)) return null
        return ((y - top) / 8).takeIf { it in state.drawings.indices }
    }

    private fun fitTransform(): ViewTransform {
        val scale = min(width / IndexedCanvas.WIDTH.toFloat(), height / IndexedCanvas.HEIGHT.toFloat())
        return ViewTransform(
            scale = scale,
            originX = (width - IndexedCanvas.WIDTH * scale) / 2f,
            originY = (height - IndexedCanvas.HEIGHT * scale) / 2f,
        )
    }

    private fun updateTouchCursor(event: MotionEvent) {
        val isFinger = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
        val verticalOffset = if (isFinger) TOUCH_CURSOR_OFFSET_DP * resources.displayMetrics.density else 0f
        updateCursor(event.getX(0), event.getY(0) - verticalOffset, clampToCanvas = isFinger)
    }

    private fun updateCursor(screenX: Float, screenY: Float, clampToCanvas: Boolean) {
        val transform = fitTransform()
        var logicalX = (screenX - transform.originX) / transform.scale
        var logicalY = (screenY - transform.originY) / transform.scale
        if (clampToCanvas) {
            logicalX = logicalX.coerceIn(0f, IndexedCanvas.WIDTH - 0.001f)
            logicalY = logicalY.coerceIn(0f, IndexedCanvas.HEIGHT - 0.001f)
        } else if (logicalX < 0f || logicalX >= IndexedCanvas.WIDTH ||
            logicalY < 0f || logicalY >= IndexedCanvas.HEIGHT
        ) {
            return
        }
        cursorX = logicalX.toInt()
        cursorY = logicalY.toInt()
    }

    private data class ViewTransform(val scale: Float, val originX: Float, val originY: Float)

    companion object {
        private const val LONG_PRESS_MILLIS = 500L
        private const val LONG_PRESS_SLOP = 8
        private const val TOUCH_CURSOR_OFFSET_DP = 0f
        private const val PAINT_HINT_MILLIS = 4_000L
        private const val FADE_MILLIS = 300L
        private const val REVEAL_MILLIS = 600L
        private const val TRANSITION_TOTAL_MILLIS = FADE_MILLIS + REVEAL_MILLIS
        private const val MENU_DARKEN_MILLIS = 300L
        private const val BLINK_PERIOD_MILLIS = 3_500L
        private const val BLINK_DURATION_MILLIS = 140L
        private const val PREFERENCES_NAME = "colorear_preferences"
        private const val PREFERENCE_PAINT_HINT_SHOWN = "paint_hint_v2_shown"
    }
}
