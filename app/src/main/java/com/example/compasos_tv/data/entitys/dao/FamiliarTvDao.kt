package com.example.compasos_tv.data.entitys

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Room para la tabla `familiares_tv`.
 *
 * Contiene tanto operaciones simples de inserción/actualización como el
 * método compuesto [guardarConservandoUbicacion], pensado específicamente
 * para evitar el bug de "parpadeo" de ubicaciones descrito más abajo.
 */
@Dao
interface FamiliarTvDao {

    /** Inserta o reemplaza por completo el registro de un familiar. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(familiar: FamiliarTvEntity)

    /** Actualiza solo la ubicación de un familiar y lo marca como en línea. */
    @Query("""
        UPDATE familiares_tv
        SET latitud = :lat,
            longitud = :lng,
            ultimaUbicacionFecha = :fecha,
            enLinea = 1
        WHERE usuarioId = :id
    """)
    suspend fun actualizarUbicacion(id: String, lat: Double, lng: Double, fecha: String)

    /** Observa la lista completa de familiares, en línea primero. */
    @Query("SELECT * FROM familiares_tv ORDER BY enLinea DESC, nombre ASC")
    fun observarTodos(): Flow<List<FamiliarTvEntity>>

    /** Marca a todos los familiares como desconectados (usado al perder la sesión). */
    @Query("UPDATE familiares_tv SET enLinea = 0")
    suspend fun marcarTodosDesconectados()═════════════════════════════════════════════════════════════════════

    /**
     * Inserta el familiar solo si todavía no existe. Devuelve -1 si ya
     * existía (por `OnConflictStrategy.IGNORE`), lo que se usa en
     * [guardarConservandoUbicacion] para decidir si hay que actualizar en
     * vez de insertar.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(familiar: FamiliarTvEntity): Long

    /** Actualiza solo nombre y apellido, sin tocar la ubicación guardada. */
    @Query("""
        UPDATE familiares_tv
        SET nombre = :nombre, apellido = :apellido
        WHERE usuarioId = :id
    """)
    suspend fun actualizarPerfil(id: String, nombre: String, apellido: String?)

    /** Cambia únicamente el flag de en línea/desconectado de un familiar. */
    @Query("UPDATE familiares_tv SET enLinea = :enLinea WHERE usuarioId = :id")
    suspend fun marcarEnLinea(id: String, enLinea: Boolean)

    /** Los que no reportan desde hace rato dejan de estar "en línea". */
    @Query("UPDATE familiares_tv SET enLinea = 0 WHERE ultimaUbicacionFecha < :antesDe")
    suspend fun marcarInactivosAntesDe(antesDe: String)

    /** Cuenta cuántos familiares hay registrados en total. */
    @Query("SELECT COUNT(*) FROM familiares_tv")
    suspend fun contar(): Int

    /**
     * Guarda (inserta o actualiza) un familiar SIN perder su última
     * ubicación conocida cuando el snapshot entrante no trae coordenadas.
     *
     * EL BUG QUE ESTO ARREGLA:
     * El código anterior llamaba a `insertar()` con `OnConflictStrategy.REPLACE`.
     * REPLACE = DELETE + INSERT. Cada snapshot que llegaba SIN latitud
     * (porque ese familiar todavía no reportaba) BORRABA la ubicación que ya
     * se tenía guardada. En pantalla se veía como si los familiares perdieran
     * su posición cada pocos segundos.
     *
     * Aquí el perfil y la ubicación se actualizan por separado, y la
     * ubicación solo se pisa cuando el snapshot trae una de verdad.
     *
     * @param f snapshot recibido por MQTT que puede o no traer ubicación.
     */
    @Transaction
    suspend fun guardarConservandoUbicacion(f: FamiliarTvEntity) {
        val insertado = insertarSiNoExiste(f)
        if (insertado == -1L) {
            actualizarPerfil(f.usuarioId, f.nombre, f.apellido)
            if (f.latitud != null && f.longitud != null) {
                actualizarUbicacion(
                    id    = f.usuarioId,
                    lat   = f.latitud,
                    lng   = f.longitud,
                    fecha = f.ultimaUbicacionFecha ?: ""
                )
            }
            // Después de actualizarUbicacion, porque esa query fuerza enLinea=1
            marcarEnLinea(f.usuarioId, f.enLinea)
        }
    }
}
