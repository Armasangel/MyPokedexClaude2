package com.uvg.mypokedex.ui.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.mypokedex.data.model.Pokemon
import com.uvg.mypokedex.data.repository.PokemonRepository
import com.uvg.mypokedex.data.repository.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = PokemonRepository(application.applicationContext)

    // Estado de la UI
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Estado de conexión
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Lista completa de Pokémon cargados
    private val allPokemon = mutableListOf<Pokemon>()

    // Configuración de paginación
    private var currentOffset = 0
    private val pageSize = 20

    // Configuración de ordenamiento (se carga desde DataStore)
    private var currentSortBy = SortBy.NUMBER
    private var isAscending = true

    // Estado de búsqueda
    private var searchQuery = ""

    init {
        // Observar el estado de conexión
        observeConnectivity()

        // Cargar preferencias de ordenamiento y luego cargar los Pokémon
        loadSortPreferencesAndPokemon()
    }

    /**
     * Observa el estado de conexión a internet
     */
    private fun observeConnectivity() {
        viewModelScope.launch {
            repository.isConnected.collect { connected ->
                _isConnected.value = connected

                // Si recuperamos la conexión y tenemos datos, sincronizar
                if (connected && allPokemon.isNotEmpty()) {
                    syncWithRemote()
                }
            }
        }
    }

    /**
     * Carga las preferencias de ordenamiento desde DataStore
     * y luego carga los Pokémon iniciales
     */
    private fun loadSortPreferencesAndPokemon() {
        viewModelScope.launch {
            // Cargar preferencias
            repository.getSortPreferences().collect { (sortType, ascending) ->
                val newSortBy = when (sortType) {
                    "NAME" -> SortBy.NAME
                    else -> SortBy.NUMBER
                }
                val newIsAscending = ascending

                // Solo actualizar si cambió
                if (newSortBy != currentSortBy || newIsAscending != isAscending) {
                    currentSortBy = newSortBy
                    isAscending = newIsAscending

                    // Si ya tenemos datos, reordenar
                    if (allPokemon.isNotEmpty()) {
                        updateUiState()
                    }
                }

                // Cargar Pokémon iniciales solo la primera vez
                if (allPokemon.isEmpty()) {
                    loadInitialPokemon()
                }
            }
        }
    }

    /**
     * Carga inicial de Pokémon
     */
    private fun loadInitialPokemon() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            repository.getPokemonList(limit = pageSize, offset = 0).collect { result ->
                when (result) {
                    is UiState.Loading -> {
                        _uiState.value = HomeUiState.Loading
                    }
                    is UiState.Success -> {
                        allPokemon.clear()
                        allPokemon.addAll(result.data)
                        currentOffset = pageSize
                        updateUiState()
                    }
                    is UiState.Error -> {
                        _uiState.value = HomeUiState.Error(result.message)
                    }
                    is UiState.Empty -> {
                        _uiState.value = HomeUiState.Empty
                    }
                }
            }
        }
    }

    /**
     * Carga más Pokémon (paginación)
     */
    fun loadMorePokemon() {
        // Evitar cargas múltiples simultáneas
        if (_uiState.value is HomeUiState.LoadingMore) return

        viewModelScope.launch {
            val currentList = when (val state = _uiState.value) {
                is HomeUiState.Success -> state.pokemonList
                else -> emptyList()
            }
            _uiState.value = HomeUiState.LoadingMore(currentList)

            repository.getPokemonList(limit = pageSize, offset = currentOffset).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        // Agregar solo los nuevos Pokémon que no estén ya en la lista
                        val newPokemon = result.data.filter { newPoke ->
                            allPokemon.none { it.id == newPoke.id }
                        }
                        allPokemon.addAll(newPokemon)
                        currentOffset += pageSize
                        updateUiState()
                    }
                    is UiState.Error -> {
                        _uiState.value = HomeUiState.Success(
                            pokemonList = currentList,
                            errorMessage = "Error al cargar más: ${result.message}"
                        )
                    }
                    else -> {
                        updateUiState()
                    }
                }
            }
        }
    }

    /**
     * Busca un Pokémon por nombre
     */
    fun searchPokemon(query: String) {
        searchQuery = query.trim()

        if (searchQuery.isEmpty()) {
            updateUiState()
            return
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            repository.searchPokemonByName(searchQuery).collect { result ->
                when (result) {
                    is UiState.Loading -> {
                        _uiState.value = HomeUiState.Loading
                    }
                    is UiState.Success -> {
                        _uiState.value = HomeUiState.Success(
                            pokemonList = listOf(result.data),
                            isSearchResult = true
                        )
                    }
                    is UiState.Error -> {
                        _uiState.value = HomeUiState.Error(result.message)
                    }
                    else -> {
                        _uiState.value = HomeUiState.Empty
                    }
                }
            }
        }
    }

    /**
     * Limpia la búsqueda y vuelve a la lista completa
     */
    fun clearSearch() {
        searchQuery = ""
        updateUiState()
    }

    /**
     * Aplica ordenamiento a la lista y guarda la preferencia
     */
    fun applySorting(sortBy: SortBy, ascending: Boolean) {
        currentSortBy = sortBy
        isAscending = ascending

        // Guardar en DataStore
        viewModelScope.launch {
            val sortTypeString = when (sortBy) {
                SortBy.NUMBER -> "NUMBER"
                SortBy.NAME -> "NAME"
            }
            repository.saveSortPreferences(sortTypeString, ascending)
        }

        // Actualizar UI con el nuevo ordenamiento
        updateUiState()
    }

    /**
     * Reintenta la carga después de un error
     */
    fun retry() {
        if (allPokemon.isEmpty()) {
            loadInitialPokemon()
        } else {
            updateUiState()
        }
    }

    /**
     * Sincroniza los datos con el servidor remoto
     * Solo funciona si hay conexión
     */
    private fun syncWithRemote() {
        viewModelScope.launch {
            repository.forceSync()
        }
    }

    /**
     * Actualiza el estado de la UI aplicando filtros y ordenamiento
     */
    private fun updateUiState() {
        if (allPokemon.isEmpty()) {
            _uiState.value = HomeUiState.Empty
            return
        }

        val sortedList = when (currentSortBy) {
            SortBy.NUMBER -> {
                if (isAscending) {
                    allPokemon.sortedBy { it.id }
                } else {
                    allPokemon.sortedByDescending { it.id }
                }
            }
            SortBy.NAME -> {
                if (isAscending) {
                    allPokemon.sortedBy { it.name }
                } else {
                    allPokemon.sortedByDescending { it.name }
                }
            }
        }

        _uiState.value = HomeUiState.Success(
            pokemonList = sortedList,
            isSearchResult = searchQuery.isNotEmpty()
        )
    }
}

/**
 * Estados posibles de la pantalla Home
 */
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(
        val pokemonList: List<Pokemon>,
        val isSearchResult: Boolean = false,
        val errorMessage: String? = null
    ) : HomeUiState()
    data class LoadingMore(val currentList: List<Pokemon>) : HomeUiState()
}

/**
 * Opciones de ordenamiento
 */
enum class SortBy {
    NUMBER,
    NAME
}