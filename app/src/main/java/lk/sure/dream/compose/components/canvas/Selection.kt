package lk.sure.dream.compose.components.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toOffset
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class Selection(
    private val canvasDrawSize: IntSize,
) {
    enum class Mode {
        Replace,
        Addition,
        Subtraction;
    }

    private val points = mutableStateListOf<Offset>()
    var mode by mutableStateOf(Mode.Replace)

    val userSelection get() = points.toList()

    val selectionPixels = BooleanArray(canvasDrawSize.width * canvasDrawSize.height) { false }

    var selectionPolygons: List<List<Offset>> by mutableStateOf(emptyList())

    val isSelectedNone get() = selectionPixels.all { !it }

    private var boundingLeft = Float.POSITIVE_INFINITY
    private var boundingRight = Float.NEGATIVE_INFINITY
    private var boundingTop = Float.POSITIVE_INFINITY
    private var boundingBottom = Float.NEGATIVE_INFINITY

    fun selectGiven(
        from: IntOffset,
        grid: BooleanArray,
        pixelSize: Float,
    ) {
        fun IntOffset.index() = x + y * canvasDrawSize.width
        if (mode == Mode.Replace) {
            selectionPixels.fill(false)
        }

        val flood = floodFill(grid, from).map { it.index() }

        repeat(selectionPixels.size) {
            val shouldSelect = flood.contains(it)
            if (!shouldSelect) return@repeat
            selectionPixels[it] = mode != Mode.Subtraction
        }

        selectionPolygons = outlinePolygon(selectionPixels).map { list -> list.map { it.toOffset() * pixelSize } }
    }

    fun pushPoint(offset: Offset): Boolean {
        points.lastOrNull()?.let {
            if (!((it - offset).getDistanceSquared() > 900)) return false
        }

        if (offset.x < boundingLeft)
            boundingLeft = offset.x
        else if (offset.x > boundingRight)
            boundingRight = offset.x


        if (offset.y < boundingTop)
            boundingTop = offset.y
        else if (offset.y > boundingBottom)
            boundingBottom = offset.y

        return points.add(offset)
    }

    /**
     * Calculates the outline of the selection according to [points] (which is user outline)
     */
    fun process(
        pixelSize: Float,
        rPixelSize: Float,
    ) {
        val (width, height) = canvasDrawSize
        if (mode == Mode.Replace)
            selectionPixels.fill(false)
        val halfPixelSize = pixelSize * .5F

        fun Int.position() = this * pixelSize + halfPixelSize

        val startX = max(0, (boundingLeft * rPixelSize).toInt())
        val endX = min(width, ceil(boundingRight * rPixelSize).toInt())
        val topY = max(0, (boundingTop * rPixelSize).toInt())
        val bottomY = min(height, ceil(boundingBottom * rPixelSize).toInt())

        for (y in topY..<bottomY) {
            for (x in startX..<endX) {
                val (px, py) = Offset(x.position(), y.position())
                var count = 0
                for (idx in 0..<points.size) {
                    val (x1, y1) = points[idx]
                    val (x2, y2) = points[(idx + 1) % points.size]

                    if ((y1 <= py && y2 > py) || (y2 <= py && y1 > py)) {
                        val xIntersect = x1 + (py - y1) * (x2 - x1).toDouble() / (y2 - y1)
                        if (xIntersect > px) {
                            count++
                        }
                    }
                }

                if (count % 2 == 1) {
                    selectionPixels[x + width * y] = mode != Mode.Subtraction
                }
            }
        }

        selectionPolygons =
            generateOutline(selectionPixels).map { list -> list.map { it.toOffset() * pixelSize } }
    }

    /**
     * Takes [grid] where selected pixel values are true not selected are false.
     * And returns real outline for those selected pixels. (With the inner outline if
     * shape is convex or hallow).
     * Made for single continuous shape. For multiple shapes see [generateOutline]
     */
    private fun outlinePolygon(grid: BooleanArray): List<List<IntOffset>> {
        val points = mutableListOf<IntOffset>()
        val (width, height) = canvasDrawSize
        val validRect = canvasDrawSize.toIntRect()

        fun IntOffset.isSelected(offX: Int = 0, offY: Int = 0) = IntOffset(x + offX, y + offY).let {
            if (validRect.contains(it)) grid[it.x + it.y * width] else false
        }

        fun forEach(action: (at: IntOffset, value: Boolean) -> Unit) {
            for (y in 0..<height) {
                for (x in 0..<width) {
                    IntOffset(x, y).also {
                        action(it, it.isSelected())
                    }
                }
            }
        }

        fun IntOffset.move(x: Int = 0, y: Int = 0) = this + IntOffset(x, y)

        forEach { at, value ->
            if (!value) return@forEach
            val list = mutableListOf<IntOffset>()

            val isTopSelected = at.isSelected(offY = -1)

            if (!isTopSelected) {
                list.add(at)
                list.add(at.move(x = 1))
            }

            val isRightSelected = at.isSelected(offX = 1)

            if (!isRightSelected) {
                if (isTopSelected)
                    list.add(at.move(x = 1))

                list.add(at.move(x = 1, y = 1))
            }

            val isBottomSelected = at.isSelected(offY = 1)

            if (!isBottomSelected) {
                if (isRightSelected)
                    list.add(at.move(x = 1, y = 1))

                list.add(at.move(y = 1))
            }

            val isLeftSelected = at.isSelected(offX = -1)

            if (!isLeftSelected) {
                if (isBottomSelected)
                    list.add(at.move(y = 1))

                if (isTopSelected)
                    list.add(at)
            }

            points.addAll(list)
        }

        val uniquePoints = points.toSet().toList()

        val polygons = mutableListOf<List<IntOffset>>()
        var polygon = mutableListOf<IntOffset>()
        val directions = listOf(
            IntOffset(0, -1),
            IntOffset(1, 0),
            IntOffset(0, 1),
            IntOffset(-1, 0),
        )
        var skip = 0
        val start: IntOffset = uniquePoints.firstOrNull() ?: return emptyList()
        var point = start
        val list = uniquePoints.toMutableList().apply { removeAt(0) }
        var depth = 0
        var offset = 0
        polygon.add(start)

        // Logical xnor
        infix fun Boolean.xnor(other: Boolean) = !(this xor other)
        val remainPoint = mutableListOf<IntOffset>()

        while (list.isNotEmpty()) {
            var brokenOut = false

            for (i in directions.indices) {
                val dirIdx = (i + offset) % directions.size
                if (skip == dirIdx) continue
                val dir = directions[dirIdx]

                if (dir.y == 0) {
                    if (dir.x > 0) {
                        if (point.isSelected() xnor point.isSelected(offY = -1)) continue
                    } else {
                        if (point.isSelected(offX = -1) xnor point.isSelected(
                                offX = -1,
                                offY = -1
                            )
                        ) continue
                    }
                }

                if (dir.x == 0) {
                    if (dir.y > 0) {
                        if (point.isSelected() xnor point.isSelected(offX = -1)) continue
                    } else {
                        if (point.isSelected(offY = -1) xnor point.isSelected(
                                offX = -1,
                                offY = -1
                            )
                        ) continue
                    }
                }

                val neighbor = point + dir
                if (list.contains(neighbor)) {
                    offset = if (dir.x == 0) {
                        val y = if (dir.y == 1) -1 else 0

                        if (neighbor.isSelected(offY = y)) 1
                        else 3
                    } else {
                        val x = if (dir.x == 1) -1 else 0

                        if (neighbor.isSelected(offX = x)) 2
                        else 0
                    }

                    val isDiagonalSelected = neighbor.isSelected(-1, -1) && neighbor.isSelected() &&
                            !neighbor.isSelected(-1) && !neighbor.isSelected(0, -1) ||
                            !neighbor.isSelected(-1, -1) && !neighbor.isSelected() &&
                            neighbor.isSelected(-1) && neighbor.isSelected(0, -1)

                    if (!isDiagonalSelected || remainPoint.contains(neighbor))
                        list.remove(neighbor)
                    else
                        remainPoint.add(neighbor)

                    skip = (dirIdx + 2) % directions.size
                    point = neighbor
                    polygon.add(point)
                    brokenOut = true
                    break
                }
            }

            if (!brokenOut) {
//                    throw RuntimeException("No neighbor found! $point")
//                    Log.w("A", "No neighbor found! $start $point")
                polygons.add(polygon)
                skip = -1
                point = list.removeAt(0)
                polygon = mutableListOf(point)
//                    return@let it

            }

            if (depth++ > 1_000_000) {
                throw RuntimeException("Maximum iterations reached!")
            }
        }

        polygons.add(polygon)
        return polygons.toList()
    }

    /**
     * Takes [grid] where selected pixel values are true not selected are false.
     * And returns real outline for those selected pixels. (With the inner outline if
     * shape is convex or hallow).
     * Made for multiple shape. For single shape see [outlinePolygon]
     */
    private fun generateOutline(grid: BooleanArray): List<List<IntOffset>> {
        val (width, height) = canvasDrawSize
        val visited = mutableListOf<IntOffset>()
        fun IntOffset.isVisited() = visited.contains(this)
        fun List<IntOffset>.setAllVisited() = visited.addAll(this)
        val polygons = mutableListOf<List<IntOffset>>()

        fun IntOffset.index() = x + y * width
        fun BooleanArray.forEvery(action: (IntOffset, Boolean) -> Unit) {
            for (y in 0..<height) {
                for (x in 0..<width) {
                    IntOffset(x, y).also {
                        action(it, get(it.index()))
                    }
                }
            }
        }

        grid.forEvery { at, value ->
            if (!value || at.isVisited()) return@forEvery
            val filled = floodFill(grid, at)
            filled.setAllVisited()

            val newGrid = BooleanArray(grid.size) { false }.also { arr ->
                filled.forEach { arr[it.index()] = true }
            }

            val polygon = outlinePolygon(newGrid)
            polygons.addAll(polygon)
        }

        return polygons
    }

    /**
     * Returns flood filled pixels at given [from] position for the [grid] where flood filled
     * pixels have same value as [from]'s value.
     */
    private fun floodFill(grid: BooleanArray, from: IntOffset): List<IntOffset> {
        val directions = listOf(
            IntOffset(0, -1), // UP
            IntOffset(1, 0), // RIGHT
            IntOffset(0, 1), // DOWN
            IntOffset(-1, 0), // LEFT
        )

        val filledSet = mutableSetOf<IntOffset>()
        val (width, _) = canvasDrawSize
        fun IntOffset.index() = x + y * width
        fun IntOffset.value() = grid[index()]
        fun IntOffset.setFilled() = filledSet.add(this)

        val valueAt = from.value()
        from.setFilled()

        var listToFill = listOf(from)
        val canvasRect = canvasDrawSize.toIntRect()
        var count = 0
        val visited = Array(grid.size) { false }
        visited[from.index()] = true

        while (listToFill.isNotEmpty()) {
            count++
            listToFill = buildList {
                for (at in listToFill) {
                    for (direction in directions) {
                        val curAt = at + direction
                        if (
                            canvasRect.contains(curAt) &&
                            curAt.value() == valueAt &&
                            !filledSet.contains(curAt)
                        ) {
                            curAt.setFilled()
                            add(curAt)
                        }
                    }
                }
            }
        }

        return filledSet.toList()
    }

    /** Clears the user drawn selection outline */
    fun reset() {
        points.clear()
    }

    /** Clears the pixel selection */
    fun clear() {
        selectionPolygons = emptyList()
        selectionPixels.fill(false)
        points.clear()
    }

    /** Inverts the current selection */
    fun invertSelection(pixelSize: Float) {
        selectionPixels.forEachIndexed { index, b ->
            selectionPixels[index] = !b
        }
        selectionPolygons =
            generateOutline(selectionPixels).map { list -> list.map { it.toOffset() * pixelSize } }
    }
}