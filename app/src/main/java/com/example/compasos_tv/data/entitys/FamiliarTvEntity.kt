package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Integrante de la familia con su última ubicación conocida.
 * El teléfono actualiza estos datos periódicamente vía MQTT.
 */
@Entity(tableName = "familiares_tv")
data class FamiliarTvEntity(
    @PrimaryKey
    val usuarioId:           String,
    val nombre:              String,
    val apellido:            String?  = null,
    val latitud:             Double?  = null,
    val longitud:            Double?  = null,
    val ultimaUbicacionFecha: String? = null,
    val enLinea:             Boolean  = false
)