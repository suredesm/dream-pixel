package lk.sure.dream.compose

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object Home: Screen()

    @Serializable
    data class Editor(
        val projectId: String?,
        val spriteId: String
    ): Screen()

    @Serializable
    data class Project(
        val projectId: String
    ): Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object PaletteManager : Screen()
}
