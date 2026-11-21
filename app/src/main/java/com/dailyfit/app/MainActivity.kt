package com.dailyfit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailyfit.app.feature.habits.HabitViewModel
import com.dailyfit.app.feature.habits.HabitViewModelFactory
import com.dailyfit.app.feature.home.HomeScreen
import com.dailyfit.app.feature.history.HistoryScreen
import com.dailyfit.app.feature.plan.PlanScreen
import com.dailyfit.app.ui.theme.DailyFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DailyFitTheme { DailyFitApp() } }
    }
}

private enum class MainDestination(val route: String, val label: String) {
    Home("home", "首页"), Plan("plan", "计划"), History("history", "历史")
}

@Composable
private fun DailyFitApp() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val app = activity.application as DailyFitApplication
    val habitViewModel: HabitViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = HabitViewModelFactory(app.habitRepository, app.temporaryTaskRepository)
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { habitViewModel.refreshDate() }
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    val icon = when (destination) {
                        MainDestination.Home -> Icons.Filled.Home
                        MainDestination.Plan -> Icons.Filled.CalendarMonth
                        MainDestination.History -> Icons.Filled.History
                    }
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = MainDestination.Home.route, modifier = Modifier.padding(padding)) {
            composable(MainDestination.Home.route) {
                val state by habitViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(state, habitViewModel)
            }
            composable(MainDestination.Plan.route) { PlanScreen() }
            composable(MainDestination.History.route) {
                val state by habitViewModel.uiState.collectAsStateWithLifecycle()
                HistoryScreen(state)
            }
        }
    }
}
