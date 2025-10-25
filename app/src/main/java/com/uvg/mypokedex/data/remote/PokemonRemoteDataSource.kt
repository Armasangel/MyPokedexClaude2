package com.uvg.mypokedex.data.remote

import com.uvg.mypokedex.data.model.Pokemon
import com.uvg.mypokedex.data.remote.dto.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fuente de datos remota que encapsula las llamadas a la API
 */
class PokemonRemoteDataSource(
    private val apiService: PokeApiService = RetrofitInstance.api
) {

    /**
     * Obtiene una lista de Pokémon con paginación
     */
    suspend fun getPokemonList(limit: Int = 20, offset: Int = 0): Result<List<Pokemon>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPokemonList(limit, offset)

                if (response.isSuccessful) {
                    val listResponse = response.body()
                    if (listResponse != null) {
                        // Para cada item de la lista, obtener su detalle
                        val pokemonList = listResponse.results.map { item ->
                            val detailResponse = apiService.getPokemonDetail(item.id)
                            if (detailResponse.isSuccessful && detailResponse.body() != null) {
                                detailResponse.body()!!.toDomain()
                            } else {
                                null
                            }
                        }.filterNotNull()

                        Result.success(pokemonList)
                    } else {
                        Result.failure(Exception("Respuesta vacía del servidor"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene el detalle de un Pokémon específico por ID
     */
    suspend fun getPokemonById(id: Int): Result<Pokemon> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPokemonDetail(id)

                if (response.isSuccessful) {
                    val pokemonDto = response.body()
                    if (pokemonDto != null) {
                        Result.success(pokemonDto.toDomain())
                    } else {
                        Result.failure(Exception("Pokémon no encontrado"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Busca un Pokémon por nombre
     */
    suspend fun searchPokemonByName(name: String): Result<Pokemon> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPokemonByName(name.lowercase())

                if (response.isSuccessful) {
                    val pokemonDto = response.body()
                    if (pokemonDto != null) {
                        Result.success(pokemonDto.toDomain())
                    } else {
                        Result.failure(Exception("Pokémon no encontrado"))
                    }
                } else {
                    Result.failure(Exception("Pokémon no encontrado"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error al buscar: ${e.message}"))
            }
        }
    }
}