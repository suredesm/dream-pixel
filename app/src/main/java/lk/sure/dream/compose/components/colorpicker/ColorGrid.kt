package lk.sure.dream.compose.components.colorpicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
fun ColorGrid(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    state: ColorGridState = rememberColorGridState(false),
    collapsedVisibleRows: Int = 1,
    minimumSize: Dp = 30.dp,
) {
    PrivateColorGrid(
        colors = colors,
        selectedIndex = null,
        onSelectedIndexChange = null,
        modifier = modifier,
        state = state,
        collapsedVisibleRows = collapsedVisibleRows,
        minimumSize = minimumSize,
    )
}

@Composable
fun ColorGrid(
    colors: List<Color>,
    selectedIndex: Int?,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    state: ColorGridState = rememberColorGridState(false),
    onColorLongClick: ((Color) -> Unit)? = null,
    collapsedVisibleRows: Int = 1,
    minimumSize: Dp = 30.dp,
) {
    PrivateColorGrid(
        colors = colors,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = onSelectedIndexChange as ((Int) -> Unit)?,
        modifier = modifier,
        state = state,
        onColorLongClick = onColorLongClick,
        collapsedVisibleRows = collapsedVisibleRows,
        minimumSize = minimumSize,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrivateColorGrid(
    colors: List<Color>,
    selectedIndex: Int?,
    onSelectedIndexChange: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    state: ColorGridState = rememberColorGridState(false),
    onColorLongClick: ((Color) -> Unit)? = null,
    minimumSize: Dp = 30.dp,
    collapsedVisibleRows: Int = 1,
    itemSpacing: Dp = 3.dp,
) {
    val density = LocalDensity.current
    var size by remember { mutableStateOf(minimumSize) }

    Box(
        modifier = modifier
            .height(
                if (state.expanded) {
                    (size + itemSpacing) * state.rowCount - itemSpacing
                } else {
                    (size + itemSpacing) * collapsedVisibleRows
                }
            )
    ) {
        if (colors.isEmpty()) return@Box

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minimumSize),
            modifier = Modifier.onSizeChanged {
                with(density) {
                    // If space is 0 and size is minSize
                    var count = (it.width / minimumSize.toPx()).toInt()
                    var remains = it.width - count * minimumSize.toPx()
                    var spaceCount = (remains / itemSpacing.toPx()).toInt()

                    while (spaceCount + 1 <= count) {
                        count--
                        remains += minimumSize.toPx()
                        spaceCount = (remains / itemSpacing.toPx()).toInt()
                    }

                    // Assuming Now count should be correct
                    val trueSize = (it.width - itemSpacing.toPx() * (count - 1)) / count
                    size = trueSize.toDp().coerceAtLeast(minimumSize)
                    state.rowCount = colors.size.let {
                        if (it == 0) 1
                        else ceil(it.toFloat() / count).toInt()
                    }
                }
            },
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            itemsIndexed(colors) { index, color ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1F)
                        .clip(RoundedCornerShape(3.dp))
                        .then(
                            if (index == selectedIndex)
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary)
                            else
                                Modifier
                        )
                        .background(color)
                        .then(
                            if (onSelectedIndexChange == null) Modifier
                            else {
                                Modifier.combinedClickable(
                                    onLongClick = {
                                        if (onColorLongClick != null) {
                                            onColorLongClick(color)
                                        }
                                    }
                                ) {
                                    onSelectedIndexChange(index)
                                }
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun rememberColorGridState(
    initialExpandedState: Boolean = false,
) = remember { ColorGridState(initialExpandedState) }

@Stable
class ColorGridState internal constructor(
    initialExpansion: Boolean,
) {
    fun toggleExpansion() {
        expanded = !expanded
    }

    var expanded by mutableStateOf(initialExpansion)
    var rowCount by mutableIntStateOf(1)
        internal set

    val expandable by derivedStateOf { rowCount > 1 }
}