package lk.sure.dream.compose.palettemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import lk.sure.dream.compose.components.colorpicker.ColorGrid
import lk.sure.dream.compose.components.colorpicker.ColorPicker
import lk.sure.dream.compose.components.colorpicker.rememberColorGridState
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.viewmodels.PaletteManagerViewModel

@Composable
fun PaletteManagerScreen(
    vm: PaletteManagerViewModel = hiltViewModel(),
) {
    val palettes by vm.palettes.collectAsState()

    var selectedPalette by remember { mutableStateOf<ColorPalette?>(null) }
    var deletePalette by remember { mutableStateOf<ColorPalette?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton({
                selectedPalette = ColorPalette("", "Palette", emptyList())
            }) {
                Icon(Icons.Default.Add, null)
                Text("New Palette")
            }
        }
    ) { scaffoldPadding ->

        PaletteManger(
            onPaletteEdit = {
                selectedPalette = it
            },
            onPaletteDuplicate = {
                vm.duplicatePalette(it)
            },
            onPaletteDelete = {
                deletePalette = it
            },
            palettes = palettes,
            modifier = Modifier
                .padding(scaffoldPadding)
                .padding(8.dp)
                .fillMaxSize()
        )

        selectedPalette?.let { palette ->
            val isUpdateMode = palette.id.isNotEmpty()

            var name by remember(palette) { mutableStateOf(palette.name) }

            val colors = remember(palette) {
                mutableStateListOf(*palette.colors.map { Color(it) }.toTypedArray())
            }

            ColorPickerDialog(
                onConfirm = {
                    if (isUpdateMode) {
                        vm.updatePaletteColors(palette, colors)
                    } else {
                        vm.createPalette(name, colors)
                    }
                    selectedPalette = null
                },
                onDismiss = { selectedPalette = null },
                onAddColor = { colors.add(it) },
                name = name,
                onNameChange = { name = it },
                colors = colors,
                isUpdateMode = isUpdateMode,
                onRemoveColor = { colors.remove(it) },
            )
        }

        deletePalette?.let {
            AlertDialog(
                onDismissRequest = { deletePalette = null },
                confirmButton = {
                    TextButton(onClick = {
                        vm.delete(it)
                        deletePalette = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletePalette = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Delete") },
                text = { Text("Do you want to delete ${it.name} palette?") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onAddColor: (Color) -> Unit,
    onRemoveColor: (Color) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    colors: List<Color>,
    isUpdateMode: Boolean,
) {
    var color by remember { mutableStateOf(colors.lastOrNull() ?: Color.Black) }
    val tooltipState = rememberTooltipState(initialIsVisible = false, isPersistent = true)
    val tooltipCoroutine = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onConfirm) { Text(if (isUpdateMode) "Update" else "Create") } },
        dismissButton = {
            TextButton(onDismiss) { Text("Discard") }
        },
        text = {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    trailingIcon = { Icon(Icons.Default.Edit, null) },
                    singleLine = true,
                )

                ColorPicker(
                    color = color,
                    onColorChange = { color = it },
                    colorList = colors,
                    modifier = Modifier.weight(1F, false),
                    onColorLongClick = onRemoveColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(
                                    "Long click on a color to remove from palette",
                                    modifier = Modifier.width(100.dp)
                                )
                            }
                        },
                        state = tooltipState,
                    ) {
                        OutlinedIconButton(
                            onClick = {
                                tooltipCoroutine.launch {
                                    tooltipState.show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.QuestionMark, null)
                        }
                    }

                    OutlinedButton({ onAddColor(color) }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Color")
                    }
                }
            }
        }
    )
}

@Composable
fun PaletteManger(
    onPaletteEdit: (ColorPalette) -> Unit,
    onPaletteDuplicate: (ColorPalette) -> Unit,
    onPaletteDelete: (ColorPalette) -> Unit,
    palettes: List<ColorPalette>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(palettes) { palette ->
            Palette(
                palette = palette,
                onPaletteEdit = { onPaletteEdit(palette) },
                onDuplicate = { onPaletteDuplicate(palette) },
                onDelete = { onPaletteDelete(palette) },
                modifier = Modifier,
            )
        }

        item {
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun Palette(
    palette: ColorPalette,
    modifier: Modifier = Modifier,
    onPaletteEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp, 8.dp),
) {
    Card(
        modifier = modifier,
        shape = CardDefaults.shape,
    ) {
        Palette(
            palette = palette,
            onEdit = onPaletteEdit,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            contentPadding = contentPadding
        )
    }
}

@Composable
fun Palette(
    palette: ColorPalette,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onPaletteEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp, 8.dp),
) {
    val color =
        if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant

    Card(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        shape = CardDefaults.shape,
        border = BorderStroke(3.dp, color),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawWithContent {
                            drawPath(
                                Path().apply {
                                    lineTo(size.width, 0F)
                                    lineTo(size.width, size.height)
                                    close()
                                },
                                color
                            )
                            drawContent()
                        },
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Palette(
                palette = palette,
                onEdit = onPaletteEdit,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun Palette(
    palette: ColorPalette,
    onEdit: (() -> Unit)?,
    onDuplicate: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    contentPadding: PaddingValues = PaddingValues(16.dp, 8.dp),
) {
    val gridState = rememberColorGridState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(palette.name, style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = { gridState.toggleExpansion() },
                enabled = gridState.expandable
            ) {
                Icon(
                    if (gridState.run { expanded && expandable }) Icons.Default.ArrowDropUp
                    else Icons.Default.ArrowDropDown,
                    null
                )
            }
        }

        ColorGrid(
            colors = palette.colors.map { Color(it) },
            modifier = Modifier.fillMaxWidth(),
            state = gridState,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (onDelete != null) {
                FilledIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null)
                }
            }

            if (onDuplicate != null) {
                FilledIconButton(
                    onClick = onDuplicate,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                }
            }


            if (onEdit != null) {
                FilledIconButton(
                    onClick = onEdit
                ) {
                    Icon(Icons.Default.Edit, null)
                }
            }
        }
    }
}