package com.digitalturbine.promptnews.ui.search

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.digitalturbine.promptnews.di.AppGraph

@Composable
fun PromptScreen(
    navController: NavController
) {
    val navBackStackEntry = navController.getBackStackEntry("prompt")
    val searchViewModel: SearchViewModel = viewModel(
        navBackStackEntry,
        factory = AppGraph.searchViewModelFactory
    )

    LaunchedEffect(Unit) {
        searchViewModel.resetToTrending()
    }

    SearchScreen(
        screenState = SearchScreenState.Prompt,
        navController = navController,
        searchViewModel = searchViewModel,
        onSearchRequested = { query ->
            navController.navigate("search?query=${Uri.encode(query)}&serpOnly=false")
        },
        onBack = { navController.popBackStack() }
    )
}
