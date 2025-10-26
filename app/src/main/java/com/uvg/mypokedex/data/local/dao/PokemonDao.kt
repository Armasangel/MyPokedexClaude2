package com.uvg.mypokedex.data.local.dao

import androidx.room.*
import com.uvg.mypokedex.data.local.entity.CachedPokemon
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para las operaciones de Pokémon en la base de datos local
 */
@Dao
interface PokemonDao {

    /**
     * Obtiene todos los Pokémon de la base de datos como Flow
     * Se actualiza automáticamente cuando hay cambios
     */
    @Query("SELECT * FROM cached_pokemon ORDER BY id ASC")
    fun getAllPokemon(): Flow<List<CachedPokemon>>

    /**
     * Obtiene un Pokémon específico por ID
     */
    @Query("SELECT * FROM cached_pokemon WHERE id = :pokemonId")
    suspend fun getPokemonById(pokemonId: Int): CachedPokemon?

    /**
     * Inserta o actualiza un Pokémon
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemon(pokemon: CachedPokemon)

    /**
     * Inserta o actualiza múltiples Pokémon
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPokemon(pokemon: List<CachedPokemon>)

    /**
     * Elimina un Pokémon específico
     */
    @Delete
    suspend fun deletePokemon(pokemon: CachedPokemon)

    /**
     * Elimina todos los Pokémon de la base de datos
     */
    @Query("DELETE FROM cached_pokemon")
    suspend fun deleteAllPokemon()

    /**
     * Obtiene el conteo total de Pokémon en caché
     */
    @Query("SELECT COUNT(*) FROM cached_pokemon")
    suspend fun getPokemonCount(): Int

    /**
     * Busca un Pokémon por nombre
     */
    @Query("SELECT * FROM cached_pokemon WHERE name LIKE :name LIMIT 1")
    suspend fun searchPokemonByName(name: String): CachedPokemon?

    /**
     * Obtiene Pokémon que fueron actualizados antes de cierta fecha
     * Útil para implementar estrategias de expiración de caché
     */
    @Query("SELECT * FROM cached_pokemon WHERE lastFetchedAt < :timestamp")
    suspend fun getStaleCache(timestamp: Long): List<CachedPokemon>
}