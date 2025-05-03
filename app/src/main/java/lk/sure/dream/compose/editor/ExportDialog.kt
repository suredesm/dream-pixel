package lk.sure.dream.compose.editor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ExportDialog(
    onConfirm: (
        exportAsStrip: Boolean,
        selectedFrames: List<Int>,
        uri: Uri,
        fileName: String,
    ) -> Unit,
    onCancel: () -> Unit,
    frames: List<Bitmap>?,
    fileName: String?,
) {

    var canExport by remember { mutableStateOf(false) }
    var derivedOnConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }

    BackHandler {
        onCancel()
    }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = derivedOnConfirm ?: {},
                enabled = frames != null && canExport && derivedOnConfirm != null
            ) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
        title = { Text("Export") },
        text = {
            if (frames == null || fileName == null) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val itemChecks = remember(frames) {
                    mutableStateListOf<Boolean>().apply {
                        for (frame in frames) {
                            add(true)
                        }
                    }
                }

                val allChecked by remember {
                    derivedStateOf {
                        itemChecks.all { it }
                    }
                }

                var exportAsStrip by remember { mutableStateOf(true) }
                var uri by remember { mutableStateOf<Uri?>(null) }
                var fileNameValue by remember { mutableStateOf(fileName) }

                LaunchedEffect(frames) {
                    derivedOnConfirm = {
                        uri?.let {
                            onConfirm(
                                exportAsStrip,
                                buildList {
                                    itemChecks.forEachIndexed { index, b ->
                                        if (b) add(index)
                                    }
                                },
                                it,
                                fileNameValue
                            )
                        }
                    }
                }

                Content(
                    frames,
                    onCanExportChange = { canExport = it },
                    itemsChecked = itemChecks,
                    onItemsCheckedChange = { index, checked -> itemChecks[index] = checked },
                    allChecked = allChecked,
                    onAllCheckedChanged = { itemChecks.fill(it) },
                    exportAsStrip = exportAsStrip,
                    onExportAsStripChange = { exportAsStrip = it },
                    uri = uri,
                    onUriChange = { uri = it },
                    fileName = fileNameValue,
                    onFileNameChange = { fileNameValue = it },
                )
            }
        }
    )
}

@Composable
private fun Content(
    frames: List<Bitmap>,
    onCanExportChange: (Boolean) -> Unit,
    itemsChecked: List<Boolean>,
    onItemsCheckedChange: (index: Int, checked: Boolean) -> Unit,
    allChecked: Boolean,
    onAllCheckedChanged: (checked: Boolean) -> Unit,
    exportAsStrip: Boolean,
    onExportAsStripChange: (Boolean) -> Unit,
    uri: Uri?,
    onUriChange: (Uri?) -> Unit,
    fileName: String,
    onFileNameChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val pickPath = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) {
        it?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        onUriChange(it)
        onCanExportChange(it != null)
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckCircle(
                allChecked,
                onCheckedChange = onAllCheckedChanged
            ) {
                Text("Select All")
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1F, false)
        ) {
            itemsIndexed(frames) { index, frame ->

                SelectableImage(
                    checked = itemsChecked[index],
                    onCheckedChange = { onItemsCheckedChange(index, it) }
                ) {
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1F),
                        filterQuality = FilterQuality.None
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uri?.path?.let {
                    val path = it.substring(it.indexOf(":") + 1)
                    val trim = 20
                    (if (path.length > trim) "..." else "") + path.takeLast(trim)
                } ?: "Select a folder",
                modifier = Modifier.weight(1F)
            )

            IconButton(
                onClick = { pickPath.launch(null) }
            ) {
                Icon(Icons.Default.Folder, "Select Directory")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = fileName,
                onValueChange = onFileNameChange,
                modifier = Modifier.weight(1F)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("As strip")
            Checkbox(
                checked = exportAsStrip,
                onCheckedChange = onExportAsStripChange
            )
        }
    }
}

@Composable
fun SelectableImage(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(IntrinsicSize.Max)
            .clip(CardDefaults.shape)
            .border(
                2.dp,
                CheckboxDefaults.colors().run {
                    if (checked) checkedBorderColor
                    else uncheckedBorderColor
                },
                CardDefaults.shape
            )
            .background(Color.White)
            .clickable { onCheckedChange(!checked) },
    ) {
        content()

        if (checked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) { }
        }

        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            modifier = Modifier.padding(4.dp),
            tint = CheckboxDefaults.colors().run {
                if (checked)
                    checkedBorderColor
                else
                    uncheckedBorderColor
            }
        )
    }
}

@Composable
fun CheckCircle(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    label: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = CheckboxDefaults.colors().run {
                if (checked)
                    checkedBorderColor
                else
                    uncheckedBorderColor
            }
        )

        label()
    }
}