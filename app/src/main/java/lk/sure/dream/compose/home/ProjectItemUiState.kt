package lk.sure.dream.compose.home

import androidx.compose.ui.graphics.ImageBitmap

data class ProjectItemUiState(
    val id: String,
    val projectName: String,
    val projectDescription: String,
    val thumbnail: ImageBitmap?
)