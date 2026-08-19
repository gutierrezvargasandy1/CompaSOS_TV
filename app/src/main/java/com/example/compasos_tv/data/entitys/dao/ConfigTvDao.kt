package com.example.compasos_tv.data.entitys

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Room para la tabla `config_tv`.
 *
 * Todas las queries operan sobre `WHERE id = 1`, porque [ConfigTvEntity] es
 * una fila singleton: guarda la configuración/vinculación de ESTA TV, no
 * hay múltiples registros.
 */
@Dao
interface ConfigTvDao {

    /** Inserta o reemplaza por completo la fila de configuración. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(config: ConfigTvEntity)

    /** Lee la configuración actual de forma puntual (no reactiva). */
    @Query("SELECT * FROM config_tv WHERE id = 1")
    suspend fun obtener(): ConfigTvEntity?

    /** Observa la configuración de forma reactiva (se usa en las pantallas Compose). */
    @Query("SELECT * FROM config_tv WHERE id = 1")
    fun observar(): Flow<ConfigTvEntity?>

    /** Guarda el código de vinculación que el usuario está intentando usar. */
    @Query("UPDATE config_tv SET codigoVinculacion = :codigo WHERE id = 1")
    suspend fun setCodigo(codigo: String)

    /**
     * Marca la TV como vinculada y guarda los datos del usuario/teléfono que
     * confirmó la vinculación por MQTT.
     *
     * @param usuarioId id del usuario del teléfono.
     * @param nombre    nombre del usuario, para mostrar en la UI de configuración.
     * @param email     correo del usuario.
     * @param ts        timestamp (epoch ms) del momento de la confirmación.
     */
    @Query("""
        UPDATE config_tv
        SET vinculado            = 1,
            usuarioIdVinculado   = :usuarioId,
            nombreUsuario        = :nombre,
            emailUsuario         = :email,
            ultimaActualizacion  = :ts
        WHERE id = 1
    """)
    suspend fun confirmarVinculacion(
        usuarioId: String,
        nombre:    String,
        email:     String,
        ts:        Long
    )

    /** Limpia toda la información de vinculación, dejando la TV como "sin vincular". */
    @Query("""
        UPDATE config_tv
        SET vinculado          = 0,
            usuarioIdVinculado = '',
            nombreUsuario      = '',
            emailUsuario       = '',
            codigoVinculacion  = ''
        WHERE id = 1
    """)
    suspend fun desvincular()
}
