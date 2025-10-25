
package com.uvg.mypokedex.data.repository

import com.uvg.mypokedex.data.model.Pokemon
import com.uvg.mypokedex.data.remote.PokemonRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repositorio que maneja la lógica de datos de Pokémon
 * Expone datos como flujos de estado para la capa de presentación
 */
class PokemonRepository(
    private val remoteDataSource: PokemonRemoteDataSource = PokemonRemoteDataSource()
) {
    
    // Obtiene una lista paginada de Pokémon como Flow
     
    fun getPokemonList(limit: Int = 20, offset: Int = 0): Flow<UiState<List<Pokemon>>> = flow {
        emit(UiState.Loading)
        
        val result = remoteDataSource.getPokemonList(limit, offset)
        
        result.fold(
            onSuccess = { pokemonList ->
                if (pokemonList.isEmpty()) {
                    emit(UiState.Empty)
                } else {
                    emit(UiState.Success(pokemonList))
                }
            },
            onFailure = { exception ->
                emit(UiState.Error(exception.message ?: "Error desconocido"))
            }
        )
    }
    
    // Obtiene el detalle de un Pokémon por ID
    fun getPokemonById(id: Int): Flow<UiState<Pokemon>> = flow {
        emit(UiState.Loading)
        
        val result = remoteDataSource.getPokemonById(id)
        
        result.fold(
            onSuccess = { pokemon ->
                emit(UiState.Success(pokemon))
            },
            onFailure = { exception ->
                emit(UiState.Error(exception.message ?: "Error desconocido"))
            }
        )
    }
    
    //Busca un Pokémon por nombre
     
    fun searchPokemonByName(name: String): Flow<UiState<Pokemon>> = flow {
        emit(UiState.Loading)
        
        val result = remoteDataSource.searchPokemonByName(name)
        
        result.fold(
            onSuccess = { pokemon ->
                emit(UiState.Success(pokemon))
            },
            onFailure = { exception ->
                emit(UiState.Error(exception.message ?: "Pokémon no encontrado"))
            }
        )
    }
}

/**
 * Sealed class que representa los diferentes estados de la UI
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
