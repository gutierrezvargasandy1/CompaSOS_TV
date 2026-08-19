package com.example.compasos_tv.data.entitys

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) de Room para la tabla `alertas_tv`.
 * Todas las lecturas se exponen como [Flow] para que la UI se actualice
 * automáticamente en cuanto `TvMqttService` inserta una alerta nueva.
 */
@Dao
interface AlertaTvDao {

    /** Inserta una alerta nueva; si ya existe una con el mismo id, la reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(alerta: AlertaTvEntity)

    /** Banner: última alerta activa no leída (para mostrar el aviso más reciente). */
    @Query("SELECT * FROM alertas_tv WHERE leida = 0 ORDER BY fecha DESC LIMIT 1")
    fun observarUltimaNoLeida(): Flow<AlertaTvEntity?>

    /** Historial reciente (últimas 30) — usado por DashboardTvViewModel */
    @Query("SELECT * FROM alertas_tv ORDER BY fecha DESC LIMIT 30")
    fun observarRecientes(): Flow<List<AlertaTvEntity>>

    /** Todas sin límite — resuelve "Unresolved reference 'observarTodas'" */
    @Query("SELECT * FROM alertas_tv ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<AlertaTvEntity>>

    /** Contador badge — resuelve "Unresolved reference 'contarNoLeidas'" */
    @Query("SELECT COUNT(*) FROM alertas_tv WHERE leida = 0")
    fun contarNoLeidas(): Flow<Int>

    /** Marca una alerta puntual como leída. */
    @Query("UPDATE alertas_tv SET leida = 1 WHERE id = :id")
    suspend fun marcarLeida(id: String)

    /** Marca todas las alertas existentes como leídas. */
    @Query("UPDATE alertas_tv SET leida = 1")
    suspend fun marcarTodasLeidas()
}
