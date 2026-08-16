package com.example.compasos_tv.data.entitys

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamiliarTvDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(familiar: FamiliarTvEntity)

    @Query("""
        UPDATE familiares_tv
        SET latitud = :lat,
            longitud = :lng,
            ultimaUbicacionFecha = :fecha,
            enLinea = 1
        WHERE usuarioId = :id
    """)
    suspend fun actualizarUbicacion(id: String, lat: Double, lng: Double, fecha: String)

    @Query("SELECT * FROM familiares_tv ORDER BY enLinea DESC, nombre ASC")
    fun observarTodos(): Flow<List<FamiliarTvEntity>>

    @Query("UPDATE familiares_tv SET enLinea = 0")
    suspend fun marcarTodosDesconectados()

    // ══════════════════════════════════════════════════════════════════════
    //  NUEVO — arregla el bug que hacía "parpadear" las ubicaciones
    // ══════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(familiar: FamiliarTvEntity): Long

    @Query("""
        UPDATE familiares_tv
        SET nombre = :nombre, apellido = :apellido
        WHERE usuarioId = :id
    """)
    suspend fun actualizarPerfil(id: String, nombre: String, apellido: String?)

    @Query("UPDATE familiares_tv SET enLinea = :enLinea WHERE usuarioId = :id")
    suspend fun marcarEnLinea(id: String, enLinea: Boolean)

    /** Los que no reportan desde hace rato dejan de estar "en línea". */
    @Query("UPDATE familiares_tv SET enLinea = 0 WHERE ultimaUbicacionFecha < :antesDe")
    suspend fun marcarInactivosAntesDe(antesDe: String)

    @Query("SELECT COUNT(*) FROM familiares_tv")
    suspend fun contar(): Int

    /**
     * EL BUG QUE ESTO ARREGLA:
     * procesarSesion() llamaba a insertar() con OnConflictStrategy.REPLACE.
     * REPLACE = DELETE + INSERT. Cada snapshot que llegaba SIN latitud
     * (porque ese familiar todavía no reportaba) BORRABA la ubicación que ya
     * tenías guardada. En pantalla se veía como si los familiares perdieran su
     * posición cada pocos segundos.
     *
     * Aquí el perfil y la ubicación se actualizan por separado, y la ubicación
     * solo se pisa cuando el snapshot trae una de verdad.
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