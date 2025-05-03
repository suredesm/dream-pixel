package lk.sure.dream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditMetaDialog(
    title: @Composable () -> Unit,
    confirmText: @Composable RowScope.() -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = projectName.isNotEmpty(),
                content = confirmText
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = title,
        text = {
            Content(
                projectName,
                onProjectNameChange,
                description,
                onDescriptionChange,
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
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChange,
            label = { Text("Project Name") },
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
    }
}