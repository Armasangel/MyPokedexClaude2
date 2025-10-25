package com.uvg.mypokedex.ui.detail


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.mypokedex.data.model.Pokemon
import com.uvg.mypokedex.data.repository.PokemonRepository
import com.uvg.mypokedex.data.repository.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: PokemonRepository = PokemonRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadPokemonDetail(pokemonId: Int) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading

            repository.getPokemonById(pokemonId).collect { result ->
                when (result) {
                    is UiState.Loading -> {
                        _uiState.value = DetailUiState.Loading
                    }
                    is UiState.Success -> {
                        _uiState.value = DetailUiState.Success(result.data)
                    }
                    is UiState.Error -> {
                        _uiState.value = DetailUiState.Error(result.message)
                    }
                    is UiState.Empty -> {
                        _uiState.value = DetailUiState.Error("Pokémon no encontrado")
                    }
                }
            }
        }
    }

    fun retry(pokemonId: Int) {
        loadPokemonDetail(pokemonId)
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val pokemon: Pokemon) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}