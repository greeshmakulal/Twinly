package com.example.findduplicates.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: FileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(route = Screen.DuplicateResults.route) {
            DuplicateResultScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(
            route = Screen.ViewAll.route,
            arguments = listOf(
                navArgument("groupIndex") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val groupIndex = backStackEntry.arguments?.getInt("groupIndex") ?: -1
            DuplicateGroupDetailScreen(
                viewModel = viewModel,
                groupIndex = groupIndex,
                navController = navController
            )
        }
    }
}