package com.uvg.mypokedex.data.remote

import com.uvg.mypokedex.data.remote.dto.PokemonDetailDto
import com.uvg.mypokedex.data.remote.dto.PokemonListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface que define los endpoints de la PokeAPI
 */
interface PokeApiService {

    /**
     * Obtiene una lista paginada de Pokémon
     * @param limit Número de Pokémon a obtener
     * @param offset Desde qué posición empezar
     */
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<PokemonListResponse>

    /**
     * Obtiene el detalle de un Pokémon específico
     * @param id ID del Pokémon
     */
    @GET("pokemon/{id}")
    suspend fun getPokemonDetail(
        @Path("id") id: Int
    ): Response<PokemonDetailDto>

    /**
     * Obtiene el detalle de un Pokémon por nombre
     * @param name Nombre del Pokémon
     */
    @GET("pokemon/{name}")
    suspend fun getPokemonByName(
        @Path("name") name: String
    ): Response<PokemonDetailDto>
}