package lk.sure.dream.viewmodels

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.sure.dream.compose.components.SpriteItemUiState
import lk.sure.dream.data.repos.DreamRepository
import lk.sure.dream.data.repos.PaletteRepository
import lk.sure.dream.data.repos.PreferenceRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class GroupSectionData(
    val name: String,
    val items: List<SectionItemData>,
) {
    companion object {
        const val PROJECT_GROUP_HEADLINE = "Projects"
        const val PALETTE_GROUP_HEADLINE = "Palettes"
    }
}

sealed class SectionItemData

data class ProjectSectionData(
    val id: String,
    val name: String,
    val description: String,
    val spriteData: List<SpriteSectionItemData>,
    val alreadyExists: Boolean = false,
) : SectionItemData()

data class SpriteSectionItemData(
    val id: String,
    val name: String,
    val description: String,
    val dimensions: String,
    val lastModified: String,
    val thumbnail: ImageBitmap?,
    val alreadyExists: Boolean = false,
) : SectionItemData()

data class PaletteSectionItemData(
    val id: String,
    val name: String,
    val colors: List<Int>,
    val alreadyExists: Boolean = false,
) : SectionItemData()

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dreamRepo: DreamRepository,
    private val paletteRepo: PaletteRepository,
    private val prefRepository: PreferenceRepository,
) : ViewModel() {

    private val _backupInProcess = MutableStateFlow(false)
    val backupInProcess = _backupInProcess.asStateFlow()

    private val _backupSuccessOccurred = MutableStateFlow<Boolean?>(null)
    val backupSuccessOccurred = _backupSuccessOccurred.asStateFlow()

    private val _backupFiles = MutableStateFlow<List<DocumentFile>?>(null)
    val backupFiles = _backupFiles.asStateFlow()

    private val _sectionDataList = MutableStateFlow<List<GroupSectionData>?>(null)
    val sectionDataList = _sectionDataList.asStateFlow()

    private val _dreamBackupFolderUri = MutableStateFlow<Result<Uri?>?>(null)
    val dreamBackupFolderUri = _dreamBackupFolderUri.asStateFlow()

    private val _toBeRestored = MutableStateFlow<List<GroupSectionData>?>(null)
    val toBeRestored = _toBeRestored.asStateFlow()

    private val _restoreInProgress = MutableStateFlow(false)
    val restoreInProgress = _restoreInProgress.asStateFlow()


    init {
        viewModelScope.launch {
            _dreamBackupFolderUri.update {
                Result.success(prefRepository.loadDreamBackupFolderUri())
            }
            retrieveCurrentProjectData()
        }
    }

    fun createBackup(name: String, spriteIds: List<String>, paletteIds: List<String>) {
        viewModelScope.launch {
            _backupSuccessOccurred.update { null }
            _backupInProcess.update { true }
            _backupSuccessOccurred.update {
                dreamRepo.createBackup(
                    dreamBackupFolderUri.value?.getOrNull(), name, spriteIds, paletteIds
                )
            }
            _backupInProcess.update { false }
        }
    }

    fun retrieveBackupFiles() {
        viewModelScope.launch {
            _backupFiles.update {
                dreamRepo.retrieveBackups(dreamBackupFolderUri.value?.getOrNull())
            }
        }
    }

    fun retrieveBackupMeta(file: DocumentFile?) {
        viewModelScope.launch {
            if (file == null) return@launch

            val (
                sprites,
                projects,
                palettes,
                thumbnails,
            ) = dreamRepo.retrieveBackup(file) ?: return@launch
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val list = buildList {
                if (palettes.isNotEmpty()) {
                    val allPalettes = paletteRepo.getAllPalettes()

                    add(
                        GroupSectionData(
                            GroupSectionData.PALETTE_GROUP_HEADLINE,
                            palettes.map { palette ->
                                PaletteSectionItemData(id = palette.id,
                                    name = palette.name,
                                    colors = palette.colors,
                                    alreadyExists = allPalettes.firstOrNull { palette.id == it.id } != null)
                            })
                    )
                }

                val spriteListIds = dreamRepo.getAllSpritesRegardlessProject().map { it.id }

                add(
                    GroupSectionData(

                        GroupSectionData.PROJECT_GROUP_HEADLINE, buildList {
                            sprites.groupBy { it.projectId }.forEach { (projectId, lSprites) ->
                                if (projectId == null) {
                                    addAll(lSprites.map { sprite ->
                                        SpriteSectionItemData(
                                            id = sprite.id,
                                            name = sprite.name,
                                            description = sprite.description,
                                            dimensions = "${sprite.width}x${sprite.height}",
                                            lastModified = dateFormat.format(Date(sprite.lastModified)),
                                            thumbnail = thumbnails[sprite.id],
                                            alreadyExists = spriteListIds.contains(sprite.id),
                                        )
                                    })
                                } else {
                                    val project = projects.first { it.id == projectId }

                                    add(
                                        ProjectSectionData(projectId,
                                            project.name,
                                            project.description,
                                            spriteData = lSprites.map { sprite ->
                                                SpriteSectionItemData(
                                                    id = sprite.id,
                                                    name = sprite.name,
                                                    description = sprite.description,
                                                    dimensions = "${sprite.width}x${sprite.height}",
                                                    lastModified = dateFormat.format(Date(sprite.lastModified)),
                                                    thumbnail = thumbnails[sprite.id],
                                                    alreadyExists = spriteListIds.contains(sprite.id),
                                                )
                                            })
                                    )
                                }
                            }
                        })
                )
            }
            _toBeRestored.update { list }
        }
    }

    fun resetBackupFiles() {
        _backupFiles.update { null }
    }

    private fun retrieveCurrentProjectData() {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val projects = dreamRepo.getAllProjects(false)
            val palettes = paletteRepo.getAllPalettes()

            fun SpriteItemUiState.toSpriteSectionItemData() = SpriteSectionItemData(
                id = id,
                name = spriteName,
                description = spriteDescription,
                dimensions = "${width}x${height}",
                lastModified = dateFormat.format(Date(lastModified)),
                thumbnail = thumbnail,
            )

            _sectionDataList.update {
                listOf(
                    GroupSectionData(
                        name = GroupSectionData.PALETTE_GROUP_HEADLINE,
                        items = palettes.map { palette ->
                            PaletteSectionItemData(
                                palette.id, palette.name, palette.colors
                            )
                        },
                    ),
                    GroupSectionData(
                        name = GroupSectionData.PROJECT_GROUP_HEADLINE,
                        items = buildList {
                            addAll(
                                dreamRepo.getAllSprites(null).map { it.toSpriteSectionItemData() })
                            addAll(projects.map { project ->
                                ProjectSectionData(id = project.id,
                                    name = project.projectName,
                                    description = project.projectDescription,
                                    spriteData = dreamRepo.getAllSprites(project.id).map { sprite ->
                                        sprite.toSpriteSectionItemData()
                                    })
                            })
                        }),
                )
            }
        }
    }

    fun saveDreamBackupFolder(uri: Uri) {
        viewModelScope.launch {
            prefRepository.saveDreamBackupFolderUri(uri)
            _dreamBackupFolderUri.update {
                Result.success(prefRepository.loadDreamBackupFolderUri())
            }
        }
    }

    fun restoreBackupFile(
        spriteIds: List<String>,
        paletteIds: List<String>,
        file: DocumentFile,
        conflict: BackupRestoreConflict?,
    ) {
        viewModelScope.launch {
            conflict ?: return@launch
            _restoreInProgress.update { true }
            dreamRepo.restoreSprites(file, spriteIds, conflict, paletteIds)
            _restoreInProgress.update { false }
        }
    }
}

enum class BackupRestoreConflict(val optionName: String) {
    OverwriteAll("Overwrite All"),
    OverwriteWithoutProjectMeta("Overwrite (Exclude Project Meta)"),
    TakeCopy("Take Copy");
}