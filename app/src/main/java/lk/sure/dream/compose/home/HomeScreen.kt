package lk.sure.dream.compose.home

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import lk.sure.dream.R
import lk.sure.dream.compose.components.EditMetaDialog
import lk.sure.dream.compose.components.MenuContainer
import lk.sure.dream.compose.components.NewSpriteDialog
import lk.sure.dream.compose.components.SpriteGrid
import lk.sure.dream.compose.components.SpriteItemUiState
import lk.sure.dream.viewmodels.HomeViewModel

val pixelFontFamily = FontFamily(
    Font(R.font.jacquarda_bastarda_9)
)

@Composable
fun HomeScreen(
    navigateToEditor: (String?, String) -> Unit,
    navigateToProject: (String) -> Unit,
    navigateToPaletteManager: () -> Unit,
    navigateToSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val agreedToPrivacyPolicy by vm.isPrivacyPolicyAccepted.collectAsState()

    if (agreedToPrivacyPolicy != true) {
        Scaffold {
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
        if (agreedToPrivacyPolicy == false) {
            PrivacyPolicyDialog(
                onAccept = vm::agreeToPrivacyPolicy
            )
        }
        return
    }

    val tabTitles = listOf("Projects", "Sprites")
    val pagerState = rememberPagerState { tabTitles.size }
    val pagerCoroutine = rememberCoroutineScope()

    var showNewSpriteDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val projectList by vm.projectList.collectAsStateWithLifecycle()
    val spriteList by vm.spriteList.collectAsStateWithLifecycle()

    var projectEditMeta by remember { mutableStateOf<ProjectItemUiState?>(null) }
    var spriteEditMeta by remember { mutableStateOf<SpriteItemUiState?>(null) }

    var deleteMeta by remember { mutableStateOf<DeleteMeta?>(null) }
    var duplicateMeta by remember { mutableStateOf<SpriteItemUiState?>(null) }
    var spriteIdToMove by remember { mutableStateOf<String?>(null) }

    var showSelectDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.fetchAll()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = navigateToSettings,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ) {
                    Icon(Icons.Default.Settings, "Settings")
                }

                SmallFloatingActionButton(
                    onClick = navigateToPaletteManager,
                    containerColor = MaterialTheme.colorScheme.secondary,
                ) {
                    Icon(Icons.Default.ColorLens, "Palette")
                }

                ExtendedFloatingActionButton(
                    onClick = { showSelectDialog = true }
                ) {
                    Icon(Icons.Default.Add, null)
                    Text("Create")
                }
            }
        }
    ) { scaffoldValue ->

        Column(
            modifier = Modifier
                .padding(scaffoldValue)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = pixelFontFamily,
                )
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            pagerCoroutine.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
            ) { page ->
                if (page == 0) {
                    LazyColumn(
                        modifier = Modifier,
                    ) {
                        items(projectList, key = { it.id }) {
                            ProjectItem(
                                projectName = it.projectName,
                                projectDescription = it.projectDescription,
                                thumbnail = it.thumbnail,
                                onClick = { navigateToProject(it.id) },
                                onRename = { projectEditMeta = it },
                                onDelete = {
                                    deleteMeta = DeleteMeta(
                                        it.id,
                                        false
                                    )
                                },
                                modifier = Modifier
                                    .padding(20.dp, 10.dp)
                                    .height(80.dp),
                                containerModifier = Modifier
                                    .animateItem()
                            )
                        }
                    }
                } else {
                    SpriteGrid(
                        modifier = Modifier
                            .padding(8.dp),
                        spriteList = spriteList,
                        onItemClick = { navigateToEditor(null, it.id) },
                        onItemRename = {
                            spriteEditMeta = it
                        },
                        onItemDuplicate = {
                            duplicateMeta = it
                        },
                        onItemMove = {
                            spriteIdToMove = it.id
                        },
                        onItemDelete = {
                            deleteMeta = DeleteMeta(
                                it.id,
                                true
                            )
                        },
                    )
                }
            }
        }

        projectEditMeta?.let { state ->
            var projectName by remember(state) { mutableStateOf(state.projectName) }
            var description by remember(state) { mutableStateOf(state.projectDescription) }

            EditMetaDialog(
                title = { Text(if (state.id.isEmpty()) "New Project" else "Edit Project") },
                confirmText = { Text(if (state.id.isEmpty()) "Create" else "Update") },
                onConfirm = {
                    if (state.id.isEmpty())
                        vm.createProject(projectName, description)
                    else
                        vm.updateProjectMeta(state.id, projectName, description)

                    projectEditMeta = null
                },
                onDismiss = {
                    projectEditMeta = null
                },
                projectName = projectName,
                onProjectNameChange = { projectName = it },
                description = description,
                onDescriptionChange = { description = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }

        if (showNewSpriteDialog) {
            var spriteName by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var width by remember { mutableStateOf("") }
            var height by remember { mutableStateOf("") }

            NewSpriteDialog(
                onConfirm = {
                    showNewSpriteDialog = false
                    vm.createSprite(
                        spriteName,
                        description,
                        width.toInt(),
                        height.toInt()
                    )
                },
                onDismiss = {
                    showNewSpriteDialog = false
                },
                spriteName = spriteName,
                onSpriteNameChange = { spriteName = it },
                description = description,
                onDescriptionChange = { description = it },
                width = width,
                onWidthChange = { width = it },
                height = height,
                onHeightChange = { height = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
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
    }

    deleteMeta?.let {
        AlertDialog(
            onDismissRequest = { deleteMeta = null },
            confirmButton = {
                TextButton({
                    if (it.isSprite) vm.deleteSprite(it.id)
                    else vm.deleteProject(it.id)

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

    if (showSelectDialog) {
        AlertDialog(
            onDismissRequest = { showSelectDialog = false },
            confirmButton = {},
            dismissButton = {},
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            projectEditMeta = ProjectItemUiState("", "", "", null)
                            showSelectDialog = false
                        },
                        modifier = Modifier
                            .weight(1F)
                            .aspectRatio(1F),
                        shape = CardDefaults.shape,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Project", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Surface(
                        onClick = {
                            showNewSpriteDialog = true
                            showSelectDialog = false
                        },
                        modifier = Modifier
                            .weight(1F)
                            .aspectRatio(1F),
                        shape = CardDefaults.shape,
                        contentColor = MaterialTheme.colorScheme.tertiary
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Sprite", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectItem(
    projectName: String,
    projectDescription: String,
    thumbnail: ImageBitmap?,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val finalThumbnail = remember(thumbnail) {
        thumbnail ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    MenuContainer(
        menuExpanded = menuExpanded,
        onMenuDismissRequest = { menuExpanded = false },
        onRename = onRename,
        onDuplicate = null,
        onMove = null,
        onDelete = {
            onDelete()
        },
        modifier = containerModifier
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onLongClick = { menuExpanded = true },
                    onClick = onClick
                )
                .then(modifier),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                bitmap = finalThumbnail,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1F)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = projectName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = projectDescription,
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun SpriteMoveDialog(
    projectList: List<ProjectItemUiState>,
    onDismiss: () -> Unit,
    onConfirm: (moveToProjectId: String?) -> Unit,
    currentlyInt: Int? = null,
) {
    var selectedIndex by remember { mutableStateOf(currentlyInt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIndex?.let { projectList[it].id }) }
            ) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Move to") },
        text = {
            LazyColumn {
                item {
                    ProjectMoveItem(
                        projectName = "Home",
                        onClick = { selectedIndex = null },
                        modifier = Modifier.fillMaxWidth(),
                        icon = { Icon(Icons.Default.Home, null) },
                        selected = selectedIndex == null,
                    )
                }

                itemsIndexed(projectList) { index, project ->
                    HorizontalDivider()

                    ProjectMoveItem(
                        projectName = project.projectName,
                        onClick = { selectedIndex = index },
                        modifier = Modifier.fillMaxWidth(),
                        icon = { Icon(Icons.Default.Folder, null) },
                        selected = selectedIndex == index,
                    )
                }
            }
        }
    )
}

@Composable
fun ProjectMoveItem(
    projectName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                it()
                Spacer(Modifier.width(4.dp))
            }

            Text(projectName)
        }
    }
}