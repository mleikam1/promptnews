package com.digitalturbine.promptnews.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun HomeFragmentHost(navController: NavController) {
    HomeScreen(navController = navController)
}
