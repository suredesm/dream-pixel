package lk.sure.dream.compose.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset

data class PickedColorRect(
    val pixel: Rect,
    val color: Int,
    val offset: IntOffset,
)
