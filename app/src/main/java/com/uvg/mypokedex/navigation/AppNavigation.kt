package com.uvg.mypokedex.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.uvg.mypokedex.ui.detail.DetailScreen
import com.uvg.mypokedex.ui.detail.DetailViewModel
import com.uvg.mypokedex.ui.features.home.HomeScreen
import com.uvg.mypokedex.ui.features.home.HomeViewModel
import com.uvg.mypokedex.ui.features.search.SearchToolsDialog

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    val homeViewModel: HomeViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppScreens.HomeScreen.route
    ) {
        composable(route = AppScreens.HomeScreen.route) {
            HomeScreen(
                paddingValues = paddingValues,
                onPokemonClick = { pokemonId ->
                    navController.navigate(AppScreens.DetailScreen.createRoute(pokemonId))
                },
                onSearchToolsClick = {
                    navController.navigate(AppScreens.SearchToolsDialog.route)
                },
                viewModel = homeViewModel
            )
        }

        composable(
            route = AppScreens.DetailScreen.route,
            arguments = listOf(
                navArgument("pokemonId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val pokemonId = backStackEntry.arguments?.getInt("pokemonId") ?: 0
            val detailViewModel: DetailViewModel = viewModel()

            DetailScreen(
                pokemonId = pokemonId,
                onBackClick = {
                    navController.popBackStack()
                },
                paddingValues = paddingValues,
                viewModel = detailViewModel
            )
        }

        dialog(route = AppScreens.SearchToolsDialog.route) {
            SearchToolsDialog(
                onDismiss = {
                    navController.popBackStack()
                },
                paddingValues = paddingValues,
                viewModel = homeViewModel
            )
        }
    }
}