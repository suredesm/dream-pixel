package lk.sure.dream.compose

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import lk.sure.dream.compose.editor.EditorScreen
import lk.sure.dream.compose.home.HomeScreen
import lk.sure.dream.compose.palettemanager.PaletteManagerScreen
import lk.sure.dream.compose.project.ProjectScreen
import lk.sure.dream.compose.settings.SettingsScreen

@Composable
fun DreamApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home,
    ) {
        composable<Screen.Home> {
            HomeScreen(
                navigateToEditor = { projectId, spriteId ->
                    navController.navigate(Screen.Editor(projectId, spriteId))
                },
                navigateToProject = { projectId ->
                    navController.navigate(Screen.Project(projectId))
                },
                navigateToPaletteManager = {
                    navController.navigate(Screen.PaletteManager)
                },
                navigateToSettings = { navController.navigate(Screen.Settings) },
            )
        }

        composable<Screen.Project> {
            val args = it.toRoute<Screen.Project>()
            ProjectScreen(
                projectId = args.projectId,
                navigateToEditor = { spriteId ->
                    navController.navigate(Screen.Editor(args.projectId, spriteId))
                }
            )
        }

        composable<Screen.Editor> {
            val args = it.toRoute<Screen.Editor>()
            EditorScreen(
                goBack = { navController.popBackStack() },
                spriteId = args.spriteId
            )
        }

        composable<Screen.PaletteManager> {
            PaletteManagerScreen()
        }

        composable<Screen.Settings> {
            SettingsScreen()
        }
    }
}