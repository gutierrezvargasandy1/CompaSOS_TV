package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa una alerta de emergencia (SOS) recibida desde
 * el teléfono vinculado. Se persiste en la tabla `alertas_tv`.
 *
 * Solo visualización — la TV no genera alertas, únicamente las recibe por
 * MQTT (ver `TvMqttService.procesarAlerta`) y las guarda aquí para
 * mostrarlas en `TvAlertasScreen`.
 *
 * @property id           identificador único de la alerta (clave primaria).
 * @property tipo          tipo de alerta (p. ej. "SOS"), puede venir vacío.
 * @property descripcion   texto adicional enviado por el emisor.
 * @property emisorNombre  nombre del familiar que generó la alerta.
 * @property emisorId      id del usuario que generó la alerta.
 * @property latitud       latitud del emisor en el momento de la alerta.
 * @property longitud      longitud del emisor en el momento de la alerta.
 * @property fecha         fecha/hora de la alerta en formato "yyyy-MM-dd HH:mm:ss".
 * @property leida         true si el usuario de la TV ya la marcó como vista.
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
