package lk.sure.dream.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.sure.dream.compose.components.SpriteItemUiState
import lk.sure.dream.compose.components.canvas.CanvasStateSaver
import lk.sure.dream.compose.components.canvas.addExtensionIfNot
import lk.sure.dream.compose.components.canvas.dreamFileDir
import lk.sure.dream.compose.home.ProjectItemUiState
import lk.sure.dream.data.repos.DreamRepository
import lk.sure.dream.data.repos.PreferenceRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val repository: DreamRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    val isPrivacyPolicyAccepted: StateFlow<Boolean?> =
        preferenceRepository.isPrivacyPolicyAgreed.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _projectList = MutableStateFlow(emptyList<ProjectItemUiState>())
    val projectList = _projectList.asStateFlow()

    private val _spriteList = MutableStateFlow(emptyList<SpriteItemUiState>())
    val spriteList = _spriteList.asStateFlow()

    private val dreamDir = context.dreamFileDir

    init {
        fetchAll()
    }

    fun fetchAll() {
        fetchProjects()
        fetchSprites()
    }

    private fun fetchProjects() {
        viewModelScope.launch {
            _projectList.update {
                repository.getAllProjects()
            }
        }
    }

    private fun fetchSprites() {
        viewModelScope.launch {
            _spriteList.update {
                repository.getAllSprites(null)
            }
        }
    }

    fun createProject(projectName: String, description: String) {
        viewModelScope.launch {
            if (projectName.isEmpty()) return@launch

            repository.createProject(
                projectName,
                description,
            )
            fetchProjects()
        }
    }

    fun createSprite(
        spriteName: String,
        description: String,
        width: Int,
        height: Int,
    ) {
        viewModelScope.launch {
            if (width !in 8..128 || height !in 8..128) return@launch

            val spriteId = repository.createSprite(
                spriteName,
                null,
                description,
                width,
                height,
                null,
                CanvasStateSaver.toByteArray(
                    CanvasStateSaver.getDefaultCanvasData(width, height)
                )
            )

            val spriteFileName = spriteId.addExtensionIfNot()
            val sprite = File(dreamDir, spriteFileName)
            if (sprite.createNewFile()) {
                val dataBytes = CanvasStateSaver.toByteArray(
                    CanvasStateSaver.getDefaultCanvasData(width, height)
                )

                sprite.writeBytes(dataBytes)
            }
            fetchSprites()
        }
    }

    fun deleteSprite(spriteId: String) {
        viewModelScope.launch {
            repository.deleteSprite(spriteId)
            fetchSprites()
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProjectWithAllContent(projectId)
            fetchProjects()
        }
    }

    fun updateProjectMeta(id: String, name: String, description: String) {
        viewModelScope.launch {
            repository.updateProjectMeta(id, name, description)
            fetchProjects()
        }
    }

    fun updateSpriteMeta(id: String, spriteName: String, description: String) {
        viewModelScope.launch {
            repository.updateSpriteMeta(id, spriteName, description)
            fetchSprites()
        }
    }

    fun agreeToPrivacyPolicy() {
        viewModelScope.launch {
            preferenceRepository.agreeToPrivacyPolicy()
        }
    }

    fun duplicateSprite(
        originalId: String,
        name: String,
        description: String,
    ) {
        viewModelScope.launch {
            repository.duplicateSprite(originalId, name, description)
            fetchSprites()
        }
    }

    fun moveSprite(spriteId: String, projectId: String?) {
        viewModelScope.launch {
            repository.changeSpriteProject(spriteId, projectId)
            fetchSprites()
            fetchProjects()
        }
    }
}