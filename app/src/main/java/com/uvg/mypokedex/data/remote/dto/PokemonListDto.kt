package com.uvg.mypokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta de lista de Pokémon de la API
 */
data class PokemonListResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("results")
    val results: List<PokemonListItem>
)

data class PokemonListItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String
) {
    val id: Int
        get() = url.trimEnd('/').split("/").last().toInt()
}