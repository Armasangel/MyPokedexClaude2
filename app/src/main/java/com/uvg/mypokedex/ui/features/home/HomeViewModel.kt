package com.uvg.mypokedex.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.mypokedex.data.model.Pokemon
import com.uvg.mypokedex.data.repository.PokemonRepository
import com.uvg.mypokedex.data.repository.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: PokemonRepository = PokemonRepository()
) : ViewModel() {

    // Estado de la UI
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Lista completa de Pokémon cargados
    private val allPokemon = mutableListOf<Pokemon>()
    
    // Configuración de paginación
    private var currentOffset = 0
    private val pageSize = 20
    
    // Configuración de ordenamiento
    private var currentSortBy = SortBy.NUMBER
    private var isAscending = true
    
    // Estado de búsqueda
    private var searchQuery = ""

    init {
        loadInitialPokemon()
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
     * Carga más Pokémon
     */
    fun loadMorePokemon() {
        // Evitar cargas múltiples simultáneas
        if (_uiState.value is HomeUiState.LoadingMore) return
        
        viewModelScope.launch {
            // Mostrar estado de carga sin perder los datos actuales
            val currentList = when (val state = _uiState.value) {
                is HomeUiState.Success -> state.pokemonList
                else -> emptyList()
            }
            _uiState.value = HomeUiState.LoadingMore(currentList)
            
            repository.getPokemonList(limit = pageSize, offset = currentOffset).collect { result ->
                when (result) {
                    is UiState.Success -> {
                        allPokemon.addAll(result.data)
                        currentOffset += pageSize
                        updateUiState()
                    }
                    is UiState.Error -> {
                        // Volver al estado exitoso anterior con mensaje de error
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
     * Aplica ordenamiento a la lista
     */
    fun applySorting(sortBy: SortBy, ascending: Boolean) {
        currentSortBy = sortBy
        isAscending = ascending
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
    object Loading : HomeUiState()
    object Empty : HomeUiState()
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
