package lk.sure.dream.compose.components

import androidx.compose.ui.graphics.ImageBitmap

data class SpriteItemUiState(
    val id: String = "",
    val spriteName: String,
    val spriteDescription: String,
    val width: Int,
    val height: Int,
    val lastModified: Long,
    val thumbnail: ImageBitmap?,
)