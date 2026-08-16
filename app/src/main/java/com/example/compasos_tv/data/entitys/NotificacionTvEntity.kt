package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Notificaciones informativas del teléfono (avisos, confirmaciones, cambios
 * en la familia). Van aparte de AlertaTvEntity a propósito: una alerta es una
 * emergencia y sale en rojo; una notificación es informativa.
 */
@Entity(tableName = "notificaciones_tv")
data class NotificacionTvEntity(
    @PrimaryKey
    val id:       String,
    val alertaId: String? = null,
    val titulo:   String,
    val mensaje:  String,
    val tipo:     String  = "info",
    val fecha:    String,
    val leida:    Boolean = false
)