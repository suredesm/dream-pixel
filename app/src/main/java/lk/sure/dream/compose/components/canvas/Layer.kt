package lk.sure.dream.compose.components.canvas

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import androidx.core.graphics.get
import kotlin.math.max
import kotlin.math.min

class Layer(
    private val canvasDrawSize: IntSize,
) {
    companion object {
        private var counter = 0
    }

    val key = counter++

    private var trigger by mutableIntStateOf(0)
    private val pixels =
        Bitmap.createBitmap(canvasDrawSize.width, canvasDrawSize.height, Bitmap.Config.ARGB_8888)
            .apply {
                setPixels(
                    IntArray(canvasDrawSize.height * canvasDrawSize.width) { EraseColor },
                    0, canvasDrawSize.width, 0, 0, canvasDrawSize.width, canvasDrawSize.height
                )
            }
    private val validRect = canvasDrawSize.toIntRect()
    var isVisible by mutableStateOf(true)
        private set

    var name by mutableStateOf(TextFieldValue("Layer"))

    private val undoRedoManager = UndoRedoManager(12)

    private var keepWork: Work? = null

    /** State of save. If there unsaved work returns false otherwise.
     * If save occurred must notify through [saved]. */
    var isSaved by mutableStateOf(true)
        private set

    fun getPixel(x: Int, y: Int): Int {
        require(!isNotValid(x, y)) { "Invalid canvas coordinates ($x, $y)" }
        return pixels.getPixel(x, y)
    }

    fun setPixel(x: Int, y: Int, color: Int): Boolean {
        if (isNotValid(x, y)) return false
        pixels.setPixel(x, y, color)
        return true
    }

    fun copyPixelsFrom(bitmap: Bitmap) {
        val (width, height) = canvasDrawSize

        for (y in 0..<height) {
            for (x in 0..<width) {
                pixels.setPixel(x, y, bitmap.getPixel(x, y))
            }
        }
    }

    fun mergePixels(bitmap: Bitmap, offset: IntOffset = IntOffset.Zero) {
        val startX = max(0, offset.x)
        val stopX = min(canvasDrawSize.width, offset.x + canvasDrawSize.width)

        val startY = max(0, offset.y)
        val stopY = min(canvasDrawSize.height, offset.y + canvasDrawSize.height)

        for (y in startY..<stopY) {
            for (x in startX..<stopX) {
                val c = bitmap[x - offset.x, y - offset.y]

                if (c != EraseColor) {
                    setPixel(x, y, c)
                }
            }
        }
    }

    fun invalidate() {
        trigger++
    }

    @SuppressLint("NewApi")
    fun asBitmap() = pixels.asShared().also { trigger }

    fun clearAll() = pixels.setPixels(
        IntArray(canvasDrawSize.height * canvasDrawSize.width) { EraseColor },
        0, canvasDrawSize.width, 0, 0, canvasDrawSize.width, canvasDrawSize.height
    )

    fun setVisibility(visible: Boolean) {
        isVisible = visible
    }

    fun pushWork() {
        undoRedoManager.pushAction(
            Work(
                0, asBitmap().copy(Bitmap.Config.ARGB_8888, false)
            )
        )

        if (isSaved) {
            isSaved = false
        }
    }

    fun keepCurrentWork() {
        keepWork = Work(
            0, asBitmap().copy(Bitmap.Config.ARGB_8888, false)
        )
    }

    fun pushKeptWork() {
        keepWork?.let { undoRedoManager.pushAction(it) }
        keepWork = null


        if (isSaved) {
            isSaved = false
        }
    }

    fun undo() {
        val (_, bitmap) = undoRedoManager.undo()
        copyPixelsFrom(bitmap)
    }

    fun redo() {
        val (_, bitmap) = undoRedoManager.redo()
        copyPixelsFrom(bitmap)
    }

    fun canUndo() = undoRedoManager.canUndo()
    fun canRedo() = undoRedoManager.canRedo()

    private fun isNotValid(x: Int, y: Int): Boolean {
        return !validRect.contains(IntOffset(x, y))
    }

    fun configureUndoRedo(initialWork: Work) {
        undoRedoManager.configure(initialWork)
    }

    /** Should call when saved */
    fun saved() {
        if (!isSaved) {
            isSaved = true
        }
    }
}