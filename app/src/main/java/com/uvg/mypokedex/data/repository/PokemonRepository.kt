package com.uvg.mypokedex.data.repository

import android.content.Context
import com.uvg.mypokedex.data.connectivity.ConnectivityObserver
import com.uvg.mypokedex.data.connectivity.NetworkConnectivityObserver
import com.uvg.mypokedex.data.datastore.UserPreferences
import com.uvg.mypokedex.data.local.database.PokemonDatabase
import com.uvg.mypokedex.data.local.entity.toCache
import com.uvg.mypokedex.data.local.entity.toDomain
import com.uvg.mypokedex.data.model.Pokemon
import com.uvg.mypokedex.data.remote.PokemonRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio que maneja la lógica de datos de Pokémon
 * Implementa estrategia cache-first con sincronización automática
 */
class PokemonRepository(
    private val context: Context,
    private val remoteDataSource: PokemonRemoteDataSource = PokemonRemoteDataSource(),
    private val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(context)
) {

    // Base de datos local
    private val database = PokemonDatabase.getDatabase(context)
    private val pokemonDao = database.pokemonDao()

    // DataStore para preferencias
    private val userPreferences = UserPreferences(context)

    /**
     * Flow que expone el estado de conectividad
     */
    val isConnected: Flow<Boolean> = connectivityObserver.observe().map { status ->
        status == ConnectivityObserver.Status.Available
    }

    /**
     * Obtiene las preferencias de ordenamiento del usuario
     */
    fun getSortPreferences(): Flow<Pair<String, Boolean>> {
        return combine(
            userPreferences.sortType,
            userPreferences.isAscending
        ) { sortType, isAscending ->
            Pair(sortType, isAscending)
        }
    }

    /**
     * Guarda las preferencias de ordenamiento
     */
    suspend fun saveSortPreferences(sortType: String, isAscending: Boolean) {
        userPreferences.saveSortPreferences(sortType, isAscending)
    }

    /**
     * Obtiene la lista de Pokémon con estrategia cache-first
     * 1. Primero retorna los datos del caché
     * 2. Si hay conexión, actualiza el caché desde la API
     */
    fun getPokemonList(limit: Int = 20, offset: Int = 0): Flow<UiState<List<Pokemon>>> = flow {
        emit(UiState.Loading)

        try {
            // Primero intentamos obtener datos del caché
            val cachedCount = pokemonDao.getPokemonCount()

            if (cachedCount > 0) {
                // Emitir datos del caché inmediatamente
                pokemonDao.getAllPokemon().collect { cachedPokemon ->
                    val domainList = cachedPokemon.map { it.toDomain() }
                    if (domainList.isNotEmpty()) {
                        emit(UiState.Success(domainList))
                    }
                }
            }

            // Si hay conexión, actualizar el caché en segundo plano
            if (connectivityObserver.isConnected()) {
                val result = remoteDataSource.getPokemonList(limit, offset)

                result.fold(
                    onSuccess = { pokemonList ->
                        // Guardar en caché
                        val cachedList = pokemonList.map { it.toCache() }
                        pokemonDao.insertAllPokemon(cachedList)

                        // Emitir los datos actualizados
                        if (pokemonList.isNotEmpty()) {
                            emit(UiState.Success(pokemonList))
                        } else {
                            emit(UiState.Empty)
                        }
                    },
                    onFailure = { exception ->
                        // Si falla la actualización pero tenemos caché, seguimos mostrando el caché
                        if (cachedCount == 0) {
                            emit(UiState.Error(exception.message ?: "Error desconocido"))
                        }
                    }
                )
            } else if (cachedCount == 0) {
                // No hay caché ni conexión
                emit(UiState.Error("No hay conexión a internet y no hay datos en caché"))
            }

        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Obtiene el detalle de un Pokémon por ID con estrategia cache-first
     */
    fun getPokemonById(id: Int): Flow<UiState<Pokemon>> = flow {
        emit(UiState.Loading)

        try {
            // Primero intentamos obtener del caché
            val cachedPokemon = pokemonDao.getPokemonById(id)

            if (cachedPokemon != null) {
                emit(UiState.Success(cachedPokemon.toDomain()))
            }

            // Si hay conexión, actualizar desde la API
            if (connectivityObserver.isConnected()) {
                val result = remoteDataSource.getPokemonById(id)

                result.fold(
                    onSuccess = { pokemon ->
                        // Guardar en caché
                        pokemonDao.insertPokemon(pokemon.toCache())
                        emit(UiState.Success(pokemon))
                    },
                    onFailure = { exception ->
                        // Si falla pero tenemos caché, ya lo emitimos antes
                        if (cachedPokemon == null) {
                            emit(UiState.Error(exception.message ?: "Error desconocido"))
                        }
                    }
                )
            } else if (cachedPokemon == null) {
                emit(UiState.Error("No hay conexión a internet y no hay datos en caché"))
            }

        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Busca un Pokémon por nombre con estrategia cache-first
     */
    fun searchPokemonByName(name: String): Flow<UiState<Pokemon>> = flow {
        emit(UiState.Loading)

        try {
            // Primero buscar en caché
            val cachedPokemon = pokemonDao.searchPokemonByName(name.lowercase())

            if (cachedPokemon != null) {
                emit(UiState.Success(cachedPokemon.toDomain()))
            }

            // Si hay conexión, buscar en la API
            if (connectivityObserver.isConnected()) {
                val result = remoteDataSource.searchPokemonByName(name)

                result.fold(
                    onSuccess = { pokemon ->
                        // Guardar en caché
                        pokemonDao.insertPokemon(pokemon.toCache())
                        emit(UiState.Success(pokemon))
                    },
                    onFailure = { exception ->
                        if (cachedPokemon == null) {
                            emit(UiState.Error(exception.message ?: "Pokémon no encontrado"))
                        }
                    }
                )
            } else if (cachedPokemon == null) {
                emit(UiState.Error("No hay conexión a internet y no hay datos en caché"))
            }

        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Error al buscar"))
        }
    }

    /**
     * Fuerza una sincronización completa con la API
     * Solo funciona si hay conexión
     */
    suspend fun forceSync(): Result<Unit> {
        return if (connectivityObserver.isConnected()) {
            try {
                val result = remoteDataSource.getPokemonList(limit = 100, offset = 0)
                result.fold(
                    onSuccess = { pokemonList ->
                        val cachedList = pokemonList.map { it.toCache() }
                        pokemonDao.insertAllPokemon(cachedList)
                        Result.success(Unit)
                    },
                    onFailure = { exception ->
                        Result.failure(exception)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No hay conexión a internet"))
        }
    }

    /**
     * Limpia todo el caché local
     */
    suspend fun clearCache() {
        pokemonDao.deleteAllPokemon()
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