package lk.sure.dream.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpriteGrid(
    modifier: Modifier = Modifier,
    spriteList: List<SpriteItemUiState>,
    onItemClick: (SpriteItemUiState) -> Unit,
    onItemRename: (SpriteItemUiState) -> Unit,
    onItemDuplicate: (SpriteItemUiState) -> Unit,
    onItemMove: (SpriteItemUiState) -> Unit,
    onItemDelete: (SpriteItemUiState) -> Unit,
) {
    var cellSize by remember { mutableStateOf(1.dp) }
    val density = LocalDensity.current

    LazyVerticalGrid(
        columns = GridCells.FixedSize(cellSize),
        modifier = modifier
            .onSizeChanged {
                cellSize = with(density) { (it.width / 3F).toDp() - 8.dp }.coerceAtLeast(1.dp)
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(spriteList, key = { it.id }) {
            var menuExpanded by remember { mutableStateOf(false) }

            MenuContainer(
                menuExpanded = menuExpanded,
                onMenuDismissRequest = { menuExpanded = false },
                onRename = {
                    onItemRename(it)
                },
                onDuplicate = {
                    onItemDuplicate(it)
                },
                onMove = {
                    onItemMove(it)
                },
                onDelete = {
                    onItemDelete(it)
                },
                modifier = if (cellSize == 1.dp) Modifier else Modifier.animateItem()
            ) {
                SpriteItem(
                    sprite = it,
                    thumbnail = it.thumbnail,
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = { menuExpanded = true },
                            onClick = { onItemClick(it) }
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}