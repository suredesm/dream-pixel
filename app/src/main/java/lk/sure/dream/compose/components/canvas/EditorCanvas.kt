package lk.sure.dream.compose.components.canvas

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.core.graphics.scale
import lk.sure.dream.compose.components.colorpicker.toHex
import lk.sure.dream.compose.editor.Tool
import lk.sure.dream.compose.editor.ToolRow

@Composable
fun EditorCanvas(
    canvasState: EditorCanvasState,
    modifier: Modifier = Modifier,
) {
    val touchPoints = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }

    // Pan canvasState.offset relative to scale
    val scaledOffset by remember {
        derivedStateOf {
            canvasState.offset + (canvasSize.toSize() * .5F * (1F - canvasState.scale)).run {
                Offset(width, height)
            }
        }
    }

    var touchStart by remember { mutableStateOf<Offset?>(null) }
    var touchMove by remember { mutableStateOf<Offset?>(null) }
    var touchEnd by remember { mutableStateOf<Offset?>(null) }

    val infiniteTransition = rememberInfiniteTransition()
    val density = LocalDensity.current

    val selectionWidth by remember(canvasState.scale) { mutableFloatStateOf(6F / canvasState.scale) }
    val dashSize by remember(canvasState.scale) {
        mutableFloatStateOf(with(density) { 8.dp.roundToPx() / canvasState.scale })
    }
    val phase by infiniteTransition.animateFloat(
        initialValue = 2 * dashSize,
        targetValue = 0F,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    fun Offset.toScaled() = (this - scaledOffset) / canvasState.scale

    val canvasModifier = Modifier
        .pointerInput(Unit) {

            awaitEachGesture {
                val down = awaitFirstDown()
                val startTouchCount = currentEvent.changes.size

                if (startTouchCount > 1) return@awaitEachGesture

                touchStart = down.position
                canvasState.touchStart(down.position)
                touchPoints.add(touchStart!!)
                touchMove = null
                touchEnd = null
                currentEvent.changes.firstOrNull()?.consume()

                fun endTouch() {
                    touchPoints.clear()
                    touchEnd = touchMove
                    canvasState.touchUp(
                        (touchMove ?: touchStart!!).toScaled(),
                        touchMove == null, (touchStart!!).toScaled()
                    )
                }

                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.size > 1) break

                    val move = event.changes.firstOrNull()
                    if (move != null && move.pressed) {
                        canvasState.touchMove(
                            move.position.toScaled(),
                            (touchMove ?: touchStart!!).toScaled(),
                            touchStart!!.toScaled()
                        )
                        touchMove = move.position
                        touchPoints.add(move.position)
                    } else break
                }

                if (currentEvent.changes.size == 1 || touchMove != null)
                    endTouch()
            }
        }

    var transparentBg by remember {
        mutableStateOf(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        )
    }

    val checkBackgroundColors = if (isSystemInDarkTheme())
        Color.Gray to Color.DarkGray
    else
        Color.White to Color.LightGray

    LaunchedEffect(Unit) {
        val (width, height) = canvasState.canvasSize
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val widthM = width / 8
        val heightM = height / 8

        for (my in 0..heightM) {
            for (mx in 0..widthM) {
                val isWhite = my % 2 == 0 && mx % 2 == 0 || my % 2 == 1 && mx % 2 == 1
                val color =
                    (if (isWhite) checkBackgroundColors.first else checkBackgroundColors.second).toArgb()
                for (y in 0..7) {
                    for (x in 0..7) {
                        try {
                            bitmap.setPixel(
                                mx * 8 + x,
                                my * 8 + y,
                                color
                            )
                        } catch (_: IllegalArgumentException) {
                            break
                        }
                    }
                }
            }
        }
        transparentBg = bitmap
    }

    Box(
        modifier = modifier.then(Modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1F) {
                        canvasState.scale = (canvasState.scale * zoom).coerceIn(.5F, 5F)
                        canvasState.offset += pan
                    }
                }
            }),
    ) {
        Canvas(
            modifier = Modifier
                .then(
                    if (canvasState.canvasSize.width >= canvasState.canvasSize.height)
                        Modifier.fillMaxWidth()
                    else
                        Modifier.fillMaxHeight()
                )
                .aspectRatio(canvasState.canvasRatio)
                .onSizeChanged {
                    canvasSize = it
                    canvasState.pixelSize =
                        canvasSize.width.toFloat() / canvasState.canvasSize.width
                }
                .then(canvasModifier)
                .graphicsLayer {
                    scaleX = canvasState.scale
                    scaleY = canvasState.scale
                    translationX = canvasState.offset.x
                    translationY = canvasState.offset.y
                }
        ) {
            drawImage(
                Bitmap.createScaledBitmap(
                    transparentBg,
                    canvasSize.width,
                    canvasSize.height,
                    false
                ).asImageBitmap()
            )

            canvasState.forwardSkins.forEachIndexed { index, image ->
                drawImage(
                    image = image,
                    dstSize = canvasSize,
                    alpha = 0.5F / (index + 1),
                    filterQuality = FilterQuality.None,
                )
            }

            canvasState.backwardSkins.forEachIndexed { index, image ->
                drawImage(
                    image = image,
                    dstSize = canvasSize,
                    alpha = 0.5F / (index + 1),
                    filterQuality = FilterQuality.None,
                )
            }

            drawImage(
                canvasState.layerImage.scale(canvasSize.width, canvasSize.height, false)
                    .asImageBitmap(),
            )

            val blackDash =
                PathEffect.dashPathEffect(floatArrayOf(dashSize, dashSize), phase = phase)
            val whiteDash =
                PathEffect.dashPathEffect(
                    floatArrayOf(dashSize, dashSize),
                    phase = dashSize + phase
                )

            canvasState.selection.userSelection.let {
                val p1 = it.firstOrNull() ?: return@let

                val path = Path()
                path.moveTo(p1.x, p1.y)
                it.forEach { point ->
                    path.lineTo(point.x, point.y)
                }
                path.close()

                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(
                        width = selectionWidth,
                        pathEffect = blackDash,
                        cap = Stroke.DefaultCap,
                    )
                )

                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(
                        width = selectionWidth,
                        pathEffect = whiteDash,
                        cap = Stroke.DefaultCap,
                    )
                )
            }

            @Suppress("UNREACHABLE_CODE")
            canvasState.selection.selectionPolygons.forEach { polygon: List<Offset> ->
                val p1 = polygon.firstOrNull() ?: return@forEach

                val path = Path()
                path.moveTo(p1.x, p1.y)
                val needToReverse = polygon.windowed(2).firstOrNull {
                    it.first().x != it.last().x
                }?.let { it.first().x > it.last().x } ?: false

                polygon.let { if (needToReverse) it.asReversed() else it }.forEach { point ->
                    path.lineTo(point.x, point.y)
                }
                path.close()

                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(
                        width = selectionWidth,
                        pathEffect = blackDash,
                        cap = Stroke.DefaultCap,
                    )
                )

                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(
                        width = selectionWidth,
                        pathEffect = whiteDash,
                        cap = Stroke.DefaultCap,
                    )
                )

                return@forEach
                drawPoints(
                    polygon + p1,
                    PointMode.Polygon,
                    Color.Magenta,
                    6f,
                    Stroke.DefaultCap,
                    blackDash
                )


                val res = 1F / polygon.size
                val pointSize = 20F
                val halfPointSize = pointSize * 0.5F

                for (idx in polygon.indices) {
                    val point = polygon[idx]

                    drawRect(
                        Color.hsl(idx * res * 360F, 1F, 0.5F),
                        point + Offset(-halfPointSize, -halfPointSize),
                        Size(pointSize, pointSize)
                    )
                }

                drawRect(
                    color = Color.Black,
                    topLeft = p1 + Offset(-halfPointSize, -halfPointSize),
                    size = Size(pointSize, pointSize),
                    style = Stroke(6F)
                )
            }

            val debug = false
            if (debug) {
                touchPoints.forEachIndexed { index, offset ->
                    val prev = touchPoints.getOrNull(index - 1)
                    prev?.let {
                        drawLine(
                            Color.Blue,
                            it, offset
                        )
                    }
                }

                touchPoints.forEach {
                    drawCircle(
                        Color.Green,
                        5F, it
                    )
                }
            }
        }

        canvasState.pickedColorRect?.let { colorRect ->
            var size by remember { mutableStateOf(IntSize.Zero) }

            Popup(
                offset = colorRect.offset - IntOffset(
                    size.center.x,
                    with(density) { 30.dp.roundToPx() } + size.height)
            ) {
                Card(
                    Modifier.onSizeChanged { size = it }
                ) {
                    Row(
                        Modifier
                            .padding(8.dp)
                            .width(150.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (colorRect.color != EraseColor) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(colorRect.color))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("#" + Color(colorRect.color).toHex())
                        } else {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Transparent")
                            }
                        }
                    }
                }
            }
        }

        canvasState.moveCopy?.let {
            Popup(
                offset = with(density) {
                    IntOffset(0, -56.dp.roundToPx())
                }
            ) {
                Row {
                    ToolRow(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Tool({ canvasState.onMoveSelectedStop() }) {
                            Icon(Icons.Default.Check, null)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    ToolRow(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Tool({ canvasState.onMoveSelectedCancel() }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }
        }
    }
}