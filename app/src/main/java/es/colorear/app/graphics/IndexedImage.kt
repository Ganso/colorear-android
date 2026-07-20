package es.colorear.app.graphics

data class IndexedImage(
    val width: Int,
    val height: Int,
    val pixels: ByteArray,
    val paletteRgb8: ByteArray,
) {
    init {
        require(width > 0 && height > 0)
        require(pixels.size == width * height)
        require(paletteRgb8.size == VgaPalette.COMPONENT_COUNT)
    }
}

