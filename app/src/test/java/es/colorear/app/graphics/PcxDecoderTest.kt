package es.colorear.app.graphics

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcxDecoderTest {
    @Test
    fun decodesEveryPcxAssetFromTheDosGame() {
        val names = listOf(
            "COLORES.PCX",
            "MENUS.PCX",
            "GAFOTAS.PCX",
            "MARCIANO.PCX",
            "PAPANOEL.PCX",
            "PAYASO.PCX",
            "PECES.PCX",
        )

        names.forEach { name ->
            val bytes = checkNotNull(javaClass.classLoader?.getResourceAsStream("dos/$name")) {
                "Missing test asset $name"
            }.use { it.readBytes() }
            val image = PcxDecoder.decode(bytes)
            assertEquals("width of $name", 320, image.width)
            assertEquals("height of $name", 200, image.height)
            assertEquals("pixel count of $name", 64_000, image.pixels.size)
        }
    }

    @Test
    fun decodesLiteralAndRunLengthPixels() {
        val data = ByteArray(128 + 4 + 1 + 768)
        data[0] = 0x0A
        data[2] = 1
        data[3] = 8
        putU16(data, 4, 0)
        putU16(data, 6, 0)
        putU16(data, 8, 3)
        putU16(data, 10, 0)
        data[65] = 1
        putU16(data, 66, 4)

        data[128] = 1
        data[129] = 2
        data[130] = 0xC2.toByte()
        data[131] = 3
        data[132] = 0x0C
        for (index in 0 until 768) data[133 + index] = (index and 0xFF).toByte()

        val image = PcxDecoder.decode(data)

        assertEquals(4, image.width)
        assertEquals(1, image.height)
        assertArrayEquals(byteArrayOf(1, 2, 3, 3), image.pixels)
        assertEquals(768, image.paletteRgb8.size)
    }

    private fun putU16(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = (value ushr 8).toByte()
    }
}
