package com.example.compasos_tv.data.entitys.dao


import androidx.room.*
import com.example.compasos_tv.data.entitys.VideoTvEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTvDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(videos: List<VideoTvEntity>)

    @Query("SELECT * FROM videos_tv WHERE categoria = :categoria ORDER BY fechaCache DESC")
    fun observarPorCategoria(categoria: String): Flow<List<VideoTvEntity>>

    @Query("DELETE FROM videos_tv WHERE categoria = :categoria")
    suspend fun borrarCategoria(categoria: String)

    @Query("SELECT MAX(fechaCache) FROM videos_tv WHERE categoria = :categoria")
    suspend fun ultimaActualizacion(categoria: String): Long?
}