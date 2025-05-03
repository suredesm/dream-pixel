package lk.sure.dream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MenuContainer(
    menuExpanded: Boolean,
    onMenuDismissRequest: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onDuplicate: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        content()

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onMenuDismissRequest
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    onRename()
                    onMenuDismissRequest()
                },
                leadingIcon = { Icon(Icons.Default.Edit, "Rename") }
            )

            HorizontalDivider()

            onDuplicate?.let {
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = {
                        onDuplicate()
                        onMenuDismissRequest()
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, "Duplicate") }
                )

                HorizontalDivider()
            }

            onMove?.let {
                DropdownMenuItem(
                    text = { Text("Move") },
                    onClick = {
                        it()
                        onMenuDismissRequest()
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Default.DriveFileMove, "Move") }
                )

                HorizontalDivider()
            }

            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    onMenuDismissRequest()
                },
                leadingIcon = { Icon(Icons.Default.Delete, "Delete") }
            )
        }
    }
}