package com.gymlog.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymlog.app.ui.screens.ExerciseDetailScreen
import com.gymlog.app.ui.screens.ExerciseNewScreen
import com.gymlog.app.ui.screens.ExercisesScreen
import com.gymlog.app.ui.screens.HomeScreen
import com.gymlog.app.ui.screens.NewSessionScreen
import com.gymlog.app.ui.screens.PresetDetailScreen
import com.gymlog.app.ui.screens.PresetEditScreen
import com.gymlog.app.ui.screens.PresetsScreen
import com.gymlog.app.ui.screens.SessionDetailScreen
import com.gymlog.app.ui.screens.SessionsScreen
import com.gymlog.app.ui.screens.SettingsScreen

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    NavTab(Screen.Home.route, "Home", Icons.Filled.Home),
    NavTab(Screen.Exercises.route, "Exercises", Icons.Filled.FitnessCenter),
    NavTab(TAB_NEW_SESSION, "Start", Icons.Filled.PlayArrow),
    NavTab(Screen.Sessions.route, "History", Icons.Filled.History),
    NavTab(Screen.Presets.route, "Routines", Icons.Filled.CalendarMonth),
)

@Composable
fun GymLogApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            // Hide bottom bar on Settings / detail screens
            val showBar = currentRoute in tabs.map { it.route } || currentRoute == null
            if (showBar) BottomBar(navController, currentRoute)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = androidx.compose.ui.Modifier
        ) {
            composable(Screen.Home.route) { HomeScreen(navController, padding) }
            composable(Screen.Exercises.route) { ExercisesScreen(navController, padding) }
            composable(Screen.ExerciseNew.route) { ExerciseNewScreen(navController, padding) }
            composable(
                Screen.ExerciseDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ExerciseDetailScreen(navController, padding, id)
            }
            composable(Screen.Presets.route) { PresetsScreen(navController, padding) }
            composable(
                Screen.PresetDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                PresetDetailScreen(navController, padding, entry.arguments?.getLong("id") ?: 0L)
            }
            composable(
                Screen.PresetEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                PresetEditScreen(navController, padding, entry.arguments?.getLong("id") ?: 0L)
            }
            composable(
                Screen.NewSession.route,
                arguments = listOf(navArgument("presetId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { entry ->
                val pid = entry.arguments?.getLong("presetId") ?: -1L
                NewSessionScreen(navController, padding, pid.takeIf { it > 0 })
            }
            composable(Screen.Sessions.route) { SessionsScreen(navController, padding) }
            composable(
                Screen.SessionDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                SessionDetailScreen(navController, padding, entry.arguments?.getLong("id") ?: 0L)
            }
            composable(Screen.Settings.route) { SettingsScreen(navController, padding) }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route ||
                (tab.route == TAB_NEW_SESSION &&
                    currentRoute?.startsWith("sessions/new/") == true)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    val target = if (tab.route == TAB_NEW_SESSION)
                        Screen.NewSession.build()
                    else tab.route
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
