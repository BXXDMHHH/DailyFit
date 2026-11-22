package com.dailyfit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
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
import com.dailyfit.app.feature.plan.PlanViewModel
import com.dailyfit.app.feature.plan.PlanViewModelFactory
import com.dailyfit.app.feature.workout.WorkoutScreen
import com.dailyfit.app.feature.workout.WorkoutViewModel
import com.dailyfit.app.feature.workout.WorkoutViewModelFactory
import java.time.LocalDate
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
    val app = LocalContext.current.applicationContext as DailyFitApplication
    val habitViewModel: HabitViewModel = viewModel(
        factory = HabitViewModelFactory(app.habitRepository, app.temporaryTaskRepository)
    )
    val planViewModel: PlanViewModel = viewModel(
        factory = PlanViewModelFactory(app.trainingPlanRepository)
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        habitViewModel.refreshDate()
        planViewModel.refreshDate()
    }
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    LaunchedEffect(currentRoute) {
        if (currentRoute == MainDestination.Home.route) planViewModel.selectDate(LocalDate.now())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute != "workout") {
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
        }
    ) { padding ->
        NavHost(navController, startDestination = MainDestination.Home.route, modifier = Modifier.padding(padding)) {
            composable(MainDestination.Home.route) {
                val state by habitViewModel.uiState.collectAsStateWithLifecycle()
                val planState by planViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    viewModel = habitViewModel,
                    todayWorkoutPlans = planState.dailyPlans.takeIf { planState.selectedDate == LocalDate.now() }.orEmpty(),
                    isWorkoutPlansLoading = planState.isLoading,
                    onStartWorkout = {
                        planViewModel.selectDate(LocalDate.now())
                        navController.navigate("workout")
                    }
                )
            }
            composable(MainDestination.Plan.route) { PlanScreen(planViewModel, habitViewModel) }
            composable("workout") {
                val planState by planViewModel.uiState.collectAsStateWithLifecycle()
                val workoutViewModel: WorkoutViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    factory = WorkoutViewModelFactory(app.workoutRepository, app.trainingPlanRepository, app.database)
                )
                WorkoutScreen(
                    viewModel = workoutViewModel,
                    plans = planState.dailyPlans.takeIf { planState.selectedDate == LocalDate.now() }.orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(MainDestination.History.route) {
                val state by habitViewModel.uiState.collectAsStateWithLifecycle()
                HistoryScreen(state, app.database, Modifier.fillMaxSize())
            }
        }
    }
}
