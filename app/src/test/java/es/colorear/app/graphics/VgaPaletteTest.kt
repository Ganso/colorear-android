package es.colorear.app.graphics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VgaPaletteTest {
    @Test
    fun recoloringBuildsTheSixOriginalShadeSteps() {
        val rgb8 = ByteArray(VgaPalette.COMPONENT_COUNT)
        setRgb8(rgb8, 65, 252, 160, 80)
        val palette = VgaPalette.fromPcxPalette(rgb8)

        palette.applyToneGroup(128, 65)

        assertEquals(63, palette.component(128, 0))
        assertEquals(40, palette.component(128, 1))
        assertEquals(20, palette.component(128, 2))
        assertEquals(59, palette.component(129, 0))
        assertEquals(36, palette.component(129, 1))
        assertEquals(16, palette.component(129, 2))
        assertEquals(43, palette.component(133, 0))
        assertEquals(20, palette.component(133, 1))
        assertEquals(0, palette.component(133, 2))
    }

    @Test
    fun pixelIndicesMapToTwentyOneGroupsOfSix() {
        assertEquals(128, VgaPalette.groupStartForPixel(128))
        assertEquals(128, VgaPalette.groupStartForPixel(133))
        assertEquals(134, VgaPalette.groupStartForPixel(134))
        assertEquals(248, VgaPalette.groupStartForPixel(253))
        assertNull(VgaPalette.groupStartForPixel(127))
        assertNull(VgaPalette.groupStartForPixel(254))
    }

    private fun setRgb8(target: ByteArray, index: Int, red: Int, green: Int, blue: Int) {
        target[index * 3] = red.toByte()
        target[index * 3 + 1] = green.toByte()
        target[index * 3 + 2] = blue.toByte()
    }
}
