package com.example.compasos_tv.data.entitys.dao

import androidx.room.*
import com.example.compasos_tv.data.entitys.NotificacionTvEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionTvDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(notificacion: NotificacionTvEntity)

    @Query("SELECT * FROM notificaciones_tv ORDER BY fecha DESC LIMIT 50")
    fun observarRecientes(): Flow<List<NotificacionTvEntity>>

    @Query("SELECT * FROM notificaciones_tv ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<NotificacionTvEntity>>

    @Query("SELECT COUNT(*) FROM notificaciones_tv WHERE leida = 0")
    fun contarNoLeidas(): Flow<Int>

    @Query("UPDATE notificaciones_tv SET leida = 1 WHERE id = :id")
    suspend fun marcarLeida(id: String)

    @Query("UPDATE notificaciones_tv SET leida = 1")
    suspend fun marcarTodasLeidas()
}