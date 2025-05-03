package lk.sure.dream.compose.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import lk.sure.dream.compose.components.EditMetaDialog
import lk.sure.dream.compose.components.NewSpriteDialog
import lk.sure.dream.compose.components.SpriteGrid
import lk.sure.dream.compose.components.SpriteItemUiState
import lk.sure.dream.compose.home.DeleteMeta
import lk.sure.dream.compose.home.SpriteMoveDialog
import lk.sure.dream.viewmodels.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    projectId: String,
    navigateToEditor: (String) -> Unit,
    vm: ProjectViewModel = hiltViewModel(),
) {
    val owner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        vm.setup(projectId)

        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.fetchSprites()
        }
    }

    val uiState by vm.uiState.collectAsState()
    val projectList by vm.projectList.collectAsState()

    var showSpriteDialog by remember { mutableStateOf(false) }
    var spriteEditMeta by remember { mutableStateOf<SpriteItemUiState?>(null) }
    var deleteMeta by remember { mutableStateOf<DeleteMeta?>(null) }
    var duplicateMeta by remember { mutableStateOf<SpriteItemUiState?>(null) }
    var spriteIdToMove by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.projectName) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSpriteDialog = true }
            ) {
                Icon(Icons.Default.Add, null)
                Text("New Sprite")
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .fillMaxSize()
        ) {
            SpriteGrid(
                modifier = Modifier
                    .weight(1F)
                    .padding(8.dp),
                spriteList = uiState.sprites,
                onItemClick = {
                    navigateToEditor(it.id)
                },
                onItemRename = {
                    spriteEditMeta = it
                },
                onItemDelete = {
                    deleteMeta = DeleteMeta(
                        it.id,
                        true
                    )
                },
                onItemDuplicate = {
                    duplicateMeta = it
                },
                onItemMove = {
                    spriteIdToMove = it.id
                },
            )
        }
    }

    if (showSpriteDialog) {
        var spriteName by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var width by remember { mutableStateOf("") }
        var height by remember { mutableStateOf("") }

        NewSpriteDialog(
            onConfirm = {
                vm.createSprite(spriteName, description, width.toInt(), height.toInt())
                showSpriteDialog = false
            },
            onDismiss = { showSpriteDialog = false },
            spriteName = spriteName,
            onSpriteNameChange = { spriteName = it },
            description = description,
            onDescriptionChange = { description = it },
            width = width,
            onWidthChange = { width = it },
            height = height,
            onHeightChange = { height = it },
        )
    }

    spriteEditMeta?.let {
        var spriteName by remember { mutableStateOf(it.spriteName) }
        var description by remember { mutableStateOf(it.spriteDescription) }

        EditMetaDialog(
            title = { Text("Edit Sprite") },
            confirmText = { Text("Update") },
            onConfirm = {
                vm.updateSpriteMeta(it.id, spriteName, description)
                spriteEditMeta = null
            },
            onDismiss = {
                spriteEditMeta = null
            },
            projectName = spriteName,
            onProjectNameChange = { spriteName = it },
            description = description,
            onDescriptionChange = { description = it },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )
    }

    deleteMeta?.let {
        AlertDialog(
            onDismissRequest = { deleteMeta = null },
            confirmButton = {
                TextButton({
                    vm.deleteSprite(it.id)
                    deleteMeta = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton({ deleteMeta = null }) {
                    Text("Cancel")
                }
            },
            text = { Text("Do you want to delete ${if (it.isSprite) "Sprite" else "Project"}?") }
        )
    }

    duplicateMeta?.let {
        var name by remember { mutableStateOf(it.spriteName) }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { duplicateMeta = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.duplicateSprite(
                            it.id, name, description
                        )
                        duplicateMeta = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton({ duplicateMeta = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Duplicate") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Description") },
                    )
                }
            }
        )
    }

    spriteIdToMove?.let { id ->
        SpriteMoveDialog(
            projectList = projectList,
            onDismiss = { spriteIdToMove = null },
            onConfirm = {
                vm.moveSprite(id, it)
                spriteIdToMove = null
            }
        )
    }
}