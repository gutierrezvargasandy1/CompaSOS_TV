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

    @Query("SELECT * FROM familiares_tv ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<FamiliarTvEntity>>

    @Query("UPDATE familiares_tv SET enLinea = 0")
    suspend fun marcarTodosDesconectados()
}