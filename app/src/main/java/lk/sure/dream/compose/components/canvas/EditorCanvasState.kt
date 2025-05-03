package lk.sure.dream.compose.components.canvas

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toOffset
import androidx.core.graphics.get
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** The state that can be use to control [EditorCanvas] */
@Stable
class EditorCanvasState(
    val canvasSize: IntSize,
) {
    val canvasRatio = canvasSize.width.toFloat() / canvasSize.height
    val frames = mutableStateListOf<Frame>()
    var activeFrameIndex by mutableIntStateOf(0)
        private set
    val activeLayerIndex get() = frames[activeFrameIndex].activeLayerIndex
    private val activeFrame get() = frames[activeFrameIndex]

    private fun activeLayer() = frames[activeFrameIndex].active()
    val activeFrameLayers: List<Layer> get() = activeFrame.layers

    var penColor by mutableIntStateOf(Color(0xFF000000).toArgb())
    var editMode by mutableStateOf(EditMode.Draw)
        private set

    var pixelSize by mutableFloatStateOf(0F)

    /** Reciprocal of [pixelSize] */
    private val rPixelSize by derivedStateOf { 1 / pixelSize }

    val layerImage get() = activeFrame.image

    private fun Offset.toCanvasIntOffset(): IntOffset {
        return IntOffset(
            (x * rPixelSize).toInt(),
            (y * rPixelSize).toInt(),
        )
    }

    val selection = Selection(canvasSize)

    val selectionMode get() = selection.mode

    var scale by mutableFloatStateOf(1f)

    // An absolute panning offset
    var offset by mutableStateOf(Offset.Zero)

    val shouldFit by derivedStateOf {
        !(scale == 1F && offset == Offset.Zero)
    }

    val recentColors = mutableStateListOf<Color>()
    var pickedColorRect: PickedColorRect? by mutableStateOf(null)

    var moveCopy: Layer? by mutableStateOf(null)
    private var copyOffset: IntOffset by mutableStateOf(IntOffset.Zero)
    private var lastEditMode: EditMode? = null

    private var clipboardLayer = Layer(canvasSize)
    private var isCutReplacement = false
    var canPaste by mutableStateOf(false)

    /** To keep track of addition or removal of layers frames
     * and more. */
    private var isActionPerformed by mutableStateOf(false)
    val allFramesSaved get() = frames.all { it.allLayersSaved } && !isActionPerformed

    val onionSkinSettings = OnionSkinSettings()

    val backwardSkins by derivedStateOf {
        getSkins(false)
    }

    val forwardSkins by derivedStateOf {
        getSkins(true)
    }

    init {
        addFrame()
    }

    private fun getSkins(isForward: Boolean) = with(onionSkinSettings) {
        buildList {
            if (!enabled) return@buildList
            val multiplier = if (isForward) 1 else -1
            val hue = if (isForward) 120F else 0F

            repeat(if (isForward) forwardSkinCount else backwardSkinCount) {
                val frame = frames.getOrNull(
                    if (isSkinWrapped) (frames.size + activeFrameIndex + multiplier * (it + 1)) % frames.size
                    else activeFrameIndex + multiplier * (it + 1)
                ) ?: return@buildList


                val bitmap = frame.image.copy(Bitmap.Config.ARGB_8888, true).apply {
                    if (useTrueColors) return@apply
                    val (width, height) = canvasSize
                    val pixels = IntArray(width * height)
                    getPixels(pixels, 0, width, 0, 0, width, height)
                    val greyScaled = pixels.map { color ->
                        val a = Color(color).alpha
                        val r = Color(color).red
                        val g = Color(color).green
                        val b = Color(color).blue
                        val gray = ((0.299 * r + 0.587 * g + 0.114 * b) * .75).toFloat() + .25F
                        Color.hsv(hue, 1F, gray, a).toArgb()
                    }.toIntArray()

                    setPixels(greyScaled, 0, width, 0, 0, width, height)
                }

                add(bitmap.asImageBitmap())
            }
        }

    }

    fun changeEditMode(editMode: EditMode) {
        if (this.editMode == EditMode.MoveSelected) return
        this.editMode = editMode
    }

    fun touchStart(position: Offset) {
        selection.reset()
        val positionInt = position.toCanvasIntOffset()

        when (editMode) {
            EditMode.PickColor -> {
                if (!canvasSize.toIntRect().contains(positionInt)) {
                    pickedColorRect = null
                    return
                }

                val colorInt = activeFrame.image[positionInt.x, positionInt.y]
                pickedColorRect = PickedColorRect(
                    Rect(positionInt.toOffset() * pixelSize, Size(1F, 1F) * pixelSize),
                    colorInt,
                    position.run { IntOffset(x.roundToInt(), y.roundToInt()) }
                )
            }

            else -> {}
        }
    }

    fun touchMove(
        position: Offset,
        prevPosition: Offset,
        startPosition: Offset,
    ) {
        val positionInt = position.toCanvasIntOffset()
        val prevPositionInt = prevPosition.toCanvasIntOffset()
        val startPositionInt = startPosition.toCanvasIntOffset()

        when (editMode) {
            EditMode.Draw -> {
                if (positionInt == prevPositionInt) {
                    drawPixel(positionInt)
                } else if ((positionInt - prevPositionInt).distance() < 2) {
                    drawPixel(prevPositionInt)
                    drawPixel(positionInt)
                } else {
                    drawLine(prevPositionInt, positionInt, activeFrame::setPixelForActiveLayer)
                }
            }

            EditMode.Erase -> {
                if (positionInt == prevPositionInt) {
                    drawPixel(positionInt, EraseColor)
                } else {
                    drawLine(
                        prevPositionInt,
                        positionInt,
                        activeFrame::setPixelForActiveLayer,
                        EraseColor
                    )
                }
            }

            EditMode.Fill -> {}

            EditMode.Line -> {
                activeFrame.previewLayerVisible = true
                activeFrame.clearPreviewLayer()
                drawLine(
                    startPositionInt,
                    positionInt,
                    activeFrame::setPixelForPreviewLayer
                )
                activeFrame.invalidateFinalImage()
            }

            EditMode.FilledRectangle -> {
                activeFrame.previewLayerVisible = true
                activeFrame.clearPreviewLayer()
                drawRectangle(
                    topLeft = startPositionInt,
                    bottomRight = positionInt,
                    putPixel = activeFrame::setPixelForPreviewLayer,
                    fill = true
                )
                activeFrame.invalidateFinalImage()
            }

            EditMode.OutlinedRectangle -> {
                activeFrame.previewLayerVisible = true
                activeFrame.clearPreviewLayer()
                drawRectangle(
                    topLeft = startPositionInt,
                    bottomRight = positionInt,
                    putPixel = activeFrame::setPixelForPreviewLayer
                )

                activeFrame.invalidateFinalImage()
            }

            EditMode.FilledCircle -> {
                activeFrame.previewLayerVisible = true
                activeFrame.clearPreviewLayer()
                val radius = (startPositionInt - positionInt).distance()
                drawCircle(
                    center = startPositionInt,
                    radius = radius,
                    putPixel = activeFrame::setPixelForPreviewLayer,
                    fill = true
                )
                activeFrame.invalidateFinalImage()
            }

            EditMode.OutlinedCircle -> {
                activeFrame.previewLayerVisible = true
                activeFrame.clearPreviewLayer()
                val radius = (startPositionInt - positionInt).distance()
                drawCircle(startPositionInt, radius, activeFrame::setPixelForPreviewLayer)
                activeFrame.invalidateFinalImage()
            }

            EditMode.Selection -> {
                Rect(
                    startPosition,
                    position
                ).normalize().apply {
                    infix fun Float.point(other: Float) = Offset(this, other)

                    selection.reset()
                    selection.pushPoint(left point top)
                    selection.pushPoint(right point top)
                    selection.pushPoint(right point bottom)
                    selection.pushPoint(left point bottom)
                }
            }

            EditMode.Lasso -> {
                selection.pushPoint(position)
            }

            EditMode.SelectNone -> {}
            EditMode.SelectionMode -> {}
            EditMode.ColorSelection -> {}
            EditMode.PickColor -> {
                if (!canvasSize.toIntRect().contains(positionInt)) {
                    pickedColorRect = null
                    return
                }

                val colorInt = activeFrame.image[positionInt.x, positionInt.y]
                pickedColorRect = PickedColorRect(
                    Rect(positionInt.toOffset() * pixelSize, Size(1F, 1F) * pixelSize),
                    colorInt,
                    position.run { IntOffset(x.roundToInt(), y.roundToInt()) }
                )
            }

            EditMode.MoveSelected -> {
                onMoveSelected(positionInt - prevPositionInt)
            }
        }
    }

    fun touchUp(
        position: Offset,
        isClick: Boolean,
        startPosition: Offset,
    ) {
        val positionInt = position.toCanvasIntOffset()
        val startPositionInt = startPosition.toCanvasIntOffset()

        if (isClick) {
            when (editMode) {
                EditMode.Draw -> {
                    drawPixel(positionInt)
                    activeFrame.pushWork()
                }

                EditMode.Erase -> {
                    drawPixel(positionInt, EraseColor)
                    activeFrame.pushWork()
                }

                EditMode.Fill -> {
                    fill(positionInt)
                    activeFrame.pushWork()
                }

                EditMode.Line,
                EditMode.FilledRectangle,
                EditMode.OutlinedRectangle,
                EditMode.FilledCircle,
                EditMode.OutlinedCircle,
                    -> {
                    activeFrame.previewLayerVisible = false
                    activeFrame.clearPreviewLayer()
                }

                EditMode.Selection, EditMode.Lasso -> {
                    selection.pushPoint((positionInt * pixelSize).toOffset())
                    selection.pushPoint((positionInt.let { it.copy(x = it.x + 1) } * pixelSize).toOffset())
                    selection.pushPoint((positionInt.let {
                        it.copy(
                            x = it.x + 1,
                            y = it.y + 1
                        )
                    } * pixelSize).toOffset())
                    selection.pushPoint((positionInt.let { it.copy(y = it.y + 1) } * pixelSize).toOffset())
                    selection.process(pixelSize, rPixelSize)
                    selection.reset()
                }

                EditMode.SelectNone -> {}
                EditMode.SelectionMode -> {}
                EditMode.ColorSelection -> {
                    val layer = activeLayer()
                    val color = layer.getPixel(positionInt.x, positionInt.y)

                    val grid = BooleanArray(canvasSize.width * canvasSize.height) {
                        val x = it % canvasSize.width
                        val y = it / canvasSize.width
                        val curColor = layer.getPixel(x, y)
                        curColor == color
                    }

                    selection.selectGiven(
                        positionInt, grid, pixelSize
                    )
                }

                EditMode.PickColor -> {
                    pickedColorRect = null
                    if (!canvasSize.toIntRect().contains(positionInt)) return
                    val colorInt = activeFrame.image[positionInt.x, positionInt.y]
                    if (colorInt == EraseColor) return

                    val color = Color(colorInt)
                    penColor = colorInt

                    if (recentColors.contains(color)) {
                        recentColors.remove(color)
                    }
                    recentColors.add(color)
                }

                EditMode.MoveSelected -> {}
            }
        } else {
            when (editMode) {
                EditMode.Draw -> activeFrame.pushWork()
                EditMode.Erase -> activeFrame.pushWork()
                EditMode.Fill -> {}

                EditMode.Line -> {
                    activeFrame.previewLayerVisible = false
                    activeFrame.clearPreviewLayer()

                    drawLine(
                        startPositionInt,
                        positionInt,
                        activeFrame::setPixelForActiveLayer
                    )
                    activeFrame.pushWork()
                }

                EditMode.FilledRectangle -> {
                    activeFrame.previewLayerVisible = false
                    activeFrame.clearPreviewLayer()

                    drawRectangle(
                        startPositionInt,
                        positionInt,
                        activeFrame::setPixelForActiveLayer,
                        true
                    )
                    activeFrame.pushWork()
                }

                EditMode.OutlinedRectangle -> {
                    activeFrame.previewLayerVisible = false
                    activeFrame.clearPreviewLayer()

                    drawRectangle(
                        startPositionInt,
                        positionInt,
                        activeFrame::setPixelForActiveLayer
                    )
                    activeFrame.pushWork()
                }

                EditMode.FilledCircle -> {
                    activeFrame.previewLayerVisible = false
                    activeFrame.clearPreviewLayer()

                    val radius = (startPositionInt - positionInt).distance()
                    drawCircle(
                        startPositionInt,
                        radius,
                        activeFrame::setPixelForActiveLayer,
                        true
                    )
                    activeFrame.pushWork()
                }

                EditMode.OutlinedCircle -> {
                    activeFrame.previewLayerVisible = false
                    activeFrame.clearPreviewLayer()

                    val radius = (startPositionInt - positionInt).distance()
                    drawCircle(
                        startPositionInt,
                        radius,
                        activeFrame::setPixelForActiveLayer
                    )
                    activeFrame.pushWork()
                }

                EditMode.Selection -> {
                    selection.process(pixelSize, rPixelSize)
                    selection.reset()
                }

                EditMode.Lasso -> {
                    selection.process(
                        pixelSize,
                        rPixelSize
                    )
                    selection.reset()

                }

                EditMode.SelectNone -> {}
                EditMode.SelectionMode -> {}
                EditMode.ColorSelection -> {}
                EditMode.PickColor -> {
                    pickedColorRect = null
                    if (!canvasSize.toIntRect().contains(positionInt)) return
                    val colorInt = activeFrame.image[positionInt.x, positionInt.y]
                    if (colorInt == EraseColor) return

                    val color = Color(colorInt)
                    penColor = colorInt

                    if (recentColors.contains(color)) {
                        recentColors.remove(color)
                    }
                    recentColors.add(color)
                }

                EditMode.MoveSelected -> {}
            }
        }
    }

    private fun IntOffset.distance(): Int {
        return max(abs(x), abs(y))
    }

    private fun shouldDraw(at: IntOffset): Boolean {
        return activeLayer().isVisible &&
                (selection.isSelectedNone ||
                        at.run {
                            val (width, _) = canvasSize
                            if (x >= width) false
                            else selection
                                .selectionPixels
                                .getOrElse(x + y * width) { false }
                        })
    }

    private fun shouldDraw(x: Int, y: Int) = shouldDraw(IntOffset(x, y))

    private fun drawPixel(at: IntOffset, color: Int = penColor) {
        if (!shouldDraw(at)) return
        activeFrame.setPixelForActiveLayer(at.x, at.y, color)
        activeFrame.invalidateFinalImage()
    }

    private fun fill(from: IntOffset) {
        val directions = listOf(
            IntOffset(0, -1), // UP
            IntOffset(1, 0), // RIGHT
            IntOffset(0, 1), // DOWN
            IntOffset(-1, 0), // LEFT
        )

        val colorAt = activeFrame.getPixelFormActiveLayer(from.x, from.y)
        if (colorAt == penColor || !shouldDraw(from)) return
        activeFrame.setPixelForActiveLayer(from.x, from.y, penColor)

        var listToFill = listOf(from)
        val canvasRect = canvasSize.toIntRect()

        while (listToFill.isNotEmpty()) {
            listToFill = buildList {
                for (at in listToFill) {
                    for (direction in directions) {
                        val curAt = at + direction
                        if (
                            canvasRect.contains(curAt) &&
                            activeFrame.getPixelFormActiveLayer(curAt.x, curAt.y) == colorAt &&
                            shouldDraw(curAt)
                        ) {
                            activeFrame.setPixelForActiveLayer(curAt.x, curAt.y, penColor)
                            add(curAt)
                        }
                    }
                }
            }
        }
        activeFrame.invalidateLayers()
    }

    private fun drawLine(
        from: IntOffset,
        to: IntOffset,
        putPixel: (x: Int, y: Int, color: Int) -> Unit,
        penColor: Int = this.penColor,
    ) {
        if (abs(to.x - from.x) > abs(to.y - from.y))
            drawLineH(from, to, penColor, putPixel)
        else
            drawLineV(from, to, penColor, putPixel)
    }

    private fun drawLineH(
        from: IntOffset,
        to: IntOffset,
        penColor: Int,
        putPixel: (Int, Int, Int) -> Unit,
    ) {
        var realFrom = from
        var realTo = to
        val validRect = canvasSize.toIntRect()

        if (from.x > to.x) {
            realFrom = to
            realTo = from
        }

        val dx = realTo.x - realFrom.x
        var dy = realTo.y - realFrom.y

        val dir = if (dy < 0) -1 else 1
        dy *= dir

        if (dx != 0) {
            var y = realFrom.y
            var p = 2 * dy - dx
            for (i in 0..dx) {
                val x = realFrom.x + i
                if (validRect.contains(IntOffset(x, y)) && shouldDraw(IntOffset(x, y)))
                    putPixel(x, y, penColor)

                if (p >= 0) {
                    y += dir
                    p -= 2 * dx
                }
                p += 2 * dy
            }
        }
    }

    private fun drawLineV(
        from: IntOffset,
        to: IntOffset,
        penColor: Int,
        putPixel: (Int, Int, Int) -> Unit,
    ) {
        var realFrom = from
        var realTo = to
        val validRect = canvasSize.toIntRect()

        if (from.y > to.y) {
            realFrom = to
            realTo = from
        }

        var dx = realTo.x - realFrom.x
        val dy = realTo.y - realFrom.y

        val dir = if (dx < 0) -1 else 1
        dx *= dir

        if (dy != 0) {
            var x = realFrom.x
            var p = 2 * dx - dy
            for (i in 0..dy) {
                val y = realFrom.y + i
                if (validRect.contains(IntOffset(x, y)) && shouldDraw(IntOffset(x, y)))
                    putPixel(x, y, penColor)

                if (p >= 0) {
                    x += dir
                    p -= 2 * dy
                }
                p += 2 * dx
            }
        }
    }

    private fun drawRectangle(
        topLeft: IntOffset,
        bottomRight: IntOffset,
        putPixel: (x: Int, y: Int, color: Int) -> Unit,
        fill: Boolean = false,
    ) {
        val canvasRect = canvasSize.toIntRect()
        val drawRect = IntRect(topLeft, bottomRight).normalize()

        for (x in drawRect.left..drawRect.right) {
            if (
                drawRect.top in canvasRect.top..<canvasRect.bottom &&
                x in canvasRect.left..<canvasRect.right &&
                shouldDraw(IntOffset(x, drawRect.top))
            ) putPixel(x, drawRect.top, penColor)

            if (
                drawRect.bottom in canvasRect.top..<canvasRect.bottom &&
                x in canvasRect.left..<canvasRect.right &&
                shouldDraw(IntOffset(x, drawRect.bottom))
            ) putPixel(x, drawRect.bottom, penColor)
        }


        val yPoints = buildList {
            for (y in drawRect.top + 1..<drawRect.bottom) {
                if (
                    drawRect.left in canvasRect.left..<canvasRect.right &&
                    y in canvasRect.top..<canvasRect.bottom &&
                    shouldDraw(IntOffset(drawRect.left, y))
                ) add(IntOffset(drawRect.left, y))

                if (
                    drawRect.right in canvasRect.left..<canvasRect.right &&
                    y in canvasRect.top..<canvasRect.bottom &&
                    shouldDraw(IntOffset(drawRect.right, y))
                ) add(IntOffset(drawRect.right, y))
            }
        }

        if (fill) {
            for (i in 0..<yPoints.size / 2) {
                val p1 = yPoints[2 * i]
                val p2 = yPoints[2 * i + 1]

                for (px in p1.x..p2.x) {
                    if (shouldDraw(IntOffset(px, p1.y)))
                        putPixel(px, p1.y, penColor)
                }
            }
        } else {
            for (point in yPoints) {
                if (shouldDraw(point))
                    putPixel(point.x, point.y, penColor)
            }
        }

        activeFrame.invalidateFinalImage()
    }

    private fun drawCircle(
        center: IntOffset,
        radius: Int,
        putPixel: (x: Int, y: Int, color: Int) -> Unit,
        fill: Boolean = false,
    ) {
        var x = 0
        var y = -radius
        var p = -radius

        while (x < -y) {
            if (p > 0) {
                y++
                p += 2 * (x + y) + 1
            } else {
                p += 2 * x + 1
            }

            val points = buildList {
                add(IntOffset(center.x - x, center.y + y))
                add(IntOffset(center.x + x, center.y + y))

                add(IntOffset(center.x - x, center.y - y))
                add(IntOffset(center.x + x, center.y - y))

                add(IntOffset(center.x + y, center.y + x))
                add(IntOffset(center.x - y, center.y + x))

                add(IntOffset(center.x + y, center.y - x))
                add(IntOffset(center.x - y, center.y - x))
            }

            val canvasRect = canvasSize.toIntRect()
            fun isValid(x: Int, y: Int) = canvasRect.contains(IntOffset(x, y))
            fun isValid(at: IntOffset) = canvasRect.contains(at)

            if (fill) {
                for (i in 0..<4) {
                    val p1 = points[2 * i]
                    val p2 = points[2 * i + 1]
                    for (px in p1.x..p2.x) {
                        if (isValid(px, p1.y) && shouldDraw(px, p1.y))
                            putPixel(px, p1.y, penColor)
                    }
                }
            } else {
                for (point in points) {
                    if (isValid(point) && shouldDraw(point))
                        putPixel(point.x, point.y, penColor)
                }
            }
            x += 1
        }
        activeFrame.invalidateFinalImage()
    }

    val canUndo by derivedStateOf { activeFrame.canUndo() }
    val canRedo by derivedStateOf { activeFrame.canRedo() }
    fun undo() = activeFrame.undoWork()
    fun redo() = activeFrame.redoWork()

    fun clearSelection() = selection.clear()

    fun addLayer() {
        var number = 1

        while (activeFrame.anyLayer { it.name.text == "Layer $number" }) {
            number++
        }


        frames.forEach {
            it.addLayer("Layer $number")
        }
    }

    fun removeLayer(layerIndex: Int) {
        frames.forEach {
            it.removeLayer(layerIndex)
        }
        isActionPerformed = true
    }

    fun onActiveFrameIndexChange(frameIndex: Int) {
        activeFrameIndex = frameIndex
    }

    fun setActiveLayer(layerIndex: Int) {
        frames.forEach {
            it.activeLayerIndex = layerIndex
        }
    }

    fun setLayerName(layerIndex: Int, textValue: TextFieldValue) {
        frames.forEach {
            it[layerIndex].name = textValue
        }
        isActionPerformed = true
    }

    fun isLayerVisible(layerIndex: Int) = activeFrame[layerIndex].isVisible

    fun setLayerVisibility(layerIndex: Int, visible: Boolean) {
        frames.forEach {
            it[layerIndex].setVisibility(visible)
            it.invalidateLayers()
        }
        isActionPerformed = true
    }

    fun moveLayer(from: Int, to: Int) {
        frames.forEach {
            it.moveLayer(from, to)
        }
        isActionPerformed = true
    }

    fun addFrame() {
        addFrame(frames.size)
    }

    fun addFrame(index: Int) {
        frames.add(index, Frame(canvasSize).also {
            if (frames.isEmpty()) return@also it.addLayer("Layer 1")

            val frameRef = frames.first()
            for (i in 0..<frameRef.layerCount) {
                val layerRef = frameRef[i]
                it.addLayer(layerRef.name.text)
                val layer = it[i]
                layer.setVisibility(layerRef.isVisible)
            }
        })
    }

    fun removeFrame(frameIndex: Int) {
        if (frameIndex == 0) {
            activeFrameIndex = 0
        } else if (activeFrameIndex > frameIndex || activeFrameIndex == frames.lastIndex) {
            activeFrameIndex--
        } else if (activeFrameIndex == frameIndex) {
            activeFrameIndex++
        }

        frames.removeAt(frameIndex)
        isActionPerformed = true
    }

    fun setSelectionMode(mode: Selection.Mode) {
        selection.mode = mode
    }

    fun invertSelection() {
        selection.invertSelection(pixelSize)
    }

    fun toByteArray(): ByteArray {
        val layers = buildList {
            activeFrame.forEachLayer {
                add(
                    CanvasStateSaver.LayerData(
                        it.name.text,
                        it.isVisible,
                    )
                )
            }
        }

        val bitmaps = buildList {
            repeat(frames.size) { frameIdx ->
                repeat(layers.size) { layerIdx ->
                    add(frames[frameIdx][layerIdx].asBitmap())
                }
            }
        }


        return CanvasStateSaver.toByteArray(
            CanvasStateSaver.CanvasData(
                canvasSize.width,
                canvasSize.height,
                frames.size,
                layers,
                bitmaps,
                recentColors.map { it.toArgb() },
            )
        )
    }

    private fun setupWith(result: CanvasStateSaver.CanvasData) {
        frames.removeAt(0)
        frames.add(Frame(canvasSize).also {
            result.layers.forEach { layerData ->
                it.addLayer(layerData.name)
                it.lastLayer().setVisibility(layerData.isVisible)
            }
        })
        repeat(result.frameCount - 1) {
            addFrame()
        }

        repeat(result.frameCount) { frameIdx ->
            repeat(result.layers.size) { layerIdx ->
                val layer = frames[frameIdx][layerIdx]
                val bitmap = result.bitmaps[frameIdx * result.layers.size + layerIdx]
                layer.copyPixelsFrom(bitmap)
                layer.configureUndoRedo(Work(0, bitmap))
            }
            frames[frameIdx].invalidateLayers()
        }

        penColor = result.recentColors?.lastOrNull() ?: Color.Black.toArgb()

        if (result.recentColors.isNullOrEmpty()) {
            recentColors.add(Color(penColor))
        } else {
            recentColors.addAll(result.recentColors.map { Color(it) })
        }
    }

    fun mergeDownLayer(layerIndex: Int) {
        frames.forEach {
            it.mergeDown(layerIndex)
        }
        isActionPerformed = true
    }

    fun fitCanvas() {
        scale = 1F
        offset = Offset.Zero
    }

    fun swapFrames(fromIndex: Int, toIndex: Int) {
        Collections.swap(frames, fromIndex, toIndex)
        isActionPerformed = true
    }

    fun isFrameExists(frameIndex: Int): Boolean {
        return frameIndex in frames.indices
    }

    fun onMoveSelectedStart() {
        lastEditMode = editMode
        editMode = EditMode.MoveSelected
        activeFrame.previewLayerVisible = true
        activeFrame.clearPreviewLayer()

        activeLayer().keepCurrentWork()

        val bitmap = activeLayer().asBitmap().copy(Bitmap.Config.ARGB_8888, true).also {
            var doClearSelection = false
            if (selection.isSelectedNone) {
                selection.invertSelection(pixelSize)
                doClearSelection = true
            }

            for (y in 0..<canvasSize.height) {
                for (x in 0..<canvasSize.width) {
                    if (selection.selectionPixels[y * canvasSize.width + x]) {
                        activeLayer().setPixel(x, y, EraseColor)
                    } else {
                        it.setPixel(x, y, EraseColor)
                    }
                }
            }

            if (doClearSelection) {
                selection.invertSelection(pixelSize)
            }
        }
        selection.clear()

        moveCopy = Layer(canvasSize)
        moveCopy?.copyPixelsFrom(bitmap)
        copyOffset = IntOffset.Zero
        activeFrame.copyToPreviewLayer(moveCopy!!.asBitmap(), copyOffset)
        activeFrame.invalidateLayers()
    }

    private fun onMoveSelected(moveAmount: IntOffset) {
        moveCopy?.asBitmap()?.let {
            activeFrame.clearPreviewLayer()
            copyOffset += moveAmount
            activeFrame.copyToPreviewLayer(it, copyOffset)
            activeFrame.invalidateLayers()
        }
    }

    fun onMoveSelectedCancel() {
        editMode = lastEditMode!!
        lastEditMode = null
        if (!isCutReplacement) {
            moveCopy
                ?.asBitmap()
                ?.let {
                    activeLayer().mergePixels(it, IntOffset.Zero)
                }
        }
        moveCopy = null
        activeFrame.previewLayerVisible = false
        activeFrame.clearPreviewLayer()
        isCutReplacement = false
        activeFrame.invalidateLayers()
    }

    fun onMoveSelectedStop() {
        editMode = lastEditMode!!
        lastEditMode = null
        moveCopy?.asBitmap()?.let { activeLayer().mergePixels(it, copyOffset) }
        moveCopy = null
        activeFrame.previewLayerVisible = false
        activeFrame.clearPreviewLayer()

        if (!isCutReplacement) {
            activeLayer().pushKeptWork()
        }
        activeLayer().pushWork()

        isCutReplacement = false
    }

    fun copySelectionToClipboard(cutAndCopy: Boolean = false) {
        val bitmap = activeLayer()
            .asBitmap()
            .copy(Bitmap.Config.ARGB_8888, true)
            .also {
                var doClearSelection = false
                if (selection.isSelectedNone) {
                    selection.invertSelection(pixelSize)
                    doClearSelection = true
                }

                for (y in 0..<canvasSize.height) {
                    for (x in 0..<canvasSize.width) {
                        if (selection.selectionPixels[y * canvasSize.width + x]) {
                            if (cutAndCopy) activeLayer().setPixel(x, y, EraseColor)
                        } else {
                            it.setPixel(x, y, EraseColor)
                        }
                    }
                }

                if (doClearSelection) {
                    selection.invertSelection(pixelSize)
                }
            }


        clipboardLayer.copyPixelsFrom(bitmap)
        canPaste = true
        if (cutAndCopy) {
            activeLayer().pushWork()
            activeFrame.invalidateLayers()
        }
    }

    fun pasteFromClipboard() {
        lastEditMode = editMode
        editMode = EditMode.MoveSelected
        activeFrame.previewLayerVisible = true
        activeFrame.clearPreviewLayer()

        moveCopy = Layer(canvasSize)
        moveCopy?.copyPixelsFrom(clipboardLayer.asBitmap())
        copyOffset = IntOffset.Zero
        activeFrame.copyToPreviewLayer(moveCopy!!.asBitmap(), copyOffset)
        activeFrame.invalidateLayers()
        isCutReplacement = true
    }

    fun duplicateActiveFrame() {
        val frame = activeFrame
        val newIndex = activeFrameIndex + 1
        addFrame(newIndex)
        val newFrame = frames[newIndex]

        newFrame.layers.forEachIndexed { i, layer ->
            layer.copyPixelsFrom(frame.layers[i].asBitmap())
        }
        isActionPerformed = true
        newFrame.invalidateLayers()
    }

    fun addToRecent(selectedColor: Color) {
        recentColors.remove(selectedColor)
        recentColors.add(selectedColor)
    }

    fun saveAll() {
        frames.forEach { frame ->
            frame.saveAllLayers()
        }
        isActionPerformed = false
    }

    fun removeAllRecentColors() {
        recentColors.clear()
    }

    companion object {
        fun createState(data: CanvasStateSaver.CanvasData): EditorCanvasState {
            val canvasState = EditorCanvasState(IntSize(data.width, data.height))
            canvasState.setupWith(data)
            return canvasState
        }
    }
}