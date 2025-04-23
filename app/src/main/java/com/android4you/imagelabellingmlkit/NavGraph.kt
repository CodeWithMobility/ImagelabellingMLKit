package com.android4you.imagelabellingmlkit

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
}

@Composable
fun NavGraph(navController: NavHostController, viewModel: ImageLabelingViewModel) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController, viewModel)
        }
        composable(Screen.Camera.route) {
            CameraScreen(navController, viewModel)
        }
    }
}
