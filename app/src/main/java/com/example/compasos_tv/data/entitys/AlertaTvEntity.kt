package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Alertas recibidas desde el teléfono vinculado.
 * Solo visualización — la TV no genera alertas.
 */
@Entity(tableName = "alertas_tv")
data class AlertaTvEntity(
    @PrimaryKey
    val id:           String,
    val tipo:         String?  = null,
    val descripcion:  String?  = null,
    val emisorNombre: String?  = null,
    val emisorId:     String?  = null,
    val latitud:      Double?  = null,
    val longitud:     Double?  = null,
    val fecha:        String,
    val leida:        Boolean  = false
)