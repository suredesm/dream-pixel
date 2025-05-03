package lk.sure.dream.compose.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lk.sure.dream.compose.home.PrivacyPolicyInfoDialog
import lk.sure.dream.compose.palettemanager.Palette
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.utils.isValidFileName
import lk.sure.dream.utils.nameWithoutExtension
import lk.sure.dream.utils.toHumanReadable
import lk.sure.dream.viewmodels.BackupRestoreConflict
import lk.sure.dream.viewmodels.GroupSectionData
import lk.sure.dream.viewmodels.PaletteSectionItemData
import lk.sure.dream.viewmodels.ProjectSectionData
import lk.sure.dream.viewmodels.SettingsViewModel
import lk.sure.dream.viewmodels.SpriteSectionItemData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
) {
    val snackbarScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPrivacyInfoDialog by remember { mutableStateOf(false) }
    var showBackupListDialog by remember { mutableStateOf(false) }
    var showDataBackupDialog by remember { mutableStateOf(false) }
    var showDataRestoreDialog by remember { mutableStateOf(false) }

    val backupInProgress by vm.backupInProcess.collectAsState()
    val backupSuccess by vm.backupSuccessOccurred.collectAsState()
    val backupFiles by vm.backupFiles.collectAsState()
    val groupDataList by vm.sectionDataList.collectAsState()
    val restoreInProgress by vm.restoreInProgress.collectAsStateWithLifecycle()

    val backupDestinationUri by vm.dreamBackupFolderUri.collectAsState()
    val toBeRestored by vm.toBeRestored.collectAsState()

    var currentBackupFile by remember { mutableStateOf<DocumentFile?>(null) }

    if (restoreInProgress) {
        Dialog(onDismissRequest = {}) {
            Card {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Restoring...")
                }
            }
        }
    }

    LaunchedEffect(backupSuccess) {
        val success = backupSuccess ?: return@LaunchedEffect

        snackbarScope.launch {
            snackbarHostState.showSnackbar(
                if (success) "Backup successful"
                else "Backup failed"
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(scaffoldPadding)
                .fillMaxWidth()
        ) {
            item {
                var dest by remember(backupDestinationUri) {
                    mutableStateOf(backupDestinationUri?.getOrNull()?.toHumanReadable() ?: "")
                }
                val context = LocalContext.current
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { mUri ->
                    mUri?.let {
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                        vm.saveDreamBackupFolder(mUri)
                        dest = mUri.toHumanReadable()
                    }
                }

                OutlinedTextField(
                    value = if (backupDestinationUri == null) "Loading..." else dest,
                    onValueChange = {},
                    label = { Text("Backup destination") },
                    trailingIcon = {
                        IconButton(
                            onClick = { picker.launch(null) }
                        ) {
                            Icon(Icons.Default.Folder, null)
                        }
                    },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    placeholder = { Text("Select a backup directory") }
                )

                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier
                        .clickable(enabled = !backupInProgress) {
                            if (backupDestinationUri?.getOrNull() != null) {
                                showDataBackupDialog = true
                            } else {
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("Choose Backup Directory First")
                                }
                            }
                        }
                        .fillMaxWidth()
                        .padding(24.dp, 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Backup Data")

                    if (backupInProgress) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }

                HorizontalDivider()
            }

            item {
                Row(
                    Modifier
                        .clickable {
                            if (backupDestinationUri?.getOrNull() != null) {
                                showBackupListDialog = true
                                vm.retrieveBackupFiles()
                            } else {
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("Choose Backup Directory First")
                                }
                            }
                        }
                        .fillMaxWidth()
                        .padding(24.dp, 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Restore Backup")
                }

                HorizontalDivider()
            }

            item {
                Row(
                    Modifier
                        .clickable { showPrivacyInfoDialog = true }
                        .fillMaxWidth()
                        .padding(24.dp, 16.dp)
                ) {
                    Text("Privacy Policy")
                }
                HorizontalDivider()
            }
        }
    }

    if (showPrivacyInfoDialog) {
        PrivacyPolicyInfoDialog(
            onDismissRequest = { showPrivacyInfoDialog = false },
            modifier = Modifier.padding(vertical = 20.dp)
        )
    }

    if (showBackupListDialog) {
        LaunchedEffect(backupFiles) {
            val files = backupFiles
            if (files != null && files.isEmpty()) {
                showBackupListDialog = false
                snackbarScope.launch {
                    snackbarHostState.showSnackbar(
                        "No Backups were taken"
                    )
                }
            }
        }

        BackupListDialog(
            backupFiles = backupFiles,
            onDismissRequest = {
                showBackupListDialog = false
                vm.resetBackupFiles()
            },
            onConfirm = {
                if (it != null) {
                    vm.retrieveBackupMeta(it)
                    showDataRestoreDialog = true
                    currentBackupFile = it
                }
                vm.resetBackupFiles()
                showBackupListDialog = false
            }
        )
    }

    if (showDataRestoreDialog) {
        BackupRestoreDataDialog(
            asBackupElseRestore = false,
            onConfirm = { spriteIds, paletteIds, overwrite ->
                showDataRestoreDialog = false
                currentBackupFile?.let {
                    vm.restoreBackupFile(spriteIds, paletteIds, it, overwrite)
                }
                currentBackupFile = null
            },
            onDismiss = {
                showDataRestoreDialog = false
                currentBackupFile = null
            },
            groups = toBeRestored,
            backupName = TextFieldValue(""),
            onBackupNameChange = {},
            canBackup = toBeRestored != null
        )
    }

    if (showDataBackupDialog) {
        var name by remember(backupFiles) {
            val text = backupFiles?.let { files ->
                var n = "Backup"
                var i = 1
                while (files.firstOrNull { it.nameWithoutExtension == n } != null) {
                    n = "Backup($i)"
                    i++
                }
                n
            } ?: ""

            mutableStateOf(
                TextFieldValue(
                    text = text,
                )
            )
        }

        LaunchedEffect(Unit) {
            vm.retrieveBackupFiles()
        }

        BackupRestoreDataDialog(
            asBackupElseRestore = true,
            onConfirm = { spriteIds, colorIds, _ ->
                vm.createBackup(name.text, spriteIds, colorIds)
                showDataBackupDialog = false
                vm.resetBackupFiles()
            },
            onDismiss = {
                showDataBackupDialog = false
                vm.resetBackupFiles()
            },
            groups = groupDataList,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            backupName = name,
            onBackupNameChange = { name = it },
            canBackup = name.text.isValidFileName() &&
                    backupFiles != null &&
                    backupFiles?.firstOrNull { it.nameWithoutExtension == name.text } == null,
        )
    }
}

fun Map<String, Boolean>.toTriState() = when {
    all { it.value } -> ToggleableState.On
    none { it.value } -> ToggleableState.Off
    else -> ToggleableState.Indeterminate
}

fun List<Boolean>.toTriState() = when {
    all { it } -> ToggleableState.On
    none { it } -> ToggleableState.Off
    else -> ToggleableState.Indeterminate
}

@Composable
fun BackupListDialog(
    backupFiles: List<DocumentFile>?,
    onDismissRequest: () -> Unit,
    onConfirm: (DocumentFile?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                { onConfirm(selected?.let { backupFiles?.get(it) }) },
                enabled = selected != null
            ) { Text("Next") }
        },
        dismissButton = { TextButton(onDismissRequest) { Text("Cancel") } },
        title = { Text("Available Backup") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (backupFiles == null) {
                    CircularProgressIndicator()
                    return@Column
                }

                BackupList(
                    selected = selected,
                    onSelectedChange = { selected = it },
                    files = backupFiles,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
fun BackupList(
    selected: Int?,
    onSelectedChange: (Int) -> Unit,
    files: List<DocumentFile>?,
) {
    LazyColumn {
        itemsIndexed(files!!) { index, file ->
            Surface(
                onClick = { onSelectedChange(index) },
                modifier = Modifier
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.run { if (selected == index) primary else Color.Transparent }
            ) {
                Column(
                    Modifier
                        .padding(24.dp, 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        file.nameWithoutExtension ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                            Date(
                                file.lastModified()
                            )
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (index != files.lastIndex)
                HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDataDialog(
    asBackupElseRestore: Boolean,
    onConfirm: (spriteIds: List<String>, paletteIds: List<String>, overwrite: BackupRestoreConflict?) -> Unit,
    onDismiss: () -> Unit,
    groups: List<GroupSectionData>?,
    backupName: TextFieldValue,
    onBackupNameChange: (TextFieldValue) -> Unit,
    canBackup: Boolean,
    modifier: Modifier = Modifier,
) {
    val groupExpanded = remember(groups) {
        (groups?.map { false } ?: emptyList()).toMutableStateList()
    }

    val projectExpandedMap = remember(groups) {
        (groups?.firstOrNull { it.name == GroupSectionData.PROJECT_GROUP_HEADLINE }
            ?.items
            ?.filterIsInstance<ProjectSectionData>()
            ?.map { it.id to false } ?: emptyList())
            .toMutableStateMap()
    }

    val spriteCheckedMap = remember(groups) {
        val sprites = groups
            ?.firstOrNull { it.name == GroupSectionData.PROJECT_GROUP_HEADLINE }
            ?.items?.let { itemsNonNull ->
                itemsNonNull.filterIsInstance<SpriteSectionItemData>() +
                        itemsNonNull.filterIsInstance<ProjectSectionData>().map { it.spriteData }
                            .flatten()
            } ?: emptyList()

        sprites.map { it.id to false }.toMutableStateMap()
    }

    val paletteCheckedMap = remember(groups) {
        (groups?.firstOrNull { it.name == GroupSectionData.PALETTE_GROUP_HEADLINE }
            ?.items
            ?.filterIsInstance<PaletteSectionItemData>()
            ?.map { it.id to false }
            ?: emptyList())
            .toMutableStateMap()
    }

    val tooltipCoroutine = rememberCoroutineScope()

    val allSelected by remember(groups) {
        derivedStateOf {
            (spriteCheckedMap + paletteCheckedMap).toTriState()
        }
    }

    var overwrite by remember { mutableStateOf(BackupRestoreConflict.OverwriteAll.takeIf { !asBackupElseRestore }) }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(modifier) {
            Column(
                modifier = Modifier
                    .padding(8.dp, 16.dp)
            ) {
                Text(
                    if (asBackupElseRestore) "Backup Data" else "Restore Data",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (groups == null) {
                    CircularProgressIndicator()
                    return@Column
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TriStateCheckbox(
                        state = allSelected,
                        onClick = {
                            val newValue = when (allSelected) {
                                ToggleableState.On -> false
                                ToggleableState.Off, ToggleableState.Indeterminate -> true
                            }

                            spriteCheckedMap.forEach { (id, _) ->
                                spriteCheckedMap[id] = newValue
                            }

                            paletteCheckedMap.forEach { (id, _) ->
                                paletteCheckedMap[id] = newValue
                            }
                        }
                    )

                    Text("Select All")
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1F)
                ) {
                    groups.forEachIndexed { index, groupSectionData ->
                        groupHeader(
                            expanded = groupExpanded[index],
                            onExpandedChange = { groupExpanded[index] = it },
                            tooltipCoroutine = tooltipCoroutine,
                            groupItem = groupSectionData,
                            isSectionExpanded = when (groupSectionData.items.firstOrNull()) {
                                is SpriteSectionItemData -> {
                                    { id: String ->
                                        projectExpandedMap[id]!!
                                    }
                                }

                                else -> null
                            },
                            onSectionExpandedChange = when (groupSectionData.items.firstOrNull()) {
                                is SpriteSectionItemData -> {
                                    { id: String, expanded: Boolean ->
                                        projectExpandedMap[id] = expanded
                                    }
                                }

                                else -> null
                            },
                            isItemChecked = when (groupSectionData.items.firstOrNull()) {
                                is PaletteSectionItemData -> {
                                    { paletteCheckedMap[it]!! }
                                }

                                is SpriteSectionItemData -> {
                                    { spriteCheckedMap[it]!! }
                                }

                                else -> {
                                    { false }
                                }
                            },
                            onItemCheckedChange = when (groupSectionData.items.firstOrNull()) {
                                is PaletteSectionItemData -> {
                                    { id, checked -> paletteCheckedMap[id] = checked }
                                }

                                is SpriteSectionItemData -> {
                                    { id, checked -> spriteCheckedMap[id] = checked }
                                }

                                else -> {
                                    { _, _ -> }
                                }
                            }
                        )
                    }
                }

                if (asBackupElseRestore) {
                    OutlinedTextField(
                        value = backupName,
                        onValueChange = { onBackupNameChange(it) },
                        label = { Text("Backup Name") },
                        isError = !canBackup,
                        supportingText = {
                            if (!canBackup) {
                                Text(
                                    "Name Invalid or Already Exists",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.onFocusChanged {
                            if (it.hasFocus) {
                                onBackupNameChange(
                                    backupName.copy(
                                        selection = TextRange(
                                            0,
                                            backupName.text.length
                                        )
                                    )
                                )
                            }
                        }
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    val tooltipState = rememberTooltipState(isPersistent = true)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("If exists: ")

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        ) {
                            OutlinedTextField(
                                value = overwrite?.optionName ?: "Select a option",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                leadingIcon = {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = {
                                            PlainTooltip {
                                                Text(
                                                    when (overwrite) {
                                                        BackupRestoreConflict.OverwriteAll -> "Current items will replaced by backup"
                                                        BackupRestoreConflict.OverwriteWithoutProjectMeta -> "Current items will replaced by backup (excludes project meta)"
                                                        BackupRestoreConflict.TakeCopy -> "Backup restore as copy"
                                                        null -> ""
                                                    }
                                                )
                                            }
                                        },
                                        state = tooltipState
                                    ) {
                                        IconButton(
                                            onClick = {}
                                        ) {
                                            Icon(Icons.Default.Info, null)
                                        }
                                    }

                                }
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                BackupRestoreConflict.entries
                                    .forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.optionName) },
                                            onClick = {
                                                overwrite = option
                                                expanded = false
                                                tooltipCoroutine.launch {
                                                    tooltipState.show()
                                                }
                                            }
                                        )
                                    }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .padding(12.dp)
                                .background(alreadyExistColor(true))
                                .size(24.dp)
                        )

                        Text("Already exists")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(4.dp))

                    TextButton(
                        onClick = {
                            onConfirm(
                                spriteCheckedMap.filter { (_, value) -> value }
                                    .map { (id, _) -> id },
                                paletteCheckedMap.filter { (_, value) -> value }
                                    .map { (id, _) -> id },
                                overwrite
                            )
                        },
                        enabled = canBackup && allSelected != ToggleableState.Off,
                    ) {
                        Text(if (asBackupElseRestore) "Backup" else "Restore")
                    }
                }
            }
        }
    }
}

fun LazyListScope.groupHeader(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    tooltipCoroutine: CoroutineScope,
    groupItem: GroupSectionData,
    isSectionExpanded: ((String) -> Boolean)?,
    onSectionExpandedChange: ((id: String, expanded: Boolean) -> Unit)?,
    isItemChecked: (id: String) -> Boolean,
    onItemCheckedChange: (id: String, expanded: Boolean) -> Unit,
) {
    item {
        val allSelectedInGroup by remember(groupItem.items) {
            derivedStateOf {
                if (groupItem.items.isEmpty()) return@derivedStateOf ToggleableState.Off

                val outcome = buildList {
                    groupItem.items.map { item ->
                        when (item) {
                            is PaletteSectionItemData -> add(isItemChecked(item.id))
                            is SpriteSectionItemData -> add(isItemChecked(item.id))
                            is ProjectSectionData -> {
                                addAll(item.spriteData.map { isItemChecked(it.id) })
                            }
                        }
                    }
                }

                outcome.toTriState()
            }
        }

        Surface(
            onClick = { onExpandedChange(!expanded) },
            enabled = groupItem.items.isNotEmpty()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TriStateCheckbox(
                    state = allSelectedInGroup,
                    onClick = {
                        val selectedAll = allSelectedInGroup
                        val newValue = when (selectedAll) {
                            ToggleableState.On -> false
                            ToggleableState.Off, ToggleableState.Indeterminate -> true
                        }

                        groupItem.items.forEach { item ->
                            when (item) {
                                is PaletteSectionItemData -> onItemCheckedChange(item.id, newValue)
                                is SpriteSectionItemData -> onItemCheckedChange(item.id, newValue)
                                is ProjectSectionData -> item.spriteData.forEach {
                                    onItemCheckedChange(it.id, newValue)
                                }
                            }
                        }
                    }
                )

                Text(groupItem.name, style = MaterialTheme.typography.titleMedium)

                Surface {
                    Box(
                        Modifier.minimumInteractiveComponentSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons
                                .Default
                                .run {
                                    if (expanded) KeyboardArrowUp
                                    else KeyboardArrowDown
                                },
                            contentDescription = null,
                            tint = IconButtonDefaults.iconButtonColors().run {
                                if (groupItem.items.isNotEmpty()) contentColor
                                else disabledContentColor
                            }
                        )
                    }
                }
            }
        }
    }

    if (expanded) {
        groupItem.items.forEach { subItem ->
            when (subItem) {
                is PaletteSectionItemData -> paletteItem(
                    paletteData = subItem,
                    checked = isItemChecked(subItem.id),
                    onCheckedChange = onItemCheckedChange,
                )

                is ProjectSectionData -> projectHeader(
                    expanded = isSectionExpanded!!(subItem.id),
                    onExpandedChange = { onSectionExpandedChange!!(subItem.id, it) },
                    projectItem = subItem,
                    tooltipCoroutine = tooltipCoroutine,
                    spriteChecked = isItemChecked,
                    onSpriteCheckedChange = onItemCheckedChange,
                )

                is SpriteSectionItemData -> spriteItem(
                    sprite = subItem,
                    tooltipCoroutine = tooltipCoroutine,
                    checked = isItemChecked(subItem.id),
                    onCheckedChange = { onItemCheckedChange(subItem.id, it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.projectHeader(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    projectItem: ProjectSectionData,
    tooltipCoroutine: CoroutineScope,
    spriteChecked: (id: String) -> Boolean,
    onSpriteCheckedChange: (id: String, checked: Boolean) -> Unit,
) {
    item(key = projectItem.id) {
        val tooltipState = rememberTooltipState(initialIsVisible = false, isPersistent = true)
        val allSelected by remember {
            derivedStateOf {
                if (projectItem.spriteData.isEmpty()) ToggleableState.Off
                else projectItem
                    .spriteData
                    .map { spriteChecked(it.id) }
                    .toTriState()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = projectItem.spriteData.isNotEmpty()
                ) { onExpandedChange(!expanded) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriStateCheckbox(
                state = allSelected,
                onClick = {
                    val bool = allSelected
                    projectItem.spriteData.forEach {
                        onSpriteCheckedChange(
                            it.id,
                            when (bool) {
                                ToggleableState.On -> false
                                ToggleableState.Off, ToggleableState.Indeterminate -> true
                            }
                        )
                    }
                },
                enabled = projectItem.spriteData.isNotEmpty()
            )

            Text(
                text = projectItem.name,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(Modifier.weight(1F))

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(projectItem.description)
                    }
                },
                state = tooltipState,
            ) {
                IconButton({
                    tooltipCoroutine.launch {
                        tooltipState.show()
                    }
                }, enabled = projectItem.description.isNotEmpty()) {
                    Icon(Icons.Outlined.Info, null)
                }
            }
            Box(Modifier.minimumInteractiveComponentSize()) {
                Icon(
                    Icons.Outlined.run {
                        if (expanded && projectItem.spriteData.isNotEmpty()) KeyboardArrowUp else KeyboardArrowDown
                    },
                    null,
                    tint = IconButtonDefaults.iconButtonColors().let {
                        if (projectItem.spriteData.isNotEmpty())
                            it.contentColor
                        else it.disabledContentColor
                    }
                )
            }
        }
    }

    if (expanded) {
        projectItem.spriteData.forEach { sprite ->
            spriteItem(
                sprite = sprite,
                tooltipCoroutine = tooltipCoroutine,
                modifier = Modifier.padding(start = 16.dp),
                checked = spriteChecked(sprite.id),
                onCheckedChange = { onSpriteCheckedChange(sprite.id, it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.spriteItem(
    sprite: SpriteSectionItemData,
    tooltipCoroutine: CoroutineScope,
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    item(sprite.id) {
        val tooltipState = rememberTooltipState(initialIsVisible = false, isPersistent = true)

        Surface(
            color = alreadyExistColor(sprite.alreadyExists)
        ) {
            Row(
                modifier = modifier
                    .clickable { onCheckedChange(!checked) }
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked, { onCheckedChange(!checked) })

                sprite
                    .thumbnail
                    ?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.White)
                        )
                    } ?: Box(
                    Modifier
                        .size(60.dp)
                        .background(Color.White)
                )

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(sprite.name, style = MaterialTheme.typography.labelMedium)
                    Text(sprite.dimensions, style = MaterialTheme.typography.labelSmall)
                    Text(sprite.lastModified, style = MaterialTheme.typography.labelSmall)
                }

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(sprite.description)
                        }
                    },
                    state = tooltipState,
                ) {
                    IconButton({
                        tooltipCoroutine.launch {
                            tooltipState.show()
                        }
                    }, enabled = sprite.description.isNotEmpty()) {
                        Icon(Icons.Outlined.Info, null)
                    }
                }
            }
        }

        HorizontalDivider()
    }
}

fun LazyListScope.paletteItem(
    paletteData: PaletteSectionItemData,
    checked: Boolean,
    onCheckedChange: (id: String, checked: Boolean) -> Unit,
) {
    item {
        val palette = remember(paletteData) {
            paletteData.run {
                ColorPalette(id, name, colors)
            }
        }

        Palette(
            palette = palette,
            checked = checked,
            onCheckedChange = { onCheckedChange(paletteData.id, !checked) },
            modifier = Modifier.padding(8.dp),
            containerColor = alreadyExistColor(paletteData.alreadyExists)
        )
    }
}

@Composable
fun alreadyExistColor(alreadyExists: Boolean): Color = MaterialTheme.colorScheme.run {
    if (alreadyExists) tertiary else surfaceVariant
}