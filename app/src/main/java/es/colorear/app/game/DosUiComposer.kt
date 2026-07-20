package es.colorear.app.game

import es.colorear.app.graphics.IndexedCanvas
import kotlin.math.max

class DosUiComposer(
    private val assets: DosAssets,
    private val state: ColoringGameState,
) {
    val canvas = IndexedCanvas()

    fun compose(
        cursorX: Int,
        cursorY: Int,
        banner: String,
        includeCursor: Boolean,
        blink: Boolean,
    ): IndexedCanvas {
        when (state.screen) {
            ColoringGameState.Screen.PICKER -> composePicker(cursorX, cursorY, banner, includeCursor, blink)
            ColoringGameState.Screen.MENU -> composeMenu(cursorX, cursorY, banner, includeCursor, blink)
            ColoringGameState.Screen.PAINT -> composePainting(cursorX, cursorY, includeCursor)
            ColoringGameState.Screen.TRANSITION -> Unit
        }
        return canvas
    }

    fun composeTransition(revealedRows: Int): IndexedCanvas {
        canvas.fill(0)
        val image = state.activeDrawing.image
        val rows = revealedRows.coerceIn(0, IndexedCanvas.HEIGHT)
        image.pixels.copyInto(canvas.pixels, endIndex = rows * IndexedCanvas.WIDTH)
        return canvas
    }

    fun bannerAt(x: Int, y: Int): String = when (state.screen) {
        ColoringGameState.Screen.PICKER -> pickerBannerAt(x, y)
        ColoringGameState.Screen.MENU -> menuBannerAt(x, y)
        else -> ""
    }

    private fun composePicker(
        cursorX: Int,
        cursorY: Int,
        banner: String,
        includeCursor: Boolean,
        blink: Boolean,
    ) {
        canvas.fill(2)
        canvas.draw(assets.stop, 282, 151)
        canvas.draw(assets.filePanel, 102, 17)
        canvas.draw(assets.arrowUp, 190, 49)
        canvas.draw(assets.arrowDown, 190, 77)
        drawFileNames(114, 28)
        drawBottomBar(banner, cursorX, cursorY, blink)
        if (includeCursor) canvas.draw(assets.hand, cursorX, cursorY, transparentIndex = 31)
    }

    private fun composeMenu(
        cursorX: Int,
        cursorY: Int,
        banner: String,
        includeCursor: Boolean,
        blink: Boolean,
    ) {
        canvas.copyFrom(state.activeDrawing.image.pixels)
        canvas.draw(assets.colorChooser, 16, 3)
        canvas.draw(assets.stop, 282, 151)
        canvas.draw(assets.reset, 214, 147)
        canvas.draw(assets.filePanel, 197, 12)
        canvas.draw(assets.arrowUp, 285, 44)
        canvas.draw(assets.arrowDown, 285, 72)
        canvas.draw(assets.undo, 281, 118)
        drawFileNames(209, 23)
        drawBottomBar(banner, cursorX, cursorY, blink)
        if (includeCursor) canvas.draw(assets.hand, cursorX, cursorY, transparentIndex = 31)
    }

    private fun composePainting(cursorX: Int, cursorY: Int, includeCursor: Boolean) {
        canvas.copyFrom(state.activeDrawing.image.pixels)
        if (includeCursor) canvas.draw(assets.pencil, cursorX, cursorY, transparentIndex = 31)
    }

    private fun drawFileNames(x: Int, y: Int) {
        state.drawings.forEachIndexed { index, drawing ->
            drawText(drawing.name, x, y + index * 8)
        }
    }

    private fun drawBottomBar(message: String, cursorX: Int, cursorY: Int, blink: Boolean) {
        canvas.draw(assets.childrenAndBanner, 6, 158)
        canvas.fillRect(8, 186, 311, 191, 3)
        drawCenteredText(message, 185)
        drawEyes(cursorX, cursorY, blink)
    }

    private fun drawText(text: String, x: Int, y: Int) {
        text.forEachIndexed { position, original ->
            val character = if (original == ' ') '<' else original
            val code = character.code.coerceIn(
                DosAssets.FONT_FIRST_CHARACTER,
                DosAssets.FONT_LAST_CHARACTER,
            )
            canvas.draw(
                assets.font[code - DosAssets.FONT_FIRST_CHARACTER],
                x + position * 8,
                y,
                transparentIndex = 0,
            )
        }
    }

    private fun drawCenteredText(text: String, y: Int) {
        val fitted = if (text.length <= 38) text else text.take(38)
        drawText(fitted, max(0, (IndexedCanvas.WIDTH - fitted.length * 8) / 2), y)
    }

    private fun pickerBannerAt(x: Int, y: Int): String {
        val fileIndex = fileAt(x, y, 114, 28)
        if (fileIndex != null) return "PONER LA PANTALLA ${state.drawings[fileIndex].name}"
        return when {
            x in 190..206 && y in 49..59 -> "SUBE LA LISTA DE ARCHIVOS"
            x in 190..206 && y in 77..87 -> "BAJA LA LISTA DE ARCHIVOS"
            x in 282..313 && y in 151..179 -> "SALE DEL PROGRAMA"
            else -> PICKER_DEFAULT_BANNER
        }
    }

    private fun menuBannerAt(x: Int, y: Int): String {
        val fileIndex = fileAt(x, y, 209, 23)
        if (fileIndex != null) return "PONER LA PANTALLA ${state.drawings[fileIndex].name}"
        return when {
            x in 285..301 && y in 44..54 -> "SUBE LA LISTA DE ARCHIVOS"
            x in 285..301 && y in 72..82 -> "BAJA LA LISTA DE ARCHIVOS"
            x in 281..312 && y in 118..145 -> "DESHACE EL ULTIMO CAMBIO"
            x in 16..114 && y in 3..179 -> "ELIGE EL COLOR"
            x in 282..313 && y in 151..179 -> "SALE DEL PROGRAMA"
            x in 214..269 && y in 147..181 -> "QUITA LOS COLORES DE LA PANTALLA"
            else -> MENU_DEFAULT_BANNER
        }
    }

    private fun fileAt(x: Int, y: Int, left: Int, top: Int): Int? {
        if (x !in left..(left + 63) || y !in top..(top + 79)) return null
        val index = (y - top) / 8
        return index.takeIf { it in state.drawings.indices }
    }

    private fun drawEyes(x: Int, y: Int, blink: Boolean) {
        drawChildEyes(x, y, splitX = 146, upper = intArrayOf(53264, 53267), lower = intArrayOf(53584, 53587), blink)
        drawChildEyes(x, y, splitX = 185, upper = intArrayOf(53303, 53306), lower = intArrayOf(53623, 53626), blink)
    }

    private fun drawChildEyes(
        x: Int,
        y: Int,
        splitX: Int,
        upper: IntArray,
        lower: IntArray,
        blink: Boolean,
    ) {
        if (blink) {
            (upper + lower).forEach { offset ->
                setOffset(offset, 23)
                setOffset(offset + 1, 23)
            }
            return
        }

        val side = if (x < splitX) {
            if (y < 166) 0 else 1
        } else {
            if (y < 166) 3 else 2
        }
        upper.forEach { offset ->
            when (side) {
                0 -> setPair(offset, 11, 2)
                3 -> setPair(offset, 2, 11)
                else -> setPair(offset, 3, 3)
            }
        }
        lower.forEach { offset ->
            when (side) {
                1 -> setPair(offset, 11, 2)
                2 -> setPair(offset, 2, 11)
                else -> setPair(offset, 3, 3)
            }
        }
    }

    private fun setPair(offset: Int, first: Int, second: Int) {
        setOffset(offset, first)
        setOffset(offset + 1, second)
    }

    private fun setOffset(offset: Int, color: Int) {
        if (offset in canvas.pixels.indices) canvas.pixels[offset] = color.toByte()
    }

    companion object {
        const val PICKER_DEFAULT_BANNER = "ELIGE UN DIBUJO PARA EMPEZAR"
        const val MENU_DEFAULT_BANNER = "TOCA EL DIBUJO PARA COLOREAR"
    }
}
