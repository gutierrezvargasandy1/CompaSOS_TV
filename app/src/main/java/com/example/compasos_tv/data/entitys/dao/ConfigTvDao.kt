package com.example.compasos_tv.data.entitys

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigTvDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(config: ConfigTvEntity)

    @Query("SELECT * FROM config_tv WHERE id = 1")
    suspend fun obtener(): ConfigTvEntity?

    @Query("SELECT * FROM config_tv WHERE id = 1")
    fun observar(): Flow<ConfigTvEntity?>

    @Query("UPDATE config_tv SET codigoVinculacion = :codigo WHERE id = 1")
    suspend fun setCodigo(codigo: String)

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