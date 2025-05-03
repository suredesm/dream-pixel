package lk.sure.dream.compose.editor

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import lk.sure.dream.compose.components.canvas.Frame

private val fpsList = listOf(1, 6, 12, 24, 30)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewWindow(
    sequence: List<Frame>,
    frameAspectRatio: Float,
    offset: () -> Offset,
    onDrag: (dragAmount: Offset) -> Unit,
    dragArea: IntRect,
    restFrameIndex: Int? = null,
) {
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var currentFrame by remember { mutableStateOf(sequence.first().image.asImageBitmap()) }
    var playState by remember { mutableStateOf(true) }
    var showPlayButton by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var fps by remember { mutableIntStateOf(24) }
    var delayMills by remember { mutableLongStateOf((1000 / fps).toLong()) }

    var windowSize by remember { mutableStateOf(IntSize.Zero) }

    val cornerRadius = 8.dp
    val framePadding = 3.dp

    LaunchedEffect(playState, sequence.size, sequence[currentFrameIndex]) {
        if (playState) {
            while (true) {
                delay(delayMills)
                currentFrameIndex = (currentFrameIndex + 1) % sequence.size
                currentFrame = sequence[currentFrameIndex].image.asImageBitmap()
            }
        }
    }

    LaunchedEffect(
        playState,
        sequence.size,
        restFrameIndex,
        restFrameIndex?.let { sequence[it] }?.image
    ) {
        if (!playState && restFrameIndex != null) {
            currentFrame = sequence[restFrameIndex].image.asImageBitmap()
        }
    }

    LaunchedEffect(playState) {
        showPlayButton = true
        delay(750)
        showPlayButton = false
    }

    Card(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .onSizeChanged { windowSize = it },
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(offset) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            var realX = dragAmount.x
                            var realY = dragAmount.y

                            (offset().x + dragAmount.x).also { left ->
                                if (left < 0) {
                                    realX = offset().x
                                } else if (left + windowSize.width >= dragArea.width) {
                                    realX = dragArea.width - offset().x - windowSize.width
                                }
                            }

                            (offset().y + dragAmount.y).also { top ->
                                if (top < 0) {
                                    realY = offset().y
                                } else if (top + windowSize.height >= dragArea.height) {
                                    realY = dragArea.height - offset().y - windowSize.height
                                }
                            }

                            onDrag(Offset(realX, realY))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {

                Icon(Icons.Default.DragHandle, "Drag Window")
                Text(
                    "$fps",
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .padding(framePadding)
                    .width(100.dp)
                    .clip(RoundedCornerShape(cornerRadius - framePadding))
                    .background(Color.White)
                    .aspectRatio(frameAspectRatio)
                    .combinedClickable(
                        onLongClick = { menuExpanded = true }
                    ) { playState = !playState },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    currentFrame,
                    null,
                    modifier = Modifier.fillMaxSize(),
                    filterQuality = FilterQuality.None
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showPlayButton,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(Icons.Default.run { if (playState) Pause else PlayArrow }, null)
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    fpsList.forEach {
                        DropdownMenuItem(
                            text = { Text("$it fps") },
                            onClick = {
                                fps = it
                                delayMills = (1000F / it).toLong()
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}