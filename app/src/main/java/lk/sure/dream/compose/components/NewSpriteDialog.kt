package lk.sure.dream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NewSpriteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    spriteName: String,
    onSpriteNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    width: String,
    onWidthChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(
            onClick = onConfirm,
            enabled = spriteName.isNotEmpty() &&
                    width.toIntOrNull()?.let { it in 1..64 } ?: false &&
                    height.toIntOrNull()?.let { it in 1..64 } ?: false
        ) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New Sprite") },
        text = {
            Content(
                spriteName,
                onSpriteNameChange,
                description,
                onDescriptionChange,
                width,
                onWidthChange,
                height,
                onHeightChange
            )
        },
        modifier = modifier,
    )
}


@Composable
private fun Content(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    width: String,
    onWidthChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
) {
    var widthWarning by remember { mutableStateOf<String?>(null) }
    var heightWarning by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChange,
            label = { Text("Sprite Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (Optional)") },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = width,
                onValueChange = {
                    onWidthChange(it)
                    widthWarning = if ((it.toIntOrNull() ?: 0) > 64) {
                        "Not compatible above 64"
                    } else {
                        null
                    }
                },
                label = { Text("Width") },
                placeholder = { Text("8 - 64") },
                singleLine = true,
                suffix = { Text("px") },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                modifier = Modifier.weight(1F),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                isError = widthWarning != null
            )

            OutlinedTextField(
                value = height,
                onValueChange = {
                    onHeightChange(it)
                    heightWarning = if ((it.toIntOrNull() ?: 0) > 64) {
                        "Not compatible above 64"
                    } else {
                        null
                    }
                },
                label = { Text("Height") },
                placeholder = { Text("8 - 64") },
                singleLine = true,
                suffix = { Text("px") },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                modifier = Modifier.weight(1F),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                isError = heightWarning != null
            )
        }

        if (widthWarning != null || heightWarning != null) {
            WarningText(widthWarning ?: heightWarning ?: "")
        }
    }
}


@Composable
internal fun WarningText(
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, null, tint = Color.Yellow)
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = Color.Yellow,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}