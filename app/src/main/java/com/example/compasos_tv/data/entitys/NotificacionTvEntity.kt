package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para notificaciones informativas enviadas desde el teléfono
 * (avisos, confirmaciones, cambios en la familia). Se persiste en la tabla
 * `notificaciones_tv`.
 *
 * Van aparte de [AlertaTvEntity] a propósito: una alerta es una emergencia y
 * se muestra en rojo con máxima prioridad; una notificación es meramente
 * informativa y se muestra con un estilo distinto en `TvAlertasScreen`.
 *
 * @property id        identificador único de la notificación (clave primaria).
 * @property alertaId  id de la alerta relacionada, si esta notificación deriva de una.
 * @property titulo    título corto mostrado en la bandeja.
 * @property mensaje   cuerpo del mensaje.
 * @property tipo      categoría de la notificación (por defecto "info").
 * @property fecha     fecha/hora de recepción.
 * @property leida     true si el usuario de la TV ya la marcó como vista.
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
