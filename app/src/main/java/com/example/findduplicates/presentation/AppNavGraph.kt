package com.example.findduplicates.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: FileViewModel
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {

        composable(
            route = Screen.Home.route
        ) {

            HomeScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(
            route = Screen.DuplicateResults.route
        ) {

            DuplicateResultScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
    }
}