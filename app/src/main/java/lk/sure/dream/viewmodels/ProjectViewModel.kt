package lk.sure.dream.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.sure.dream.compose.components.SpriteItemUiState
import lk.sure.dream.compose.components.canvas.CanvasStateSaver
import lk.sure.dream.compose.home.ProjectItemUiState
import lk.sure.dream.data.repos.DreamRepository
import javax.inject.Inject

data class ProjectUiState(
    val projectId: String = "",
    val projectName: String = "",
    val sprites: List<SpriteItemUiState> = emptyList(),
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val repository: DreamRepository,
) : ViewModel() {
    private var projectId: String? = null

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState = _uiState.asStateFlow()

    private val _projectList = MutableStateFlow(emptyList<ProjectItemUiState>())
    val projectList = _projectList.asStateFlow()

    fun setup(projectId: String) = viewModelScope.launch {
        val projectName = repository.getProject(projectId).projectName
        _uiState.update {
            it.copy(projectName = projectName)
        }
        _projectList.update { repository.getAllProjects(false) }
        this@ProjectViewModel.projectId = projectId
        fetchSprites()
    }

    fun fetchSprites() {
        projectId ?: return
        viewModelScope.launch {
            val sprites = repository.getAllSprites(projectId)

            _uiState.update {
                it.copy(sprites = sprites)
            }
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

            repository.createSprite(
                spriteName,
                projectId,
                description,
                width,
                height,
                null,
                CanvasStateSaver.toByteArray(
                    CanvasStateSaver.getDefaultCanvasData(width, height)
                )
            )

            fetchSprites()
        }
    }

    fun deleteSprite(spriteId: String) {
        viewModelScope.launch {
            repository.deleteSprite(spriteId)
            fetchSprites()
        }
    }

    fun updateSpriteMeta(id: String, spriteName: String, description: String) {
        viewModelScope.launch {
            repository.updateSpriteMeta(id, spriteName, description)
            fetchSprites()
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
        }
    }
}