package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que guarda la configuración del dispositivo y el estado de
 * vinculación con una cuenta de teléfono. Se persiste en la tabla
 * `config_tv`.
 *
 * Es un singleton a nivel de fila: siempre se usa `id = 1`, por lo que la
 * tabla nunca tiene más de un registro (ver `ConfigTvDao`, que siempre
 * consulta/actualiza `WHERE id = 1`).
 *
 * @property id                  clave primaria fija en 1 (patrón singleton row).
 * @property tvDeviceId          identificador único de esta TV (ver `TvMqttService.obtenerTvId`).
 * @property codigoVinculacion   código de 4-6 caracteres usado en el último intento de vinculación.
 * @property usuarioIdVinculado  id del usuario del teléfono ya vinculado.
 * @property nombreUsuario       nombre del usuario vinculado, para mostrar en UI.
 * @property emailUsuario        correo del usuario vinculado.
 * @property vinculado           true si la TV está actualmente vinculada a una cuenta.
 * @property ultimaActualizacion timestamp (epoch ms) de la última vez que se actualizó este registro.
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
