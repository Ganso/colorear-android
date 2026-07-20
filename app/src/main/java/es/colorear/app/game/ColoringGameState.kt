package es.colorear.app.game

import es.colorear.app.graphics.VgaPalette

class ColoringGameState(
    private val masterPalette: VgaPalette,
    val drawings: List<DosAssets.Drawing>,
) {
    enum class Screen {
        PICKER,
        TRANSITION,
        MENU,
        PAINT,
    }

    var screen: Screen = Screen.PICKER
        private set
    var activeDrawingIndex: Int = 0
        private set
    var selectedTone: Int = DEFAULT_TONE
        private set
    var transitionStartedAt: Long = 0L
        private set
    var menuOpenedAt: Long = 0L
        private set

    var palette: VgaPalette = freshPalette()
        private set

    private var previousDrawingPalette: ByteArray? = null

    val activeDrawing: DosAssets.Drawing
        get() = drawings[activeDrawingIndex]

    val canUndo: Boolean
        get() = previousDrawingPalette != null

    fun selectDrawing(index: Int, now: Long, animate: Boolean = true) {
        require(index in drawings.indices)
        activeDrawingIndex = index
        palette = freshPalette()
        selectedTone = DEFAULT_TONE
        palette.applyToneGroup(CURSOR_SLEEVE_GROUP, selectedTone)
        previousDrawingPalette = null
        if (animate) {
            transitionStartedAt = now
            screen = Screen.TRANSITION
        } else {
            openMenu(now)
        }
    }

    fun finishTransition(now: Long) {
        if (screen == Screen.TRANSITION) openMenu(now)
    }

    fun openMenu(now: Long) {
        screen = Screen.MENU
        menuOpenedAt = now
    }

    fun startPainting() {
        if (screen == Screen.MENU) screen = Screen.PAINT
    }

    fun returnToPicker() {
        screen = Screen.PICKER
        palette = freshPalette()
        selectedTone = DEFAULT_TONE
        previousDrawingPalette = null
    }

    fun chooseTone(toneIndex: Int) {
        if (toneIndex !in 64..95) return
        selectedTone = toneIndex
        palette.applyToneGroup(CURSOR_SLEEVE_GROUP, toneIndex)
    }

    fun recolorPixel(pixelIndex: Int): Boolean {
        val groupStart = VgaPalette.groupStartForPixel(pixelIndex) ?: return false
        if (palette.groupMatchesTone(groupStart, selectedTone)) return false
        previousDrawingPalette = palette.snapshotDrawing()
        palette.applyToneGroup(groupStart, selectedTone)
        return true
    }

    fun resetDrawing(): Boolean {
        previousDrawingPalette = palette.snapshotDrawing()
        palette.makeDrawingWhite()
        return true
    }

    fun undo(): Boolean {
        val previous = previousDrawingPalette ?: return false
        val current = palette.snapshotDrawing()
        palette.restoreDrawing(previous)
        previousDrawingPalette = current
        return true
    }

    private fun freshPalette(): VgaPalette = masterPalette.copy()

    companion object {
        const val DEFAULT_TONE = 65
        const val CURSOR_SLEEVE_GROUP = 40
    }
}
