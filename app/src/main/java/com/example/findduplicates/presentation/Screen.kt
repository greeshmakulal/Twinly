package com.example.findduplicates.presentation

sealed class Screen(
    val route: String
) {

    object Home : Screen("home")

    object DuplicateResults :
        Screen("duplicateResults")
}