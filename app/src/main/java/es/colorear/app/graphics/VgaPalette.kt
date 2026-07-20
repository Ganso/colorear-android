package es.colorear.app.graphics

import kotlin.math.roundToInt

/** Mutable VGA DAC palette. Components are deliberately kept in their native 0..63 range. */
class VgaPalette private constructor(
    private val components: ByteArray,
) {
    init {
        require(components.size == COMPONENT_COUNT)
        require(components.all { (it.toInt() and 0xFF) <= MAX_COMPONENT })
    }

    fun copy(): VgaPalette = VgaPalette(components.copyOf())

    fun component(colorIndex: Int, channel: Int): Int {
        require(colorIndex in 0 until COLOR_COUNT)
        require(channel in 0..2)
        return components[colorIndex * 3 + channel].toInt() and 0xFF
    }

    fun setColor(colorIndex: Int, red: Int, green: Int, blue: Int) {
        require(colorIndex in 0 until COLOR_COUNT)
        val offset = colorIndex * 3
        components[offset] = red.coerceIn(0, MAX_COMPONENT).toByte()
        components[offset + 1] = green.coerceIn(0, MAX_COMPONENT).toByte()
        components[offset + 2] = blue.coerceIn(0, MAX_COMPONENT).toByte()
    }

    fun applyToneGroup(groupStart: Int, toneIndex: Int) {
        require(groupStart in 0..(COLOR_COUNT - SHADE_COUNT))
        require(toneIndex in 0 until COLOR_COUNT)
        val red = component(toneIndex, 0)
        val green = component(toneIndex, 1)
        val blue = component(toneIndex, 2)
        for (shade in 0 until SHADE_COUNT) {
            val delta = shade * SHADE_STEP
            setColor(groupStart + shade, red - delta, green - delta, blue - delta)
        }
    }

    fun groupMatchesTone(groupStart: Int, toneIndex: Int): Boolean {
        val red = component(toneIndex, 0)
        val green = component(toneIndex, 1)
        val blue = component(toneIndex, 2)
        return component(groupStart, 0) == red &&
            component(groupStart, 1) == green &&
            component(groupStart, 2) == blue &&
            component(groupStart + 1, 0) == (red - SHADE_STEP).coerceAtLeast(0) &&
            component(groupStart + 1, 1) == (green - SHADE_STEP).coerceAtLeast(0) &&
            component(groupStart + 1, 2) == (blue - SHADE_STEP).coerceAtLeast(0)
    }

    fun makeDrawingWhite() {
        for (index in DRAWING_FIRST..DRAWING_LAST) {
            setColor(index, MAX_COMPONENT, MAX_COMPONENT, MAX_COMPONENT)
        }
    }

    fun snapshotDrawing(): ByteArray = components.copyOfRange(
        DRAWING_FIRST * 3,
        (DRAWING_LAST + 1) * 3,
    )

    fun restoreDrawing(snapshot: ByteArray) {
        require(snapshot.size == DRAWING_COMPONENT_COUNT)
        snapshot.copyInto(components, destinationOffset = DRAWING_FIRST * 3)
    }

    fun toArgb(
        drawingDarken: Int = 0,
        brightness: Float = 1f,
        outlineOnly: Boolean = false,
    ): IntArray {
        val result = IntArray(COLOR_COUNT)
        val safeBrightness = brightness.coerceIn(0f, 1f)
        for (index in 0 until COLOR_COUNT) {
            if (outlineOnly) {
                result[index] = if (index == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                continue
            }
            val darken = if (index in DRAWING_FIRST..DRAWING_LAST) drawingDarken else 0
            val red = ((component(index, 0) - darken).coerceAtLeast(0) * safeBrightness).roundToInt()
            val green = ((component(index, 1) - darken).coerceAtLeast(0) * safeBrightness).roundToInt()
            val blue = ((component(index, 2) - darken).coerceAtLeast(0) * safeBrightness).roundToInt()
            result[index] = 0xFF000000.toInt() or
                (expandTo8Bit(red) shl 16) or
                (expandTo8Bit(green) shl 8) or
                expandTo8Bit(blue)
        }
        return result
    }

    companion object {
        const val COLOR_COUNT = 256
        const val COMPONENT_COUNT = COLOR_COUNT * 3
        const val MAX_COMPONENT = 63
        const val DRAWING_FIRST = 128
        const val DRAWING_LAST = 253
        const val SHADE_COUNT = 6
        const val SHADE_STEP = 4
        const val DRAWING_COMPONENT_COUNT = (DRAWING_LAST - DRAWING_FIRST + 1) * 3

        fun fromPcxPalette(rgb8: ByteArray): VgaPalette {
            require(rgb8.size == COMPONENT_COUNT)
            return VgaPalette(ByteArray(COMPONENT_COUNT) { offset ->
                ((rgb8[offset].toInt() and 0xFF) ushr 2).toByte()
            })
        }

        fun groupStartForPixel(pixelIndex: Int): Int? {
            if (pixelIndex !in DRAWING_FIRST..DRAWING_LAST) return null
            return pixelIndex - ((pixelIndex - 2) % SHADE_COUNT)
        }

        private fun expandTo8Bit(value: Int): Int = (value.coerceIn(0, MAX_COMPONENT) * 255 + 31) / 63
    }
}

