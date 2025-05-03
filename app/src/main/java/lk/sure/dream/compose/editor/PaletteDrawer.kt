package lk.sure.dream.compose.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import lk.sure.dream.compose.palettemanager.ColorPickerDialog
import lk.sure.dream.compose.palettemanager.Palette
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.viewmodels.EditorViewModel

@Composable
fun PaletteDrawer(
    onClose: () -> Unit,
    recentColors: List<Color>,
    vm: EditorViewModel,
) {
    LaunchedEffect(Unit) {
        vm.fetchPalettes()
    }

    val palettes by vm.palettes.collectAsState()
    var paletteToEdit by remember { mutableStateOf<ColorPalette?>(null) }

    var selectedIndex by remember { mutableIntStateOf(-1) }
    var addPickedColorToPalette by remember { mutableStateOf(false) }

    val recentColorPalette = remember(recentColors.size) {
        ColorPalette(
            "", "Recent Color", recentColors.asReversed().map { it.toArgb() }
        )
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(.8F)
    ) {
        if (palettes == null) {
            Box(
                modifier = Modifier.weight(1F),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Palette(
                        palette = recentColorPalette,
                        checked = selectedIndex == -1,
                        onCheckedChange = {
                            selectedIndex = -1
                            vm.changeMainPaletteToRecent()
                        },
                        onDuplicate = {
                            vm.duplicatePalette(recentColorPalette)
                        },
                    )
                }

                itemsIndexed(palettes!!) { index, palette ->
                    Palette(
                        palette = palette,
                        checked = selectedIndex == index,
                        onCheckedChange = {
                            selectedIndex = index
                            vm.changeMainPalette(palette)
                            if (addPickedColorToPalette) {
                                vm.changeShouldUpdatePalette(palette)
                            }
                        },
                        onPaletteEdit = {
                            paletteToEdit = palette
                        },
                        onDuplicate = {
                            vm.duplicatePalette(palette)
                        },
                        onDelete = {
                            vm.delete(palette)
                        },
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Add using colors")
                Checkbox(
                    checked = addPickedColorToPalette,
                    onCheckedChange = {
                        addPickedColorToPalette = it
                        if (it) {
                            vm.changeShouldUpdatePalette(palettes?.getOrNull(selectedIndex))
                        } else {
                            vm.changeShouldUpdatePalette(null)
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        paletteToEdit = ColorPalette("", "Palette", emptyList())
                    },
                    enabled = palettes != null,
                    modifier = Modifier.weight(1F)
                ) {
                    Icon(Icons.Default.Add, null)
                    Text("Create Palette")
                }

                Spacer(Modifier.width(4.dp))

                FilledIconButton(
                    onClick = onClose,
                ) {
                    Icon(Icons.Default.Close, null)
                }
            }
        }
    }

    paletteToEdit?.let { palette ->
        val isUpdateMode = palette.id.isNotEmpty()
        var name by remember(palette) { mutableStateOf(palette.name) }
        val colors = remember(palette) {
            mutableStateListOf(*palette.colors.map { Color(it) }.toTypedArray())
        }

        ColorPickerDialog(
            onConfirm = {
                if (isUpdateMode) {
                    vm.updatePaletteColors(palette.copy(name = name), colors)
                } else {
                    vm.createPalette(name, colors)
                }
                paletteToEdit = null
            },
            onDismiss = { paletteToEdit = null },
            onAddColor = { colors.add(it) },
            onRemoveColor = { colors.remove(it) },
            name = name,
            onNameChange = { name = it },
            colors = colors,
            isUpdateMode = isUpdateMode
        )
    }
}