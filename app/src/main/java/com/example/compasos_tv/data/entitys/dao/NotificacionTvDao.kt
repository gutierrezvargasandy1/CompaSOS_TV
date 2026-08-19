package com.example.compasos_tv.data.entitys.dao

import androidx.room.*
import com.example.compasos_tv.data.entitys.NotificacionTvEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Room para la tabla `notificaciones_tv`.
 * Análogo a [com.example.compasos_tv.data.entitys.AlertaTvDao] pero para
 * mensajes informativos (no emergencias).
 */
@Dao
interface NotificacionTvDao {

    /** Inserta una notificación nueva; si ya existe una con el mismo id, la reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(notificacion: NotificacionTvEntity)

    /** Últimas 50 notificaciones, ordenadas de más reciente a más antigua. */
    @Query("SELECT * FROM notificaciones_tv ORDER BY fecha DESC LIMIT 50")
    fun observarRecientes(): Flow<List<NotificacionTvEntity>>

    /** Todas las notificaciones, sin límite. */
    @Query("SELECT * FROM notificaciones_tv ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<NotificacionTvEntity>>

    /** Cantidad de notificaciones sin leer, para mostrar el badge. */
    @Query("SELECT COUNT(*) FROM notificaciones_tv WHERE leida = 0")
    fun contarNoLeidas(): Flow<Int>

    /** Marca una notificación puntual como leída. */
    @Query("UPDATE notificaciones_tv SET leida = 1 WHERE id = :id")
    suspend fun marcarLeida(id: String)

    /** Marca todas las notificaciones existentes como leídas. */
    @Query("UPDATE notificaciones_tv SET leida = 1")
    suspend fun marcarTodasLeidas()
}
