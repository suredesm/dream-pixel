package lk.sure.dream.compose.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lk.sure.dream.R
import lk.sure.dream.compose.components.canvas.EditMode
import lk.sure.dream.compose.components.canvas.EditorCanvas
import lk.sure.dream.compose.components.canvas.Frame
import lk.sure.dream.compose.components.canvas.Layer
import lk.sure.dream.compose.components.canvas.OnionSkinSettingsDialog
import lk.sure.dream.compose.components.canvas.overlay
import lk.sure.dream.compose.components.colorpicker.ColorGrid
import lk.sure.dream.compose.components.colorpicker.ColorPickerDialog
import lk.sure.dream.ui.graphics.BubbleShape
import lk.sure.dream.viewmodels.EditorViewModel
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    vm: EditorViewModel = hiltViewModel(),
    goBack: () -> Boolean,
    spriteId: String,
) {
    LaunchedEffect(Unit) {
        vm.restoreState(spriteId)
    }

    val isLoading by vm.isLoading.collectAsState()
    val framesBitmaps by vm.frameBitmaps.collectAsState()
    val spriteName by vm.spriteName.collectAsState()

    if (isLoading) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val canvasState = vm.canvasState

    var showColorPicker by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var color by remember(canvasState.penColor) { mutableStateOf(Color(canvasState.penColor)) }

    var showSaveConfirmation by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }

    val snackbarScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showPreviewWindow by remember { mutableStateOf(false) }
    var previewWindowOffset by remember { mutableStateOf(Offset.Zero) }

    var previewWindowRomeSize by remember { mutableStateOf(IntSize.Zero) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val mainColorPalette by vm.mainPalette.collectAsState()

    val allFramesSaved = canvasState.allFramesSaved

    var selectedColorIndex by remember {
        mutableStateOf(
            mainColorPalette
                .indexOf(Color(canvasState.penColor))
                .takeUnless { it == -1 }
        )
    }

    var showOnionSettings by remember { mutableStateOf(false) }

    LaunchedEffect(canvasState.penColor) {
        vm.addToRecentPalette(Color(canvasState.penColor))
        selectedColorIndex = mainColorPalette
            .indexOf(Color(canvasState.penColor))
            .takeUnless { it == -1 }
    }

    BackHandler {
        if (allFramesSaved) {
            goBack()
            vm.saveOnlyRecentColors(spriteId)
        } else {
            showSaveConfirmation = true
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            PaletteDrawer(
                onClose = {
                    snackbarScope.launch {
                        drawerState.close()
                    }
                },
                recentColors = canvasState.recentColors,
                vm = vm
            )
        },
        drawerState = drawerState,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { scaffoldPadding ->
            Box(
                modifier = Modifier
                    .padding(scaffoldPadding)
                    .onSizeChanged { previewWindowRomeSize = it },
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ActionTools(
                            onPreviewClick = { showPreviewWindow = !showPreviewWindow },
                            onSaveClick = if (allFramesSaved) {
                                null
                            } else {
                                {
                                    vm.saveState(spriteId)
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Saved",
                                            actionLabel = "OK",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onSaveAsClick = {
                                showSaveAsDialog = true
                            },
                            onExportClick = {
                                showExport = true
                                vm.loadBitmapFrames()
                            }
                        )
                    }

                    Spacer(Modifier.weight(1F))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        ToolRow {
                            Tool(
                                onClick = {
                                    canvasState.onionSkinSettings.enabled =
                                        !canvasState.onionSkinSettings.enabled
                                },
                                onLongClick = { showOnionSettings = true },
                                selected = canvasState.onionSkinSettings.enabled,
                            ) {
                                Icon(Icons.Default.Animation, null)
                            }
                        }

                        ToolRow {
                            Tool(
                                onClick = { canvasState.copySelectionToClipboard() }
                            ) {
                                Icon(Icons.Default.ContentCopy, null)
                            }

                            Tool(
                                onClick = { canvasState.copySelectionToClipboard(true) },
                            ) {
                                Icon(Icons.Default.ContentCut, null)
                            }

                            Tool(
                                onClick = { canvasState.pasteFromClipboard() },
                                enabled = canvasState.canPaste
                            ) {
                                Icon(Icons.Default.ContentPaste, null)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ToolRow {
                            Tool(
                                onClick = { canvasState.changeEditMode(EditMode.PickColor) },
                                selected = canvasState.editMode == EditMode.PickColor
                            ) {
                                Icon(Icons.Default.Colorize, null)
                            }

                            Tool(
                                onClick = { canvasState.onMoveSelectedStart() },
                                selected = canvasState.editMode == EditMode.MoveSelected
                            ) {
                                Icon(Icons.Default.OpenWith, null)
                            }
                        }

                        SelectionTools(
                            editMode = canvasState.editMode,
                            onEditModeChange = { canvasState.changeEditMode(it) },
                            mode = canvasState.selectionMode,
                            onSelectionModeChange = canvasState::setSelectionMode,
                            onInvertSelection = canvasState::invertSelection,
                            onSelectionClear = canvasState::clearSelection
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        UndoRedoTools(
                            undo = canvasState::undo,
                            redo = canvasState::redo,
                            canUndo = canvasState.canUndo,
                            canRedo = canvasState.canRedo,
                        )

                        DrawTools(
                            editMode = canvasState.editMode,
                            onEditModeChange = { canvasState.changeEditMode(it) },
                            onColorToolClick = { showColorPicker = true },
                            shouldFit = canvasState.shouldFit,
                            onFitToolClick = {
                                canvasState.fitCanvas()
                            },
                            penColor = Color(canvasState.penColor),
                        )
                    }

                    EditorCanvas(
                        canvasState = canvasState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1F)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ColorGrid(
                            colors = mainColorPalette,
                            selectedIndex = selectedColorIndex,
                            onSelectedIndexChange = {
                                val selectedColor = mainColorPalette[it]
                                canvasState.penColor = selectedColor.toArgb()
                            },
                            collapsedVisibleRows = 2,
                        )
                    }


                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolColumn {
                            Tool({ showLayersSheet = true }) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.onSurface) {
                                            Text("${canvasState.activeLayerIndex + 1}")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Layers, null)
                                }
                            }

                            Tool(onClick = {
                                snackbarScope.launch {
                                    drawerState.open()
                                }
                            }) {
                                Icon(Icons.Default.ColorLens, null)
                            }
                        }

                        PreviewSlider(
                            frames = canvasState.frames,
                            selectedFrameIndex = canvasState.activeFrameIndex,
                            onFrameChange = canvasState::onActiveFrameIndexChange,
                            modifier = Modifier.weight(1F),
                            canDelete = canvasState.frames.size > 1,
                            onDeleteFrame = canvasState::removeFrame,
                            onAddFrame = canvasState::addFrame,
                            onSwapFrame = canvasState::swapFrames,
                            canSwap = canvasState::isFrameExists
                        )

                        ToolColumn {
                            Tool(onClick = { canvasState.duplicateActiveFrame() }) {
                                Icon(painterResource(R.drawable.duplicate_frame_right), null)
                            }

                            Tool(canvasState::addFrame) {
                                Icon(painterResource(R.drawable.add_last_frame), null)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showPreviewWindow,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .offset {
                            previewWindowOffset.let {
                                IntOffset(
                                    it.x.toInt(),
                                    it.y.toInt()
                                )
                            }
                        }
                ) {
                    PreviewWindow(
                        sequence = canvasState.frames,
                        frameAspectRatio = canvasState.canvasRatio,
                        offset = { previewWindowOffset },
                        onDrag = { previewWindowOffset += it },
                        dragArea = IntRect(IntOffset.Zero, previewWindowRomeSize),
                        restFrameIndex = canvasState.activeFrameIndex
                    )
                }

            }

            if (showColorPicker) {
                var confirm by remember { mutableStateOf(false) }

                LaunchedEffect(confirm) {
                    if (confirm) {
                        delay(700)
                        confirm = false
                    }
                }

                ColorPickerDialog(
                    color = color,
                    onColorChange = { color = it },
                    onDismiss = { showColorPicker = false },
                    onConfirm = {
                        showColorPicker = false
                        canvasState.penColor = color.toArgb()
                    },
                    recentColors = canvasState.recentColors,
                    onClearAllColors = {
                        if (!confirm) {
                            confirm = true
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("Click again to clear all recent colors")
                            }
                        } else {
                            canvasState.removeAllRecentColors()
                            confirm = false
                        }
                    }
                )
            }

            if (showLayersSheet) {
                BottomSheet(
                    layers = canvasState.activeFrameLayers,
                    activeLayerIndex = canvasState.activeLayerIndex,
                    onActiveLayerIndexChange = canvasState::setActiveLayer,
                    onAddLayer = canvasState::addLayer,
                    onDismissRequest = { showLayersSheet = false },
                    isLayerVisible = canvasState::isLayerVisible,
                    onVisibilityChange = canvasState::setLayerVisibility,
                    moveLayer = canvasState::moveLayer,
                    onNameChange = canvasState::setLayerName,
                    onDeleteLayer = canvasState::removeLayer,
                    onMergeDownLayer = canvasState::mergeDownLayer,
                )
            }

            if (showSaveConfirmation) {
                AlertDialog(
                    onDismissRequest = { showSaveConfirmation = false },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.saveState(spriteId)
                            showSaveConfirmation = false
                            goBack()
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showSaveConfirmation = false
                        }) { Text("Cancel") }

                        TextButton(onClick = {
                            showSaveConfirmation = false
                            vm.saveOnlyRecentColors(spriteId)
                            goBack()
                        }) { Text("Discard") }
                    },
                    title = { Text("Close") },
                    text = { Text("Do you want to save your work?") }
                )
            }
        }
    }

    if (showExport) {
        ExportDialog(
            onConfirm = { exportAsStrip, selectedFrames, uri, fileName ->
                vm.export(exportAsStrip, selectedFrames, fileName, uri)
                showExport = false
            },
            onCancel = {
                showExport = false
                vm.freeBitmapFrames()
            },
            frames = framesBitmaps,
            fileName = spriteName
        )
    }

    if (showSaveAsDialog) {
        spriteName?.let { name ->
            var progress by remember { mutableStateOf(false) }

            SaveAsDialog(
                initialSpriteName = name,
                onConfirm = { spriteName, description ->
                    progress = true
                    vm.saveAsSprite(spriteId, spriteName, description) {
                        showSaveAsDialog = false
                    }
                },
                onDismiss = {
                    showSaveAsDialog = false
                },
                taskProgress = progress,
            )
        }
    }

    if (showOnionSettings) {
        OnionSkinSettingsDialog(
            onionSkinSettings = canvasState.onionSkinSettings,
            onDismissRequest = { showOnionSettings = false },
            modifier = Modifier
        )
    }
}

@Composable
fun PreviewSlider(
    frames: List<Frame>,
    selectedFrameIndex: Int,
    onFrameChange: (Int) -> Unit,
    canDelete: Boolean,
    onDeleteFrame: (frameIndex: Int) -> Unit,
    onAddFrame: (at: Int) -> Unit,
    canSwap: (index: Int) -> Boolean,
    onSwapFrame: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
    ) {
        itemsIndexed(frames) { idx, frame ->
            PreviewFrame(
                bitmap = frame.image,
                selected = idx == selectedFrameIndex,
                canDelete = canDelete,
                onDeleteFrame = { onDeleteFrame(idx) },
                onAddFrameBefore = { onAddFrame(idx) },
                onAddFrameAfter = { onAddFrame(idx + 1) },
                onSwapFrameBefore = { onSwapFrame(idx, idx - 1) },
                onSwapFrameAfter = { onSwapFrame(idx, idx + 1) },
                canSwapBack = canSwap(idx - 1),
                canSwapForward = canSwap(idx + 1),
                onClick = { onFrameChange(idx) }
            )
        }
    }
}

@Composable
fun PreviewFrame(
    bitmap: Bitmap,
    selected: Boolean,
    canDelete: Boolean,
    onDeleteFrame: () -> Unit,
    onAddFrameBefore: () -> Unit,
    onAddFrameAfter: () -> Unit,
    onSwapFrameBefore: () -> Unit,
    onSwapFrameAfter: () -> Unit,
    canSwapBack: Boolean,
    canSwapForward: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .size(80.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { expanded = true },
                    onTap = { onClick() },
                )
            }
            .then(
                if (selected)
                    Modifier.border(3.dp, Color(0xFF00AAFF), shape)
                else Modifier
            ),
        shape = shape
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            filterQuality = FilterQuality.None
        )

        ToolMenu(
            expanded,
            { expanded = false }
        ) {
            ToolRow(
                shape = it?.run {
                    BubbleShape(4.dp, 6.dp, toOffset())
                } ?: RoundedCornerShape(4.dp),
                shadowElevation = 8.dp
            ) {
                Tool(
                    onClick = {
                        onSwapFrameBefore()
                        expanded = false
                    },
                    enabled = canSwapBack
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        null
                    )
                }

                Tool(
                    onClick = {
                        onAddFrameBefore()
                        expanded = false
                    }) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.add_frame_left),
                        null
                    )
                }

                Tool(
                    onClick = {
                        showDeleteConfirmation = true
                        expanded = false
                    },
                    enabled = canDelete
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null
                    )
                }

                Tool(
                    onClick = {
                        onAddFrameAfter()
                        expanded = false
                    }) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.add_frame_right),
                        null
                    )
                }
                Tool(
                    onClick = {
                        onSwapFrameAfter()
                        expanded = false
                    }, enabled = canSwapForward
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowRight,
                        null
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            confirmButton = {
                TextButton({
                    onDeleteFrame()
                    showDeleteConfirmation = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton({ showDeleteConfirmation = false }) { Text("Cancel") }
            },
            title = { Text("Delete Frame") },
            text = { Text("Do you want to delete the frame?") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    layers: List<Layer>,
    activeLayerIndex: Int,
    onActiveLayerIndexChange: (Int) -> Unit,
    onAddLayer: () -> Unit,
    onDismissRequest: () -> Unit,
    isLayerVisible: (layerIndex: Int) -> Boolean,
    onVisibilityChange: (layerIndex: Int, visible: Boolean) -> Unit,
    moveLayer: (from: Int, to: Int) -> Unit,
    onNameChange: (layerIndex: Int, name: TextFieldValue) -> Unit,
    onDeleteLayer: (layerIndex: Int) -> Unit,
    onMergeDownLayer: (layerIndex: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.weight(1F, false),
            verticalArrangement = Arrangement.Bottom,
            reverseLayout = true,
            state = listState
        ) {
            itemsIndexed(layers, key = { _, l -> l.key }) { index, layer ->

                LayerItem(
                    name = layer.name,
                    onNameChange = { onNameChange(index, it) },
                    isActive = { activeLayerIndex == it },
                    onClick = { onActiveLayerIndexChange(it) },
                    visible = isLayerVisible(index),
                    toggleVisibility = { onVisibilityChange(index, it) },
                    thumbnail = layer.asBitmap().asImageBitmap(),
                    index = index,
                    moveLayerUp = {
                        (index + 1)
                            .takeIf { it != layers.size }
                            ?.let {
                                moveLayer(index, it)
                            }
                    },
                    moveLayerDown = {
                        (index - 1)
                            .takeIf { it != -1 }
                            ?.let {
                                moveLayer(index, it)
                            }
                    },
                    canDelete = layers.size > 1,
                    onDelete = onDeleteLayer,
                    onMergeDown = onMergeDownLayer,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = onAddLayer
            ) {
                Icon(Icons.Default.Add, null)
                Text("Add Layer")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyItemScope.LayerItem(
    name: TextFieldValue,
    onNameChange: (TextFieldValue) -> Unit,
    isActive: (Int) -> Boolean,
    onClick: (Int) -> Unit,
    visible: Boolean,
    toggleVisibility: (Boolean) -> Unit,
    thumbnail: ImageBitmap,
    index: Int,
    moveLayerUp: () -> Unit,
    moveLayerDown: () -> Unit,
    canDelete: Boolean,
    onDelete: (layerIndex: Int) -> Unit,
    onMergeDown: (layerIndex: Int) -> Unit,
) {
    val currentIndex by rememberUpdatedState(index)
    val selectedColor = BottomSheetDefaults.ContainerColor overlay Color(0x3300AAFF)

    RevealOptions(
        canDelete = canDelete,
        onDelete = { onDelete(currentIndex) },
        canMergeDown = currentIndex != 0,
        onMergeDown = { onMergeDown(currentIndex) },
        modifier = Modifier
            .animateItem()
    ) {
        Row(
            modifier = Modifier
                .background(
                    if (isActive(currentIndex)) selectedColor
                    else BottomSheetDefaults.ContainerColor
                )
                .clickable { onClick(currentIndex) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .padding(4.dp)
                    .size(80.dp)
                    .background(Color.White),
                filterQuality = FilterQuality.None
            )

            EditableText(
                name,
                onNameChange,
                modifier = Modifier.weight(1F)
            )

            IconToggleButton(
                checked = visible,
                onCheckedChange = toggleVisibility
            ) {
                Icon(
                    if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    "Visibility"
                )
            }

            IconButton(
                onClick = moveLayerUp
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    "Bring Layer Up"
                )
            }

            IconButton(
                onClick = moveLayerDown
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    "Bring Layer Down"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditableText(
    textValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(imeVisible) {
        if (!imeVisible) focusManager.clearFocus()
    }

    OutlinedTextField(
        textValue,
        onTextChange,
        modifier = modifier.onFocusChanged {
            if (it.hasFocus) onTextChange(
                textValue.copy(
                    selection = TextRange(0, textValue.text.length)
                )
            )
        },
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        singleLine = true
    )
}

@Composable
fun RevealOptions(
    canDelete: Boolean,
    onDelete: () -> Unit,
    canMergeDown: Boolean,
    onMergeDown: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    var offset by remember { mutableFloatStateOf(0F) }
    var optionsWidth by remember { mutableIntStateOf(0) }
    var isDragged by remember { mutableStateOf(false) }
    val animateOffset by animateFloatAsState(
        targetValue = offset,
        label = "Option Animation"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragged = true },
                    onDragEnd = {
                        offset = (offset / optionsWidth).roundToInt() * optionsWidth.toFloat()
                        isDragged = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offset = (offset + dragAmount)
                        .coerceIn(0F, optionsWidth.toFloat())
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .onSizeChanged { optionsWidth = it.width }
        ) {
            FilledIconButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(0),
                enabled = canDelete
            ) {
                Icon(Icons.Default.Delete, "Delete")
            }

            FilledIconButton(
                onClick = onMergeDown,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(0),
                enabled = canMergeDown
            ) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowDown,
                    "Merge Down"
                )
            }
        }

        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        if (isDragged) {
                            offset.toInt()
                        } else {
                            animateOffset.toInt()
                        },
                        0
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
fun SaveAsDialog(
    initialSpriteName: String,
    onConfirm: (spriteName: String, description: String) -> Unit,
    onDismiss: () -> Unit,
    taskProgress: Boolean,
) {
    var spriteName by remember(initialSpriteName) { mutableStateOf(initialSpriteName) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(spriteName, description)
                },
                enabled = spriteName.isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (taskProgress)
                    CircularProgressIndicator()
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        title = { Text("Save As") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = spriteName,
                    onValueChange = { spriteName = it },
                    singleLine = true,
                    label = { Text("Sprite Name") },
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    maxLines = 3,
                )
            }
        }
    )
}

