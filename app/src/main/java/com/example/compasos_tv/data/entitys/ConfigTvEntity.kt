package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Configuración y sesión vinculada.
 * Es un singleton (id = 1 siempre).
 */
@Entity(tableName = "config_tv")
data class ConfigTvEntity(
    @PrimaryKey
    val id:                   Int     = 1,
    val tvDeviceId:           String  = "",
    val codigoVinculacion:    String  = "",
    val usuarioIdVinculado:   String  = "",
    val nombreUsuario:        String  = "",
    val emailUsuario:         String  = "",
    val vinculado:            Boolean = false,
    val ultimaActualizacion:  Long    = 0L
)