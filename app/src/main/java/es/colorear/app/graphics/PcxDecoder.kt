package es.colorear.app.graphics

/** Decoder for the 8-bit, one-plane PCX files used by the DOS game. */
object PcxDecoder {
    private const val HEADER_SIZE = 128
    private const val PALETTE_SIZE = 768
    private const val PALETTE_MARKER_SIZE = 1

    fun decode(data: ByteArray): IndexedImage {
        require(data.size >= HEADER_SIZE + PALETTE_MARKER_SIZE + PALETTE_SIZE) {
            "PCX file is too short"
        }
        require(data.u8(0) == 0x0A) { "Unsupported PCX manufacturer" }
        require(data.u8(2) == 1) { "Only PCX RLE encoding is supported" }
        require(data.u8(3) == 8) { "Only 8-bit PCX images are supported" }
        require(data.u8(65) == 1) { "Only one-plane PCX images are supported" }

        val xMin = data.u16(4)
        val yMin = data.u16(6)
        val xMax = data.u16(8)
        val yMax = data.u16(10)
        val width = xMax - xMin + 1
        val height = yMax - yMin + 1
        val bytesPerLine = data.u16(66)
        require(width > 0 && height > 0 && bytesPerLine >= width) {
            "Invalid PCX dimensions"
        }

        val markerOffset = data.size - PALETTE_SIZE - PALETTE_MARKER_SIZE
        require(data.u8(markerOffset) == 0x0C) { "PCX VGA palette marker is missing" }
        val palette = data.copyOfRange(markerOffset + 1, data.size)
        val pixels = ByteArray(width * height)
        val scanLine = ByteArray(bytesPerLine)
        var input = HEADER_SIZE

        for (y in 0 until height) {
            var output = 0
            while (output < bytesPerLine) {
                require(input < markerOffset) { "Truncated PCX image payload" }
                val token = data.u8(input++)
                val count: Int
                val value: Byte
                if (token and 0xC0 == 0xC0) {
                    count = token and 0x3F
                    require(count > 0 && input < markerOffset) { "Invalid PCX RLE run" }
                    value = data[input++]
                } else {
                    count = 1
                    value = token.toByte()
                }
                require(output + count <= bytesPerLine) { "PCX RLE run crosses a scan line" }
                scanLine.fill(value, output, output + count)
                output += count
            }
            scanLine.copyInto(pixels, destinationOffset = y * width, endIndex = width)
        }

        return IndexedImage(width, height, pixels, palette)
    }

    private fun ByteArray.u8(offset: Int): Int = this[offset].toInt() and 0xFF

    private fun ByteArray.u16(offset: Int): Int = u8(offset) or (u8(offset + 1) shl 8)
}

