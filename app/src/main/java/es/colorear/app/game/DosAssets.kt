package es.colorear.app.game

import android.content.Context
import es.colorear.app.graphics.IndexedImage
import es.colorear.app.graphics.IndexedSprite
import es.colorear.app.graphics.PcxDecoder
import es.colorear.app.graphics.VgaPalette

class DosAssets(context: Context) {
    private val assetManager = context.assets
    private val colorsSheet = loadPcx("COLORES.PCX")
    private val menusSheet = loadPcx("MENUS.PCX")

    val masterPalette: VgaPalette = VgaPalette.fromPcxPalette(colorsSheet.paletteRgb8)

    val drawings: List<Drawing> = listOf(
        "GAFOTAS",
        "MARCIANO",
        "PAPANOEL",
        "PAYASO",
        "PECES",
    ).map { name -> Drawing(name, loadPcx("$name.PCX")) }

    val pencil = colorsSheet.sprite(1, 1, 16, 16)
    val hand = colorsSheet.sprite(26, 1, 16, 16)
    val childrenAndBanner = colorsSheet.sprite(6, 158, 308, 36)
    val font: List<IndexedSprite> = List(FONT_GLYPH_COUNT) { glyph ->
        val ordinal = glyph + FONT_FIRST_CHARACTER
        val sheetPosition = glyph
        val x = if (sheetPosition < 25) 120 + sheetPosition * 8 else ((sheetPosition - 25) % 40) * 8
        val y = if (sheetPosition < 25) 19 else 27 + ((sheetPosition - 25) / 40) * 8
        colorsSheet.sprite(x, y, 8, 8).also {
            check(ordinal in FONT_FIRST_CHARACTER..FONT_LAST_CHARACTER)
        }
    }

    val colorChooser = menusSheet.sprite(16, 3, 99, 177)
    val stop = menusSheet.sprite(282, 151, 32, 29)
    val reset = menusSheet.sprite(214, 147, 56, 35)
    val filePanel = menusSheet.sprite(197, 12, 115, 102)
    val arrowUp = menusSheet.sprite(134, 44, 17, 11)
    val arrowDown = menusSheet.sprite(155, 43, 17, 11)
    val undo = menusSheet.sprite(281, 118, 32, 28)

    private fun loadPcx(name: String): IndexedImage = assetManager
        .open("dos/$name")
        .use { PcxDecoder.decode(it.readBytes()) }

    private fun IndexedImage.sprite(x: Int, y: Int, width: Int, height: Int): IndexedSprite =
        IndexedSprite.extract(this, x, y, width, height)

    data class Drawing(val name: String, val image: IndexedImage)

    companion object {
        const val FONT_FIRST_CHARACTER = 48
        const val FONT_LAST_CHARACTER = 122
        const val FONT_GLYPH_COUNT = FONT_LAST_CHARACTER - FONT_FIRST_CHARACTER + 1
    }
}

