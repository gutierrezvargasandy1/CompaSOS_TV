package com.example.compasos_tv.data.entitys.dao


import androidx.room.*
import com.example.compasos_tv.data.entitys.VideoTvEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Room para la tabla `videos_tv`.
 * Usado por [com.example.compasos_tv.data.entitys.VideosSeguridadRepository]
 * para implementar el caché local de resultados de YouTube.
 */
@Dao
interface VideoTvDao {

    /** Inserta (o reemplaza) un lote completo de videos de una categoría. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(videos: List<VideoTvEntity>)

    /** Observa los videos cacheados de una categoría, más recientes primero. */
    @Query("SELECT * FROM videos_tv WHERE categoria = :categoria ORDER BY fechaCache DESC")
    fun observarPorCategoria(categoria: String): Flow<List<VideoTvEntity>>

    /** Borra todos los videos cacheados de una categoría (previo a refrescar). */
    @Query("DELETE FROM videos_tv WHERE categoria = :categoria")
    suspend fun borrarCategoria(categoria: String)

    /** Timestamp del video más recientemente cacheado de una categoría, usado para saber si el caché expiró. */
    @Query("SELECT MAX(fechaCache) FROM videos_tv WHERE categoria = :categoria")
    suspend fun ultimaActualizacion(categoria: String): Long?
}
