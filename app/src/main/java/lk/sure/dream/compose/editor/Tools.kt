package lk.sure.dream.compose.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Rectangle
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import lk.sure.dream.R
import lk.sure.dream.compose.components.canvas.EditMode
import lk.sure.dream.compose.components.canvas.Selection
import lk.sure.dream.ui.graphics.BubbleShape

sealed class ToolState

class NonExpandableToolState(
    val icon: ImageVector,
    val contentDescription: String? = null,
    val tint: Color? = null,
    val isSelected: () -> Boolean = { false },
    val enabled: Boolean = true,
    val onClick: () -> Unit = {},
) : ToolState()

class ExpandableToolState(
    val options: List<NonExpandableToolState>,
    val isSelected: () -> Boolean = { false },
    val enabled: Boolean = true,
) : ToolState() {
    constructor(
        vararg options: NonExpandableToolState,
        isSelected: () -> Boolean = { false },
    ) : this(listOf(*options), isSelected)
}

@Composable
fun DrawTools(
    editMode: EditMode,
    onEditModeChange: (EditMode) -> Unit,
    penColor: Color,
    onColorToolClick: () -> Unit,
    shouldFit: Boolean,
    onFitToolClick: () -> Unit,
) {
    val drawTools = listOf(
        ExpandableToolState(
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.stylus_pen_24dp),
                isSelected = { editMode == EditMode.Draw }
            ) {
                onEditModeChange(EditMode.Draw)
            },
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.ink_eraser_24dp),
                isSelected = { editMode == EditMode.Erase }
            ) {
                onEditModeChange(EditMode.Erase)
            },
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.colors_24dp),
                isSelected = { editMode == EditMode.Fill }
            ) {
                onEditModeChange(EditMode.Fill)
            }
        ) {
            listOf(EditMode.Draw, EditMode.Erase, EditMode.Fill).any { editMode == it }
        },
        ExpandableToolState(
            NonExpandableToolState(
                icon = Icons.Default.HorizontalRule,
                isSelected = { editMode == EditMode.Line }
            ) {
                onEditModeChange(EditMode.Line)
            },
            NonExpandableToolState(
                icon = Icons.Outlined.Rectangle,
                isSelected = { editMode == EditMode.OutlinedRectangle }
            ) {
                onEditModeChange(EditMode.OutlinedRectangle)
            },
            NonExpandableToolState(
                icon = Icons.Filled.Rectangle,
                isSelected = { editMode == EditMode.FilledRectangle }
            ) {
                onEditModeChange(EditMode.FilledRectangle)
            },
            NonExpandableToolState(
                icon = Icons.Outlined.Circle,
                isSelected = { editMode == EditMode.OutlinedCircle }
            ) {
                onEditModeChange(EditMode.OutlinedCircle)
            },
            NonExpandableToolState(
                icon = Icons.Filled.Circle,
                isSelected = { editMode == EditMode.FilledCircle }
            ) {
                onEditModeChange(EditMode.FilledCircle)
            },
        ) {
            listOf(
                EditMode.Line,
                EditMode.OutlinedRectangle,
                EditMode.FilledRectangle,
                EditMode.OutlinedCircle,
                EditMode.FilledCircle
            ).any { editMode == it }
        },
        NonExpandableToolState(
            icon = ImageVector.vectorResource(R.drawable.color),
            tint = penColor,
        ) { onColorToolClick() },
        NonExpandableToolState(
            ImageVector.vectorResource(R.drawable.baseline_fit_screen_24),
            enabled = shouldFit
        ) { onFitToolClick() }
    )

    Tools(drawTools)
}

@Composable
fun SelectionTools(
    editMode: EditMode,
    onEditModeChange: (EditMode) -> Unit,
    mode: Selection.Mode,
    onSelectionModeChange: (Selection.Mode) -> Unit,
    onInvertSelection: () -> Unit,
    onSelectionClear: () -> Unit,
) {
    val selectionTools = listOf(
        ExpandableToolState(
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.select_24dp),
                isSelected = { editMode == EditMode.Selection }
            ) {
                onEditModeChange(EditMode.Selection)
            },
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.lasso_select_24dp),
                isSelected = { editMode == EditMode.Lasso }
            ) {
                onEditModeChange(EditMode.Lasso)
            },
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.select_by_color),
                isSelected = { editMode == EditMode.ColorSelection }
            ) {
                onEditModeChange(EditMode.ColorSelection)
            }
        ) {
            listOf(
                EditMode.Selection,
                EditMode.Lasso,
                EditMode.ColorSelection
            ).any { editMode == it }
        },
        ExpandableToolState(
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.select_24dp),
                isSelected = { mode == Selection.Mode.Replace }
            ) {
                onSelectionModeChange(Selection.Mode.Replace)
            },
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.selection_add),
                isSelected = { mode == Selection.Mode.Addition }
            ) {
                onSelectionModeChange(Selection.Mode.Addition)
            },
            NonExpandableToolState(
                icon = ImageVector.vectorResource(R.drawable.selection_minus),
                isSelected = { mode == Selection.Mode.Subtraction }
            ) {
                onSelectionModeChange(Selection.Mode.Subtraction)
            }
        ),
        NonExpandableToolState(
            icon = ImageVector.vectorResource(R.drawable.selection_invert)
        ) { onInvertSelection() },
        NonExpandableToolState(
            ImageVector.vectorResource(R.drawable.remove_selection_24dp)
        ) { onSelectionClear() }
    )

    Tools(selectionTools)
}

@Composable
fun UndoRedoTools(
    undo: () -> Unit,
    redo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
) {
    val undoRedo = listOf(
        NonExpandableToolState(
            Icons.AutoMirrored.Default.Undo,
            enabled = canUndo
        ) { undo() },

        NonExpandableToolState(
            Icons.AutoMirrored.Default.Redo,
            enabled = canRedo
        ) { redo() },
    )

    Tools(undoRedo)
}

@Composable
fun ActionTools(
    onPreviewClick: () -> Unit,
    onSaveClick: (() -> Unit)?,
    onSaveAsClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    val actions = listOf(
        NonExpandableToolState(
            icon = Icons.Default.Preview,
            onClick = onPreviewClick
        ),
        NonExpandableToolState(
            icon = Icons.Default.Save,
            onClick = onSaveClick ?: {},
            enabled = onSaveClick != null
        ),
        NonExpandableToolState(
            icon = Icons.Default.SaveAs,
            onClick = onSaveAsClick
        ),
        NonExpandableToolState(
            icon = Icons.Default.IosShare,
            onClick = onExportClick
        ),
    )

    Tools(actions)
}

@Composable
fun Tools(
    tools: List<ToolState>,
) {
    ToolRow {
        tools.forEach { state ->
            when (state) {
                is ExpandableToolState -> {
                    var expanded by remember { mutableStateOf(false) }
                    var selectedOption by remember { mutableIntStateOf(0) }

                    ExpandableTool(
                        menuItems = {
                            ToolRow(
                                shape = it?.run {
                                    BubbleShape(4.dp, 6.dp, toOffset())
                                } ?: RoundedCornerShape(4.dp),
                                shadowElevation = 8.dp,
                            ) {
                                state.options.forEachIndexed { index, subState ->
                                    Tool(
                                        onClick = {
                                            selectedOption = index
                                            subState.onClick()
                                            expanded = false
                                        },
                                        selected = subState.isSelected(),
                                        enabled = subState.enabled
                                    ) {
                                        Icon(
                                            subState.icon,
                                            subState.contentDescription
                                        )
                                    }
                                }
                            }
                        },
                        menuExpanded = expanded,
                        onMenuExpanded = { expanded = it },
                        enabled = state.enabled,
                        selected = state.isSelected(),
                        onClick = {
                            state.options.getOrNull(selectedOption)?.onClick?.let { it() }
                        },
                    ) {
                        state.options.getOrNull(selectedOption)?.let {
                            Icon(it.icon, null)
                        }
                    }
                }

                is NonExpandableToolState -> Tool(
                    state.onClick,
                    enabled = state.enabled,
                    content = {
                        Icon(
                            imageVector = state.icon,
                            contentDescription = state.contentDescription,
                            tint = state.tint ?: LocalContentColor.current
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun ToolRow(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    colors: CardColors = CardDefaults.cardColors(),
    shadowElevation: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        elevation = CardDefaults.cardElevation(shadowElevation),
        colors = colors
    ) {
        Row {
            content()
        }
    }
}

@Composable
fun ToolColumn(
    shape: Shape = RoundedCornerShape(4.dp),
    shadowElevation: Dp = 0.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        elevation = CardDefaults.cardElevation(shadowElevation),
    ) {
        Column {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tool(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val defaultIconColor =
        MaterialTheme.colorScheme.run { if (selected) onPrimary else onSurfaceVariant }

    Surface(
        modifier = modifier
            .drawWithContent {
                drawContent()
                onLongClick?.let {
                    drawPath(
                        Path().apply {
                            moveTo(size.width, size.height)
                            val size = 8.dp.toPx()

                            relativeLineTo(-size, 0F)
                            relativeLineTo(size, -size)
                            close()
                        },
                        defaultIconColor
                    )
                }
            }
            .size(48.dp)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick
            ),
        color = if (!enabled)
            IconButtonDefaults.filledIconButtonColors().disabledContainerColor
        else if (selected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun ExpandableTool(
    menuExpanded: Boolean,
    onMenuExpanded: (Boolean) -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    menuItems: @Composable (pointTo: IntOffset?) -> Unit,
    content: @Composable () -> Unit,
) {
    Tool(
        onClick = onClick,
        onLongClick = { onMenuExpanded(true) },
        enabled = enabled,
        selected = selected,
        modifier = modifier,
    ) {
        content()

        ToolMenu(
            expanded = menuExpanded,
            onDismissRequest = { onMenuExpanded(false) }
        ) {
            menuItems(it)
        }
    }
}

@Composable
fun ToolMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable (pointTo: IntOffset?) -> Unit,
) {
    if (expanded) {
        var pointTo by remember { mutableStateOf<IntOffset?>(null) }

        val popupPositionProvider = remember {
            ToolMenuPositionProvider {
                pointTo = it
            }
        }

        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true, clippingEnabled = true)
        ) {
            Column {
                content(pointTo)

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

class ToolMenuPositionProvider(
    val onPositioned: (pointTo: IntOffset) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {

        var pos = anchorBounds.center.run {
            copy(y = anchorBounds.top - popupContentSize.height)
        } - popupContentSize.center.copy(y = 0)

        if (pos.x < 0) {
            pos = pos.copy(x = 0)
        } else if (pos.x + popupContentSize.width >= windowSize.width) {
            pos = pos.copy(windowSize.width - popupContentSize.width)
        }

        onPositioned(
            anchorBounds.center - pos
        )

        return pos
    }
}