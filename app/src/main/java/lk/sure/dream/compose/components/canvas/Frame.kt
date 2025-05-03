package lk.sure.dream.compose.components.canvas

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.get
import java.util.Collections

class Frame(private val canvasDrawSize: IntSize) {
    val layers = mutableStateListOf<Layer>()
    var activeLayerIndex by mutableIntStateOf(-1)

    private val finalImage by mutableStateOf(Layer(canvasDrawSize))
    val image get() = finalImage.asBitmap().also { activeLayerIndex }

    private val previewLayer by mutableStateOf(Layer(canvasDrawSize))
    var previewLayerVisible = false
    val layerCount get() = layers.size

    val allLayersSaved get() = layers.all { it.isSaved }

    fun saveAllLayers() {
        layers.forEach { it.saved() }
    }

    operator fun get(index: Int) = layers[index]

    fun lastLayer() = layers.last()

    fun forEachLayer(action: (Layer) -> Unit) = layers.forEach(action)

    fun anyLayer(predicate: (Layer) -> Boolean) = layers.any(predicate)

    operator fun iterator() = layers.iterator()

    fun addLayer(name: String) {
        layers.add(Layer(canvasDrawSize).also { it.name = it.name.copy(text = name) })
        activeLayerIndex = layerCount - 1
        pushWork()
        invalidateLayers()
    }

    fun removeLayer(layerIndex: Int) {
        if (activeLayerIndex >= layerIndex) {
            activeLayerIndex -= 1
        }
        layers.removeAt(layerIndex)
        invalidateLayers()
    }

    /** Returns active [Layer] of from the [Frame] */
    fun active() = layers[activeLayerIndex]

    fun setPixelForActiveLayer(x: Int, y: Int, color: Int) {
        if (active().setPixel(x, y, color))
            processFinalImageAt(x, y)
    }

    fun getPixelFormActiveLayer(x: Int, y: Int): Int {
        return active().getPixel(x, y)
    }

    private fun processFinalImageAt(x: Int, y: Int) {
        for (idx in layerCount - 1 downTo if (previewLayerVisible) -1 else 0) {

            val layer = when {
                !previewLayerVisible -> layers[idx]
                idx > activeLayerIndex -> layers[idx]
                idx == activeLayerIndex -> previewLayer
                else -> layers[idx + 1]
            }

            val color = layer.getPixel(x, y)
            if (layer.isVisible && color != EraseColor) {
                finalImage.setPixel(x, y, color)
                return
            }
        }

        finalImage.setPixel(x, y, EraseColor)
    }

    private fun processFinalImage() {
        for (y in 0..<canvasDrawSize.height) {
            for (x in 0..<canvasDrawSize.width) {
                processFinalImageAt(x, y)
            }
        }
    }

    fun setPixelForPreviewLayer(x: Int, y: Int, color: Int) {
        previewLayer.setPixel(x, y, color)
        processFinalImageAt(x, y)
    }

    fun copyToPreviewLayer(bitmap: Bitmap, offset: IntOffset = IntOffset.Zero) {
        previewLayer.mergePixels(bitmap, offset)
    }

    fun clearPreviewLayer() {
        previewLayer.clearAll()
        processFinalImage()
    }

    fun invalidateLayers() {
        for (idx in 0..<layers.size) {
            layers[idx].invalidate()
        }
        processFinalImage()
        previewLayer.invalidate()
        finalImage.invalidate()
    }

    fun invalidateFinalImage() {
        finalImage.invalidate()
    }

    fun pushWork() {
        active().pushWork()
    }

    fun undoWork() {
        active().undo()
        processFinalImage()
        invalidateLayers()
    }

    fun redoWork() {
        active().redo()
        processFinalImage()
        invalidateLayers()
    }

    fun canUndo() = active().canUndo()

    fun canRedo() = active().canRedo()

    fun moveLayer(from: Int, to: Int) {
        Collections.swap(layers, from, to)
        invalidateLayers()
    }

    fun mergeDown(layerIndex: Int) {
        require(layerIndex != 0)
        val image = layers[layerIndex].asBitmap()

        repeat(canvasDrawSize.height) { y ->
            repeat(canvasDrawSize.width) xLoop@{ x ->
                val color = image[x, y]
                if (color == EraseColor) return@xLoop
                layers[layerIndex - 1].setPixel(x, y, color)
            }
        }

        removeLayer(layerIndex)
    }
}