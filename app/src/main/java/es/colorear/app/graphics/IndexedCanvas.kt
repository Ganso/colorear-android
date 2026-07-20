package es.colorear.app.graphics

class IndexedCanvas(
    val width: Int = WIDTH,
    val height: Int = HEIGHT,
) {
    val pixels = ByteArray(width * height)

    fun fill(colorIndex: Int) {
        pixels.fill(colorIndex.toByte())
    }

    fun copyFrom(source: ByteArray) {
        require(source.size == pixels.size)
        source.copyInto(pixels)
    }

    fun get(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        return pixels[y * width + x].toInt() and 0xFF
    }

    fun set(x: Int, y: Int, colorIndex: Int) {
        if (x in 0 until width && y in 0 until height) {
            pixels[y * width + x] = colorIndex.toByte()
        }
    }

    fun fillRect(left: Int, top: Int, right: Int, bottom: Int, colorIndex: Int) {
        val clippedLeft = left.coerceAtLeast(0)
        val clippedTop = top.coerceAtLeast(0)
        val clippedRight = right.coerceAtMost(width - 1)
        val clippedBottom = bottom.coerceAtMost(height - 1)
        if (clippedLeft > clippedRight || clippedTop > clippedBottom) return
        for (y in clippedTop..clippedBottom) {
            pixels.fill(
                colorIndex.toByte(),
                y * width + clippedLeft,
                y * width + clippedRight + 1,
            )
        }
    }

    fun draw(sprite: IndexedSprite, x: Int, y: Int, transparentIndex: Int? = 255) {
        for (sourceY in 0 until sprite.height) {
            val destinationY = y + sourceY
            if (destinationY !in 0 until height) continue
            for (sourceX in 0 until sprite.width) {
                val destinationX = x + sourceX
                if (destinationX !in 0 until width) continue
                val value = sprite.pixels[sourceY * sprite.width + sourceX].toInt() and 0xFF
                if (transparentIndex == null || value != transparentIndex) {
                    set(destinationX, destinationY, value)
                }
            }
        }
    }

    companion object {
        const val WIDTH = 320
        const val HEIGHT = 200
    }
}

data class IndexedSprite(
    val width: Int,
    val height: Int,
    val pixels: ByteArray,
) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height)
    }

    companion object {
        fun extract(source: IndexedImage, x: Int, y: Int, width: Int, height: Int): IndexedSprite {
            require(x >= 0 && y >= 0 && x + width <= source.width && y + height <= source.height)
            val result = ByteArray(width * height)
            for (row in 0 until height) {
                source.pixels.copyInto(
                    result,
                    destinationOffset = row * width,
                    startIndex = (y + row) * source.width + x,
                    endIndex = (y + row) * source.width + x + width,
                )
            }
            return IndexedSprite(width, height, result)
        }
    }
}
