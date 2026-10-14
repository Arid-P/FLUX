package com.fluxfoxus.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.datastore.UserPreferencesRepository
import com.fluxfoxus.core.designsystem.theme.HolstColors
import com.fluxfoxus.features.focus.FocusScreen
import com.fluxfoxus.features.focus.FocusViewModel
import com.fluxfoxus.features.home.HomeScreen
import com.fluxfoxus.features.home.HomeViewModel
import com.fluxfoxus.features.limits.LimitsScreen
import com.fluxfoxus.features.limits.LimitsViewModel
import com.fluxfoxus.features.onboarding.OnboardingWizardScreen
import com.fluxfoxus.features.planner.PlannerScreen
import com.fluxfoxus.features.planner.PlannerViewModel
import com.fluxfoxus.features.presets.PresetEditorScreen
import com.fluxfoxus.features.presets.PresetEditorViewModel
import com.fluxfoxus.features.stats.AppDetailScreen
import com.fluxfoxus.features.stats.StatsScreen
import com.fluxfoxus.features.stats.StatsViewModel

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Focus : Screen("focus", "Focus", Icons.Default.HourglassEmpty)
    data object Planner : Screen("planner", "Planner", Icons.Default.DateRange)
    data object Limits : Screen("limits", "Limits", Icons.Default.Shield)
    data object Stats : Screen("stats", "Stats", Icons.Default.BarChart)

    data object PresetEditor : Screen("preset_editor?presetId={presetId}") {
        fun createRoute(presetId: String? = null): String {
            return if (presetId != null) "preset_editor?presetId=$presetId" else "preset_editor"
        }
    }

    data object AppDetail : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String): String = "app_detail/$packageName"
    }

    data object Onboarding : Screen("onboarding_wizard")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Focus,
    Screen.Planner,
    Screen.Limits,
    Screen.Stats
)

@Composable
fun FluxFoxusNavHost(
    navController: NavHostController,
    db: FluxFoxusDatabase,
    userPrefs: UserPreferencesRepository,
    startDestination: String = Screen.Home.route,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = HolstColors.Background,
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = HolstColors.Surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("flux_bottom_navigation")
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon ?: Icons.Default.Home,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(text = screen.label ?: "")
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HolstColors.Background,
                                selectedTextColor = HolstColors.PrimarySand,
                                indicatorColor = HolstColors.PrimarySand,
                                unselectedIconColor = HolstColors.MutedBlue,
                                unselectedTextColor = HolstColors.MutedBlue
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(db, userPrefs)
                )
                HomeScreen(
                    viewModel = homeVm,
                    onNavigateToFocus = {
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Onboarding.route)
                    }
                )
            }

            composable(Screen.Focus.route) {
                val focusVm: FocusViewModel = viewModel(
                    factory = FocusViewModel.Factory(db, userPrefs)
                )
                FocusScreen(
                    viewModel = focusVm,
                    onNavigateToCreatePreset = {
                        navController.navigate(Screen.PresetEditor.createRoute())
                    }
                )
            }

            composable(Screen.Planner.route) {
                val plannerVm: PlannerViewModel = viewModel(
                    factory = PlannerViewModel.Factory(db)
                )
                PlannerScreen(
                    viewModel = plannerVm
                )
            }

            composable(Screen.Limits.route) {
                val limitsVm: LimitsViewModel = viewModel(
                    factory = LimitsViewModel.Factory(db)
                )
                LimitsScreen(
                    viewModel = limitsVm,
                    onNavigateToAppDetail = { pkg ->
                        navController.navigate(Screen.AppDetail.createRoute(pkg))
                    }
                )
            }

            composable(Screen.Stats.route) {
                val statsVm: StatsViewModel = viewModel(
                    factory = StatsViewModel.Factory(db)
                )
                StatsScreen(
                    viewModel = statsVm,
                    onNavigateToAppDetail = { pkg ->
                        navController.navigate(Screen.AppDetail.createRoute(pkg))
                    }
                )
            }

            composable(
                route = Screen.PresetEditor.route,
                arguments = listOf(navArgument("presetId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val presetId = backStackEntry.arguments?.getString("presetId")
                val presetVm: PresetEditorViewModel = viewModel(
                    factory = PresetEditorViewModel.Factory(db, presetId)
                )
                PresetEditorScreen(
                    viewModel = presetVm,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(navArgument("packageName") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailScreen(
                    packageName = packageName,
                    db = db,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingWizardScreen(
                    onFinishOnboarding = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
