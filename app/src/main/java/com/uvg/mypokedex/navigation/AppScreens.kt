package com.uvg.mypokedex.navigation

sealed class AppScreens(val route: String) {
    object HomeScreen : AppScreens("home_screen")
    object DetailScreen : AppScreens("detail_screen/{pokemonId}") {
        fun createRoute(pokemonId: Int) = "detail_screen/$pokemonId"
    }
    object SearchToolsDialog : AppScreens("search_tools_dialog")
}
